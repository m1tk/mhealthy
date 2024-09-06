from concurrent.futures import ProcessPoolExecutor
import secrets
from fastapi import HTTPException, Request, responses
import msgpack
import qrcode
from io import BytesIO
import base64
from pydantic import BaseModel

from db import account as daccount
from lang import Lang
from models.account import AccountSession, AccountType

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
        resp   = await daccount.login(request.app.state.db, request.app.state.cse, uid, token)
        cookie = secrets.token_bytes(48)
        
        cookie = {"c": cookie, "id": uid}
        # we also add type of account so we can later on determine api calls allowed by type
        if resp.account_type == AccountType.CareGiver:
            cookie["g"] = True
        elif resp.account_type == AccountType.SelfCarerPatient:
            cookie["s"] = True
        
        resp.account_type = resp.account_type.value

        resp   = responses.JSONResponse(content=dict(resp))
        nonce  = request.app.state.cse.gen_nonce()
        cookie = request.app.state.cse.encrypt(msgpack.packb(cookie), nonce)
        cookie = base64.urlsafe_b64encode(nonce + cookie).decode('utf-8')

        resp.set_cookie(key="session", value=cookie)
        return resp
    except:
        raise HTTPException(status_code=400, detail=trans.t("ivt2"))

async def authenticate(request: Request):
    trans  = Lang(request.state.locale)
    cookie = request.cookies.get("session")
    if cookie is None:
        raise HTTPException(status_code=401, detail=trans.t("login_needed"))

    try:
        cookie = base64.urlsafe_b64decode(cookie)
        # Cookie is composed of the nonce and encrypted part which is the cookie ciphertext
        cookie = msgpack.unpackb(request.app.state.cse.decrypt(cookie[12:], cookie[:12]), raw=False)
        if "g" in cookie:
            atype = AccountType.CareGiver
        elif "s" in cookie:
            atype = AccountType.SelfCarerPatient
        else:
            atype = AccountType.Patient
        
        session = AccountSession(uid=cookie["id"], account_type=atype)
    except:
        raise HTTPException(status_code=401, detail=trans.t("ivt2"))

    request.state.session = session
