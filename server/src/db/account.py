import secrets
from typing import Optional
from asyncpg import Pool
from datetime import datetime, timezone
from cryptography.hazmat.primitives import constant_time
from pydantic import BaseModel

from db.caregiver import assign_caregiver_to_patient_inner
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
                         description: str, phone: str, caregiver=None) -> int:
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
                await assign_caregiver_to_patient_inner(con, cse, caregiver, newid, caregiver)
            await con.execute(
                "insert into token (id, token, change_time) values ($1, $2, 0)",
                newid,
                cse.encrypt(token, nonce)
            )
    return newid

class UserLogin(BaseModel):
    name: str
    cin: str
    account_type: AccountType

async def login(pool: Pool, cse: ColumnCryptor, uid: int, token: bytes) -> UserLogin:
    async with pool.acquire() as con:
        user = await con.fetchrow(
            "select name, cin, account_type, enc_nonce, token from token, userinfo where token.id = $1 and token.id = userinfo.id;",
            uid
        )

        if not constant_time.bytes_eq(cse.decrypt(user["token"], user["enc_nonce"]), token):
            raise ValueError("Token is not equal to the one stored in the db")

    acctype = account_type_from_int(user["account_type"])
    if acctype is None:
        raise ValueError("Invalid account type")
    return UserLogin(
        name=cse.decrypt(user["name"], user["enc_nonce"]).decode(),
        account_type=acctype,
        cin=cse.decrypt(user["cin"], user["enc_nonce"]).decode()
    )
