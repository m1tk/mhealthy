from datetime import datetime, timezone
import json
from typing import Any, Dict
from asyncpg import Pool
from pydantic import BaseModel

from db.column_cryptor import ColumnCryptor
from db.patient import Instruction
from models.account import AccountType, account_type_from_int


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

async def caregiver_assigned_to_patient(pool: Pool, caregiver: int, patient: int) -> bool:
    async with pool.acquire() as con:
        return await con.fetchval("select exists(select 1 from assigned where caregiver = $1 and patient = $2);", caregiver, patient)

class PatientInfo(BaseModel):
    info: Dict[str, Any]
    patient: int
    id: int

async def get_patient_info(pool: Pool, cse: ColumnCryptor,
                           patient: int, start: int):
    async with pool.acquire() as con:
        async with con.transaction():
            async for row in con.cursor('''
select id, patient, info, enc_nonce from patient_info
where patient = $1 and id > $2;
                ''',
                patient,
                start
                ):
                yield PatientInfo(
                    info=json.loads(cse.decrypt(row["info"], row["enc_nonce"])),
                    id=row["id"],
                    patient=row["patient"]
                )

async def get_instructions(pool: Pool, cse: ColumnCryptor,
                           caregiver: int, patient: int, start: int):
    async with pool.acquire() as con:
        async with con.transaction():
            async for row in con.cursor('''
select id, caregiver, instruction, enc_nonce from caregiver_instruction
where patient = $1 and id > $2 and caregiver != $3;
                ''',
                patient,
                start,
                caregiver
                ):
                yield Instruction(
                    instruction=json.loads(cse.decrypt(row["instruction"], row["enc_nonce"])),
                    id=row["id"],
                    caregiver=row["caregiver"]
                )


async def assign_caregiver_to_patient(pool: Pool, cse: ColumnCryptor,
                                      new_caregiver: int, patient: int,
                                      caregiver: int) -> bool:
    async with pool.acquire() as con:
        async with con.transaction():
            user = await con.fetchrow(
                "select name, phone, account_type, enc_nonce from userinfo where id = $1;",
                new_caregiver
            )

            acctype = account_type_from_int(user["account_type"])
            if acctype is None or acctype != AccountType.CareGiver:
                return False

            await con.execute("insert into assigned (caregiver, patient) values ($1, $2)", new_caregiver, patient)

            instruction = {
                "type": "assign_caregiver",
                "time": int(datetime.now(timezone.utc).timestamp()),
                "new_caregiver": {
                    "id": new_caregiver,
                    "name": cse.decrypt(user["name"], user["enc_nonce"]).decode(),
                    "phone": cse.decrypt(user["phone"], user["enc_nonce"]).decode()
                }
            }

            nonce = cse.gen_nonce()
            await con.execute('''
insert into caregiver_instruction (caregiver, patient, instruction, enc_nonce)
values ($1, $2, $3, $4);
                ''',
                caregiver,
                patient,
                cse.encrypt(json.dumps(instruction).encode(), nonce),
                nonce
            )
    return True
