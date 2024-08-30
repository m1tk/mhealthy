from typing import Optional
from asyncpg import Pool
from datetime import datetime, timezone

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

