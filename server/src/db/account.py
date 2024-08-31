import secrets
from typing import Optional, Tuple
from asyncpg import Pool
from datetime import datetime, timezone

from pydantic import BaseModel

from db.column_cryptor import ColumnCryptor
from models.account import AccountType, account_type_from_int, account_type_to_int


async def account_type(pool: Pool, uid: int) -> Optional[AccountType]:
    async with pool.acquire() as con:
        user = await con.fetchrow("SELECT account_type FROM userinfo WHERE id = $1;", uid)
        if not user:
            return None
        atype = account_type_from_int(user["account_type"])
        if atype is None:
            raise ValueError("Account type int cannot be converted to enum")
        return atype

async def cin_exists(pool: Pool, cin_hash: bytes) -> bool:
    async with pool.acquire() as con:
        return await con.fetchval("select exists(select 1 from userinfo where cin_hash = $1);", cin_hash)


async def create(pool: Pool, cse: ColumnCryptor, token: bytes,
                         name: str, cin: str, atype: AccountType,
                         description: str, phone: str, caregiver=None):
    async with pool.acquire() as con:
        async with con.transaction():
            nonce = cse.gen_nonce()

            cin_bytes = cin.encode()
            newid = await con.fetchval('''
insert into userinfo (name, cin, cin_hash, account_type, description, phone, enc_nonce, create_time, last_seen_time)
values ($1, $2, $3, $4, $5, $6, $7, $8, 0) returning id;
                ''',
                cse.encrypt(name.encode(), nonce),
                cse.encrypt(cin_bytes, nonce),
                cse.gen_hash(cin_bytes),
                account_type_to_int(atype),
                cse.encrypt(description.encode(), nonce),
                cse.encrypt(phone.encode(), nonce),
                nonce,
                int(datetime.now(timezone.utc).timestamp())
            )
            if caregiver is not None:
                await con.execute(
                    "insert into assigned (caregiver, patient) values ($1, $2)",
                    caregiver,
                    newid
                )
            await con.execute(
                "insert into token (id, token, change_time) values ($1, $2, 0)",
                newid,
                token
            )

class UserLogin(BaseModel):
    name: str
    cin: str

async def login(pool: Pool, cse: ColumnCryptor, token: bytes) -> Tuple[UserLogin, str]:
    async with pool.acquire() as con:
        user = await con.fetchrow(
            "select userinfo.id, name, cin, enc_nonce from token, userinfo where token.id = userinfo.id and token.token = $1;",
            token
        )

        cookie = secrets.token_urlsafe(96)
        await con.execute(
            '''
insert into cookie (id, cookie, update_time) values ($1, $2, $3)
on conflict (id)
do update set cookie = $2, update_time = $3
            ''',
            user["id"],
            cookie,
            int(datetime.now(timezone.utc).timestamp())
        )

    return (
        UserLogin(
            name=cse.decrypt(user["cin"], user["enc_nonce"]).decode(),
            cin=cse.decrypt(user["cin"], user["enc_nonce"]).decode()
        ),
        cookie
    )
