import os
from fastapi import FastAPI

from models import account as maccount
from services import account as saccount

if "PRODUCTION" in os.environ:
    app = FastAPI(docs_url=None, redoc_url=None)
else:
    app = FastAPI()


@app.get("/v1/join_qr/{token}")
async def join_qr(token: str):
    return await saccount.join_token_qrcode(token)
