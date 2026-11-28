FROM python:3.12-slim
WORKDIR /app
COPY recognition_backend/requirements.txt recognition_backend/requirements.txt
RUN pip install --no-cache-dir -r recognition_backend/requirements.txt
COPY recognition_backend/api.py recognition_backend/inference.py recognition_backend/fetch_detector.py recognition_backend/
COPY app/src/main/assets/recognition/breed-classifier-int8.onnx app/src/main/assets/recognition/breed-catalog.json app/src/main/assets/recognition/model-metadata.json app/src/main/assets/recognition/classifier-license.txt app/src/main/assets/recognition/
COPY app/src/main/assets/recognition/clip-dachshund-vision.onnx app/src/main/assets/recognition/clip-license.txt app/src/main/assets/recognition/dachshund-head.json app/src/main/assets/recognition/
COPY recognition_backend/dachshund-training-sources.json recognition_backend/
RUN python recognition_backend/fetch_detector.py
ENV PYTHONUNBUFFERED=1
USER 65532
CMD ["sh", "-c", "exec uvicorn recognition_backend.api:app --host 0.0.0.0 --port ${PORT:-8080}"]
