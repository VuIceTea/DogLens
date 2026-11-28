import json
from math import exp
from pathlib import Path

import numpy as np
import onnxruntime as ort
from PIL import Image


class NoMatch(Exception):
    pass


class DogRecognition:
    detector_size = 640
    dog_class_index = 16
    dog_threshold = 0.45
    breed_threshold = 0.65
    breed_margin = 0.15

    def __init__(self, detector_path: Path, catalog_dir: Path):
        options = ort.SessionOptions()
        options.intra_op_num_threads = 2
        options.inter_op_num_threads = 1
        providers = ["CPUExecutionProvider"]
        self.detector = ort.InferenceSession(str(detector_path), options, providers=providers)
        self.classifier = ort.InferenceSession(str(catalog_dir / "breed-classifier-int8.onnx"), options, providers=providers)
        self.dachshund_vision = ort.InferenceSession(
            str(catalog_dir / "clip-dachshund-vision.onnx"), options, providers=providers
        )
        self.dachshund_head = json.loads((catalog_dir / "dachshund-head.json").read_text(encoding="utf-8"))
        self.dachshund_weights = np.array(self.dachshund_head["weights"], np.float64)
        self.catalog = json.loads((catalog_dir / "breed-catalog.json").read_text(encoding="utf-8"))
        metadata = json.loads((catalog_dir / "model-metadata.json").read_text(encoding="utf-8"))
        self.temperature = float(metadata["calibration"]["temperature"])
        self.detector_input = self.detector.get_inputs()[0].name
        self.classifier_input = self.classifier.get_inputs()[0].name
        self.dachshund_input = self.dachshund_vision.get_inputs()[0].name
        grids, strides = [], []
        for stride in (8, 16, 32):
            y, x = np.mgrid[:self.detector_size // stride, :self.detector_size // stride]
            grids.append(np.stack((x, y), axis=-1).reshape(-1, 2))
            strides.append(np.full(x.size, stride, dtype=np.float32))
        self.grid = np.concatenate(grids).astype(np.float32)
        self.stride = np.concatenate(strides)

    def find_dog(self, image: Image.Image) -> tuple[float, float, float, float] | None:
        size = self.detector_size
        ratio = size / max(image.size)
        scaled_width = max(1, int(image.width * ratio))
        scaled_height = max(1, int(image.height * ratio))
        rgb = np.asarray(image.resize((scaled_width, scaled_height), Image.Resampling.BILINEAR))
        padded = np.full((size, size, 3), 114, dtype=np.uint8)
        padded[:scaled_height, :scaled_width] = rgb[:, :, ::-1]
        tensor = np.ascontiguousarray(padded.transpose(2, 0, 1)[None], dtype=np.float32)
        predictions = self.detector.run(None, {self.detector_input: tensor})[0][0]
        classes = predictions[:, 5:]
        # Each candidate must be identified as a dog by the detector itself.
        dog_scores = predictions[:, 4] * classes[:, self.dog_class_index]
        candidates = np.flatnonzero(
            (classes.argmax(axis=1) == self.dog_class_index) & (dog_scores >= self.dog_threshold)
        )
        for index in candidates[np.argsort(-dog_scores[candidates])]:
            center = (predictions[index, :2] + self.grid[index]) * self.stride[index]
            half_size = np.exp(predictions[index, 2:4]) * self.stride[index] / 2
            scale = np.array([scaled_width / image.width, scaled_height / image.height])
            low = np.clip((center - half_size) / scale, 0, image.size)
            high = np.clip((center + half_size) / scale, 0, image.size)
            if np.isfinite(low).all() and np.isfinite(high).all() and (high - low).min() >= 24:
                return float(low[0]), float(low[1]), float(high[0]), float(high[1])
        return None

    def detect_dog(self, image: Image.Image) -> tuple[float, float, float, float]:
        box = self.find_dog(image)
        if box is not None:
            return box

        # Retry at a closer scale with overlapping crops, using the same dog threshold.
        width, height = image.size
        landscape = width >= height
        crop_width = round(width * (.55 if landscape else .8))
        crop_height = round(height * (.8 if landscape else .55))
        if min(crop_width, crop_height) >= 24:
            for position in (.55, .45):
                left = round(width * (position if landscape else .4) - crop_width / 2)
                top = round(height * (.4 if landscape else position) - crop_height / 2)
                with image.crop((left, top, left + crop_width, top + crop_height)) as crop:
                    box = self.find_dog(crop)
                if box is not None:
                    x1, y1, x2, y2 = box
                    return x1 + left, y1 + top, x2 + left, y2 + top
        raise NoMatch("no_dog")

    def classify(self, image: Image.Image, box: tuple[float, float, float, float]):
        x1, y1, x2, y2 = box
        side = max(x2 - x1, y2 - y1) * 1.18
        left = (x1 + x2 - side) / 2
        top = (y1 + y2 - side) / 2
        # Keep the full detected region, including ears or paws close to the edges.
        crop = image.transform(
            (224, 224), Image.Transform.EXTENT, (left, top, left + side, top + side),
            resample=Image.Resampling.BILINEAR, fillcolor=(114, 114, 114),
        )
        rgb = np.asarray(crop, dtype=np.float32) / 255
        dachshund = self.classify_dachshund(rgb)
        if dachshund is not None:
            return dachshund
        rgb = (rgb - np.array([.485, .456, .406], np.float32)) / np.array([.229, .224, .225], np.float32)
        tensor = np.ascontiguousarray(rgb.transpose(2, 0, 1)[None])
        logits = self.classifier.run(None, {self.classifier_input: tensor})[0][0]
        if len(logits) != len(self.catalog) or not np.isfinite(logits).all():
            raise RuntimeError("Invalid classifier output")
        weights = np.exp((logits - logits.max()) / self.temperature)
        scores = weights / weights.sum()
        first, second = np.argsort(-scores)[:2]
        match = self.catalog[first]
        confidence = float(scores[first])
        if match is None or confidence < self.breed_threshold or confidence - float(scores[second]) < self.breed_margin:
            raise NoMatch("uncertain_breed")
        return {"dogId": match["id"], "confidence": min(99, round(confidence * 100))}

    def classify_dachshund(self, rgb: np.ndarray):
        rgb = (rgb - np.array([.48145466, .4578275, .40821073], np.float32)) / np.array(
            [.26862954, .26130258, .27577711], np.float32
        )
        tensor = np.ascontiguousarray(rgb.transpose(2, 0, 1)[None])
        vector = self.dachshund_vision.run(None, {self.dachshund_input: tensor})[0][0].astype(np.float64)
        if vector.shape != self.dachshund_weights.shape or not np.isfinite(vector).all():
            raise RuntimeError("Invalid Dachshund embedding")
        norm = np.linalg.norm(vector)
        if norm <= 0:
            return None
        score = float(vector @ self.dachshund_weights / norm)
        head = self.dachshund_head
        if score < head["minimumScore"]:
            return None
        log_odds = score * head["calibrationScale"] + head["calibrationBias"]
        return {"dogId": head["dogId"], "confidence": min(99, round(100 / (1 + exp(-log_odds))))}

    def recognize(self, image: Image.Image):
        return self.classify(image, self.detect_dog(image))
