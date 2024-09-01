from typing import Any, Dict
from fastapi import HTTPException
from fastapi.requests import Request
from pydantic import BaseModel
import json

from db import patient
from lang import Lang
from models.account import AccountType
from services.account import authenticate

class AddInfo(BaseModel):
    data: Dict[str, Any]

async def add_info(request: Request, instruction: AddInfo):
    await authenticate(request)

    trans = Lang(request.state.locale)

    if request.state.session.account_type == AccountType.CareGiver:
        raise HTTPException(status_code=401, detail=trans.t("not_patient"))

    try:
        await patient.add_info(
            request.app.state.db,
            request.app.state.cse,
            request.state.session.uid,
            json.dumps(instruction.data).encode()
        )
    except:
        raise HTTPException(status_code=401, detail=trans.t("add_info_failed"))
    
    return {}
