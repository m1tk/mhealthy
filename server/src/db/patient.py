import json
from typing import Any, Dict
from asyncpg import Pool
from pydantic import BaseModel

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


class Instruction(BaseModel):
    instruction: Dict[str, Any]
    caregiver: int
    id: int

async def get_instructions(pool: Pool, cse: ColumnCryptor,
                           patient: int, start: int):
    async with pool.acquire() as con:
        async with con.transaction():
            async for row in con.cursor('''
select id, caregiver, instruction, enc_nonce from caregiver_instruction
where patient = $1 and id > $2;
                ''',
                patient,
                start
                ):
                yield Instruction(
                    instruction=json.loads(cse.decrypt(row["instruction"], row["enc_nonce"])),
                    id=row["id"],
                    caregiver=row["caregiver"]
                )
