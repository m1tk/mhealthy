import asyncio
from typing import Optional
from concurrent.futures import ProcessPoolExecutor
from asyncpg import Pool
from fastapi import HTTPException, responses
import qrcode
from io import BytesIO
import base64
from pydantic import BaseModel

from db import account as daccount
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

async def login(state, req: LoginRequest):
    # this is first ever login
    try:
        token = base64.urlsafe_b64decode(req.token)
    except:
        raise HTTPException(status_code=400, detail={"error": "Invalid join token given"})
    
    try:
        (resp, cookie) = await daccount.login(state.db, state.cse, token)
        resp = responses.JSONResponse(content=dict(resp))
        resp.set_cookie(key="session", value=cookie)
        return resp
    except Exception as e:
        print(e)
        raise HTTPException(status_code=400, detail={"error": "Invalid token or user never registered"})
