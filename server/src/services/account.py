import asyncio
from typing import Optional
from concurrent.futures import ProcessPoolExecutor
from asyncpg import Pool
from fastapi import HTTPException, Request, responses
import qrcode
from io import BytesIO
import base64
from pydantic import BaseModel

from db import account as daccount
from lang import Lang
from models.account import Account

executor = ProcessPoolExecutor()


async def join_token_qrcode(token: str):
    # It doesn't matter whether token is valid or not here
    image  = qrcode.make(token)
    buffer = BytesIO()
    image.save(buffer, format="PNG")
    buffer.seek(0)

    return responses.StreamingResponse(buffer, media_type="image/png")

class LoginRequest(BaseModel):
    token: str

async def login(request: Request, req: LoginRequest):
    # this is first ever login
    trans = Lang(request.state.locale)
    try:
        token = base64.urlsafe_b64decode(req.token)
    except:
        raise HTTPException(status_code=400, detail=trans.t("ivt"))
    
    try:
        (resp, cookie) = await daccount.login(request.app.state.db, request.app.state.cse, token)
        resp = responses.JSONResponse(content=dict(resp))
        resp.set_cookie(key="session", value=cookie)
        return resp
    except:
        raise HTTPException(status_code=400, detail=trans.t("ivt2"))
