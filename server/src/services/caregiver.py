from typing import Any, Dict
from aiostream import stream
from fastapi import HTTPException
from fastapi.requests import Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import json

from db import caregiver
from lang import Lang
from models.account import AccountType
from services import heartbeat
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

class CareGiverEventsReq(BaseModel):
    patient: int
    last_info: int
    last_instruction: int

async def events(request: Request, req: CareGiverEventsReq):
    await authenticate(request)

    trans = Lang(request.state.locale)

    if request.state.session.account_type != AccountType.CareGiver:
        raise HTTPException(status_code=401, detail=trans.t("not_caregiver2"))

    try:
        assigned = await caregiver.caregiver_assigned_to_patient(
            request.app.state.db,
            request.state.session.uid,
            req.patient
        )
        if not assigned:
            raise HTTPException(status_code=401, detail=trans.t("not_assigned"))
    except:
        raise HTTPException(status_code=500)

    return StreamingResponse(stream.merge(caregiver_events_iter(request, req), heartbeat()), media_type="text/event-stream")

async def caregiver_events_iter(request: Request, req: CareGiverEventsReq):
    async with (
            request.app.state.listener.subscribe(channel="instruction_{}".format(req.patient)) as s1,
            request.app.state.listener.subscribe(channel="patient_info_{}".format(req.patient)) as s2
        ):
        read_pat = True
        read_ins = True
        while True:
            if read_ins:
                async for row in caregiver.get_instructions(
                    request.app.state.db,
                    request.app.state.cse,
                    request.state.session.uid,
                    req.patient,
                    req.last_instruction
                    ):
                    yield "event:instruction\ndata:{}\n\n".format(row.model_dump_json())
                    req.last_instruction = row.id
                read_ins = False

            # here we want to only get instructions done by other caregivers
            if read_pat:
                async for row in caregiver.get_patient_info(
                    request.app.state.db,
                    request.app.state.cse,
                    req.patient,
                    req.last_info
                    ):
                    yield "event:patient_info\ndata:{}\n\n".format(row.model_dump_json())
                    req.last_info = row.id
                read_pat = False

            async for event in stream.merge(s1, s2):
                if event.channel.startswith("instruction"):
                    if event.message[0] == request.state.session.uid:
                        continue
                    last = event.message[1]
                    if last > req.last_instruction:
                        read_ins = True
                        break
                elif event.channel.startswith("patient"):
                    last = event.message
                    if last > req.last_info:
                        read_pat = True
                        break
