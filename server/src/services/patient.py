import asyncio
from typing import Any, Dict
from fastapi import HTTPException
from fastapi.requests import Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import json
from aiostream import stream

from db import patient
from lang import Lang
from models.account import AccountType
from services import heartbeat
from services.account import authenticate

class AddInfo(BaseModel):
    data: Dict[str, Any]

async def add_info(request: Request, info: AddInfo):
    await authenticate(request)

    trans = Lang(request.state.locale)

    if request.state.session.account_type == AccountType.CareGiver:
        raise HTTPException(status_code=401, detail=trans.t("not_patient"))

    try:
        await patient.add_info(
            request.app.state.db,
            request.app.state.cse,
            request.state.session.uid,
            json.dumps(info.data).encode()
        )
    except:
        raise HTTPException(status_code=401, detail=trans.t("add_info_failed"))
    
    return {}

class EventsReq(BaseModel):
    last: int

async def events(request: Request, req: EventsReq):
    await authenticate(request)

    trans = Lang(request.state.locale)

    if request.state.session.account_type == AccountType.CareGiver:
        raise HTTPException(status_code=401, detail=trans.t("not_patient"))

    return StreamingResponse(stream.merge(instruction_iter(request, req), heartbeat()), media_type="text/event-stream")

async def instruction_iter(request: Request, req: EventsReq):
    async with request.app.state.listener.subscribe(channel="instruction_{}".format(request.state.session.uid)) as subscriber:
        while True:
            async for row in patient.get_instructions(
                request.app.state.db,
                request.app.state.cse,
                request.state.session.uid,
                req.last
                ):
                yield "event:instruction\ndata:{}\n\n".format(row.model_dump_json())
                req.last = row.id

            async for event in subscriber:
                last = int(event.message)
                if last > req.last:
                    break

