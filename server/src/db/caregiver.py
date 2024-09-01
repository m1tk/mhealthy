from asyncpg import Pool

from db.column_cryptor import ColumnCryptor


async def add_instruction(pool: Pool, cse: ColumnCryptor,
                          caregiver: int, patient: int,
                          instruction: bytes):
    async with pool.acquire() as con:
        nonce = cse.gen_nonce()
        await con.execute('''
insert into caregiver_instruction (caregiver, patient, instruction, enc_nonce)
values ($1, $2, $3, $4);
            ''',
            caregiver,
            patient,
            cse.encrypt(instruction, nonce),
            nonce
        )
