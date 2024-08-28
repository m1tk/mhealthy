import os
from fastapi import FastAPI
from fastapi.requests import Request

from i18n import i18nMiddleware
from models import account as maccount
from services import account as saccount
from lang import Lang

if "PRODUCTION" in os.environ:
    app = FastAPI(docs_url=None, redoc_url=None)
else:
    app = FastAPI()

app.add_middleware(i18nMiddleware)

@app.get("/v1/join_qr/{token}")
async def join_qr(token: str):
    return await saccount.join_token_qrcode(token)
