from fastapi import FastAPI
from app.routers import search

app = FastAPI()

app.include_router(search.router)
