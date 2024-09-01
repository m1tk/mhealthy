from typing import Any, Dict
from fastapi import HTTPException
from fastapi.requests import Request
from pydantic import BaseModel
import json

from db import caregiver
from lang import Lang
from models.account import AccountType
from services.account import authenticate

class AddInstruction(BaseModel):
    patient: int
    data: Dict[str, Any]

async def add_instruction(request: Request, instruction: AddInstruction):
    await authenticate(request)

    trans = Lang(request.state.locale)

    if request.state.session.account_type == AccountType.Patient:
        raise HTTPException(status_code=401, detail=trans.t("not_caregiver"))

    try:
        await caregiver.add_instruction(
            request.app.state.db,
            request.app.state.cse,
            request.state.session.uid,
            instruction.patient,
            json.dumps(instruction.data).encode()
        )
    except:
        raise HTTPException(status_code=401, detail=trans.t("add_instruction_failed"))
    
    return {}
