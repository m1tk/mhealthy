from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware, RequestResponseEndpoint

class i18nMiddleware(BaseHTTPMiddleware):
    # for now default locale is fr
    
    async def dispatch(self, request: Request, call_next: RequestResponseEndpoint) -> Response:
        request.state.locale = "fr"
        return await call_next(request)
