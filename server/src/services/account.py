import argon2
from argon2 import PasswordHasher
import asyncio
from typing import Optional
from concurrent.futures import ProcessPoolExecutor
from fastapi import HTTPException, responses
import json
import qrcode
from io import BytesIO

from models.account import Account

executor = ProcessPoolExecutor()


async def join_token_qrcode(token: str):
    # TODO: Check if token exists
    if False:
        raise HTTPException(status_code=404)

    image  = qrcode.make(json.dumps({"type":"join", "token": token}))
    buffer = BytesIO()
    image.save(buffer, format="PNG")
    buffer.seek(0)

    return responses.StreamingResponse(buffer, media_type="image/png")

async def activate_account(join_token: str, secret: str):
    # TODO: Check token here before proceeding

    loop = asyncio.get_event_loop()
    hash = await loop.run_in_executor(executor, calculate_secret_hash, secret)
    if hash is None:
        return
    
    # TODO: now we activate account and we remove join token

def calculate_secret_hash(secret: str) -> Optional[str]:
    ph = PasswordHasher()
    try:
        return ph.hash(secret)
    except argon2.exceptions.HashingError:
        return None

async def authenticate(token: str):
    pass
