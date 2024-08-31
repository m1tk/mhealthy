import os
from fastapi import FastAPI, HTTPException, Request
from contextlib import asynccontextmanager

from fastapi.responses import JSONResponse

from db import close_db_connection, connect_to_db
from db.column_cryptor import ColumnCryptor
from i18n import i18nMiddleware
from services import account as saccount

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.db  = await connect_to_db()
    # Client side encryption for sensitive columns to be stored to database
    app.state.cse = ColumnCryptor()

    yield
    await close_db_connection(app.state.db)

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

@app.get("/v1/join_qr/{token}")
async def join_qr(token: str):
    return await saccount.join_token_qrcode(token)

@app.post("/v1/login")
async def login(request: Request, req: saccount.LoginRequest):
    return await saccount.login(request, req)
