import hashlib
import shutil
from pathlib import Path
from urllib.request import urlopen

URL = "https://github.com/Megvii-BaseDetection/YOLOX/releases/download/0.1.1rc0/yolox_s.onnx"
SHA256 = "c5c2d13e59ae883e6af3b45daea64af4833a4951c92d116ec270d9ddbe998063"
MODEL = Path(__file__).resolve().parent / "models/yolox_s.onnx"

if __name__ == "__main__":
    MODEL.parent.mkdir(exist_ok=True)
    temporary = MODEL.with_suffix(".part")
    try:
        with urlopen(URL, timeout=60) as response, temporary.open("wb") as output:
            shutil.copyfileobj(response, output)
        with temporary.open("rb") as downloaded:
            if hashlib.file_digest(downloaded, "sha256").hexdigest() != SHA256:
                raise RuntimeError("Detector checksum does not match the release")
        temporary.replace(MODEL)
    finally:
        temporary.unlink(missing_ok=True)
