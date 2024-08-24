import os
from fastapi import FastAPI

if "PRODUCTION" in os.environ:
    app = FastAPI(docs_url=None, redoc_url=None)
else:
    app = FastAPI()


@app.get("/")
def read_root():
    return {"message": "Hello World"}
