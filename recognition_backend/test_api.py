import unittest
from io import BytesIO
from unittest.mock import patch

from fastapi.testclient import TestClient
from PIL import Image

from .api import ROOT, app, require_firebase_user


class RecognitionApiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.enterClassContext(patch.dict("os.environ", {"FIREBASE_PROJECT_ID": "doglens-test"}))
        cls.client = cls.enterClassContext(TestClient(app))

    def post_image(self, data):
        with patch.dict(app.dependency_overrides, {require_firebase_user: lambda: None}):
            return self.client.post("/recognize", headers={"Content-Type": "image/jpeg"}, content=data)

    def test_requires_firebase_id_token(self):
        self.assertEqual(self.client.post("/recognize").status_code, 401)
        invalid = self.client.post("/recognize", headers={"Authorization": "Bearer invalid"})
        self.assertEqual(invalid.status_code, 401)

    def test_known_dog_returns_catalog_id(self):
        fixture = ROOT / "app/src/androidTest/assets/labrador_on_beach.jpg"
        response = self.post_image(fixture.read_bytes())
        self.assertEqual(response.status_code, 200, response.text)
        self.assertEqual(response.json()["dogId"], "labrador_retriever")
        self.assertGreaterEqual(response.json()["confidence"], 65)

    def test_image_without_dog_never_reaches_classifier(self):
        with Image.new("RGB", (320, 240), "gray") as image, BytesIO() as stream:
            image.save(stream, "JPEG")
            with patch.object(app.state.model, "classify") as classify:
                response = self.post_image(stream.getvalue())
                classify.assert_not_called()
        self.assertEqual(response.status_code, 422)
        self.assertEqual(response.json(), {"detail": "no_dog"})

    def test_invalid_image_returns_bad_request(self):
        self.assertEqual(self.post_image(b"not a jpeg").status_code, 400)
