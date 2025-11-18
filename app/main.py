from fastapi import FastAPI
from app.routers import detect

app = FastAPI()

app.include_router(detect.router)
