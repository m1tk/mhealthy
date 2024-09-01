from asyncpg import Pool

from db.column_cryptor import ColumnCryptor


async def add_info(pool: Pool, cse: ColumnCryptor,
                   caregiver: int, info: bytes):
    async with pool.acquire() as con:
        nonce = cse.gen_nonce()
        await con.execute('''
insert into patient_info (patient, info, enc_nonce)
values ($1, $2, $3);
            ''',
            caregiver,
            cse.encrypt(info, nonce),
            nonce
        )
