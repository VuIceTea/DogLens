import asyncio
import os
from contextlib import asynccontextmanager
from functools import partial
from io import BytesIO
from pathlib import Path
from time import time
from typing import Annotated

import cachecontrol
import requests
from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.concurrency import run_in_threadpool
from google.auth.exceptions import TransportError
from google.auth.transport.requests import Request as GoogleRequest
from google.oauth2.id_token import verify_firebase_token
from PIL import Image, ImageOps, UnidentifiedImageError

from .inference import DogRecognition, NoMatch

ROOT = Path(__file__).resolve().parents[1]
MAX_UPLOAD = 8 * 1024 * 1024
MAX_PIXELS = 12_000_000


@asynccontextmanager
async def lifespan(app: FastAPI):
    project = os.environ.get("FIREBASE_PROJECT_ID")
    if not project:
        raise RuntimeError("FIREBASE_PROJECT_ID is required")
    detector = Path(os.environ.get("DOG_DETECTOR_MODEL", ROOT / "recognition_backend/models/yolox_s.onnx"))
    catalog = Path(os.environ.get("DOG_CATALOG_DIR", ROOT / "app/src/main/assets/recognition"))
    app.state.model = await run_in_threadpool(DogRecognition, detector, catalog)
    app.state.project = project
    app.state.inference_slots = asyncio.Semaphore(2)
    with cachecontrol.CacheControl(requests.Session()) as session:
        app.state.auth_transport = partial(GoogleRequest(session), timeout=5)
        yield


app = FastAPI(lifespan=lifespan)


def require_firebase_user(request: Request, authorization: Annotated[str | None, Header()] = None):
    scheme, _, token = (authorization or "").partition(" ")
    if scheme.lower() != "bearer" or token.count(".") != 2 or len(token) > 8192:
        raise HTTPException(401, "A Firebase ID token is required")
    try:
        claims = verify_firebase_token(
            token, request.app.state.auth_transport, audience=request.app.state.project
        )
        auth_time = claims.get("auth_time")
        if len(claims["sub"]) > 128 or type(auth_time) is not int or not 0 < auth_time <= time():
            raise ValueError("Invalid Firebase claims")
    except TransportError:
        raise HTTPException(503, "Authentication service temporarily unavailable") from None
    except (ValueError, TypeError, KeyError):
        raise HTTPException(401, "Invalid or expired Firebase ID token") from None


def recognize_jpeg(model: DogRecognition, data: bytearray):
    try:
        with Image.open(BytesIO(data)) as opened:
            if opened.format != "JPEG":
                raise HTTPException(415, "Expected a JPEG image")
            if opened.width * opened.height > MAX_PIXELS:
                raise HTTPException(413, "Image has too many pixels")
            image = ImageOps.exif_transpose(opened).convert("RGB")
            image.thumbnail((1600, 1600))
    except (UnidentifiedImageError, OSError, ValueError, Image.DecompressionBombError):
        raise HTTPException(400, "Invalid image") from None
    try:
        return model.recognize(image)
    except NoMatch as error:
        raise HTTPException(422, str(error)) from None
    finally:
        image.close()


@app.get("/health")
def health():
    return {"status": "ready"}


@app.post("/recognize", dependencies=[Depends(require_firebase_user)])
async def recognize(request: Request):
    if request.headers.get("content-type", "").split(";", 1)[0].strip().lower() != "image/jpeg":
        raise HTTPException(415, "Expected image/jpeg")
    data = bytearray()
    async for chunk in request.stream():
        if len(data) + len(chunk) > MAX_UPLOAD:
            raise HTTPException(413, "Image exceeds 8 MiB")
        data.extend(chunk)
    async with request.app.state.inference_slots:
        return await run_in_threadpool(recognize_jpeg, request.app.state.model, data)
