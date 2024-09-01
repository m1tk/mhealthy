import os
import asyncpg_listen
from fastapi import FastAPI, HTTPException, Request
from contextlib import asynccontextmanager
from fastapi.responses import JSONResponse
from broadcaster import Broadcast

from db import close_db_connection, connect_to_db, event_listener
from db.column_cryptor import ColumnCryptor
from i18n import i18nMiddleware
from services import account as saccount, caregiver as sc, patient as sp

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.db  = await connect_to_db()
    # Client side encryption for sensitive columns to be stored to database
    app.state.cse = ColumnCryptor()
    # Broadcast for streaming events instead of creating multiple connections
    # as pg_listen to database
    app.state.listener = Broadcast("memory://")
    await app.state.listener.connect()
    await event_listener(event_handler)

    yield
    await close_db_connection(app.state.db)
    await app.state.listener.disconnect()

if "PRODUCTION" in os.environ:
    app = FastAPI(lifespan=lifespan, docs_url=None, redoc_url=None)
else:
    app = FastAPI(lifespan=lifespan)

app.add_middleware(i18nMiddleware)

@app.exception_handler(HTTPException)
async def custom_http_exception_handler(request: Request, exc: HTTPException):
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": exc.detail}
    )

async def event_handler(event: asyncpg_listen.NotificationOrTimeout):
    if isinstance(event, asyncpg_listen.Timeout) or event.payload is None:
        return
    print(f"Received notification '{event}' woooow")
    pay = event.payload.split(",")
    if event.channel == "patient_info":
        await app.state.listener.publish(channel="patient_info_{}".format(pay[0]), message=pay[1])
    else:
        await app.state.listener.publish(channel="instruction_{}".format(pay[1]), message=pay[2])

@app.get("/v1/join_qr/{token}")
async def join_qr(token: str):
    return await saccount.join_token_qrcode(token)

@app.post("/v1/login")
async def login(request: Request, req: saccount.LoginRequest):
    return await saccount.login(request, req)

@app.post("/v1/caregiver/instruction")
async def instruction(request: Request, req: sc.AddInstruction):
    return await sc.add_instruction(request, req)

@app.post("/v1/patient/info")
async def info(request: Request, req: sp.AddInfo):
    return await sp.add_info(request, req)

@app.post("/v1/patient/events")
async def patient_ev(request: Request, req: sp.EventsReq):
    return await sp.events(request, req)
