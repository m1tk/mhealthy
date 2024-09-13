from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

class i18nMiddleware(BaseHTTPMiddleware):
    # for now default locale is fr
    
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        locale = request.headers.get('Accept-Language', None) or 'en'

        if locale not in ["en", "fr"]:
            locale = "en"
        request.state.locale = locale
        return await call_next(request)
