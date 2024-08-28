import os
from fastapi import FastAPI
from contextlib import asynccontextmanager

from db import close_db_connection, connect_to_db
from i18n import i18nMiddleware
from models import account as maccount
from services import account as saccount
from lang import Lang

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.db = await connect_to_db()
    yield
    await close_db_connection(app.state.db)

if "PRODUCTION" in os.environ:
    app = FastAPI(lifespan=lifespan, docs_url=None, redoc_url=None)
else:
    app = FastAPI(lifespan=lifespan)

app.add_middleware(i18nMiddleware)


@app.get("/v1/join_qr/{token}")
async def join_qr(token: str):
    return await saccount.join_token_qrcode(token)
