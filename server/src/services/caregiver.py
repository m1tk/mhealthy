import asyncio
from typing import Any, Dict
from aiostream import stream
from asyncpg import UniqueViolationError
from fastapi import HTTPException
from fastapi.requests import Request
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import json

from db import caregiver
from lang import Lang
from models.account import AccountType
from services import heartbeat, heartbeat_inter
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
        raise HTTPException(status_code=401, detail=trans.t("not_caregiver"))

    try:
        assigned = await caregiver.caregiver_assigned_to_patient(
            request.app.state.db,
            request.state.session.uid,
            req.patient
        )
    except:
        raise HTTPException(status_code=500)

    if not assigned:
        raise HTTPException(status_code=401, detail=trans.t("not_assigned"))

    stop = asyncio.Event()
    return StreamingResponse(stream.merge(caregiver_events_iter(request, req, stop), heartbeat_inter(stop)), media_type="text/event-stream")

async def caregiver_events_iter(request: Request, req: CareGiverEventsReq, stop: asyncio.Event):
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
                    if row is None:
                        stop.set()
                        return
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

            async with stream.merge(s1, s2).stream() as s:
                async for event in s:
                    if event.channel.startswith("instruction"):
                        #if event.message[0] == request.state.session.uid:
                        #    continue
                        last = event.message[1]
                        if last > req.last_instruction:
                            read_ins = True
                            break
                    elif event.channel.startswith("patient"):
                        last = event.message
                        if last > req.last_info:
                            read_pat = True
                            break

class CareGiverAssign(BaseModel):
    patient: int
    new_caregiver: int

async def assign_caregiver(request: Request, assign: CareGiverAssign):
    await authenticate(request)

    trans = Lang(request.state.locale)

    if request.state.session.account_type != AccountType.CareGiver:
        raise HTTPException(status_code=401, detail=trans.t("not_caregiver"))

    if assign.new_caregiver == request.state.session.uid:
        raise HTTPException(status_code=401, detail=trans.t("no_self_assign"))

    try:
        assigned = await caregiver.caregiver_assigned_to_patient(
            request.app.state.db,
            request.state.session.uid,
            assign.patient
        )
    except:
        raise HTTPException(status_code=500)

    if not assigned:
        raise HTTPException(status_code=401, detail=trans.t("not_assigned"))


    try:
        is_caregiver = await caregiver.assign_caregiver_to_patient(
                request.app.state.db,
                request.app.state.cse,
                assign.new_caregiver,
                assign.patient,
                caregiver=request.state.session.uid
        )
    except UniqueViolationError:
        raise HTTPException(status_code=401, detail=trans.t("caregiver_already_assigned"))

    if not is_caregiver:
        raise HTTPException(status_code=401, detail=trans.t("assignee_not_caregiver"))
    return {}

async def assigned_events(request: Request):
    await authenticate(request)

    trans = Lang(request.state.locale)

    if request.state.session.account_type != AccountType.CareGiver:
        raise HTTPException(status_code=401, detail=trans.t("not_caregiver"))

    return StreamingResponse(stream.merge(assigned_events_iter(request), heartbeat()), media_type="text/event-stream")

async def assigned_events_iter(request: Request):
    async with request.app.state.listener.subscribe(channel="assigned_{}".format(request.state.session.uid)) as subscriber:
        async for patient_id in caregiver.get_assigned(
            request.app.state.db,
            request.state.session.uid,
            ):
            yield "event:assigned\ndata:{}\n\n".format(patient_id)

        while True:
            async for event in subscriber:
                patient_id = event.message
                yield "event:assigned\ndata:{}\n\n".format(patient_id)

