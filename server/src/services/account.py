from concurrent.futures import ProcessPoolExecutor
from fastapi import HTTPException, Request, responses
import msgpack
import qrcode
from io import BytesIO
import base64
from pydantic import BaseModel

from db import account as daccount
from lang import Lang

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
        nonce = token[:12]
        dectk = msgpack.unpackb(request.app.state.cse.decrypt(token[12:], nonce), raw=False)
        uid   = dectk["id"]
        token = dectk["t"]
    except:
        raise HTTPException(status_code=400, detail=trans.t("ivt"))
    
    try:
        (resp, cookie) = await daccount.login(request.app.state.db, request.app.state.cse, uid, token)
        
        resp   = responses.JSONResponse(content=dict(resp))
        nonce  = request.app.state.cse.gen_nonce()
        cookie = request.app.state.cse.encrypt(msgpack.packb({"c": cookie, "id": uid}), nonce)
        cookie = base64.urlsafe_b64encode(cookie).decode('utf-8')

        resp.set_cookie(key="session", value=cookie)
        return resp
    except:
        raise HTTPException(status_code=400, detail=trans.t("ivt2"))
