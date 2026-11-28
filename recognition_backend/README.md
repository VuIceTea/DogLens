# Backend nhận diện DogLens

Ảnh từ camera hoặc thư viện đi qua màn chỉnh ảnh rồi được gửi bằng JPEG tới `POST /recognize`. API xác thực Firebase ID token của dự án DogLens. YOLOX phát hiện vùng chó trước. Mô hình ONNX chỉ phân loại giống trên vùng đã phát hiện và trả về mã giống trong danh mục DogLens nếu đủ tin cậy. Ảnh chỉ được xử lý trong bộ nhớ.

Khi build app với `DOG_RECOGNITION_API_URL` để trống, Android chạy YOLOX-S và mô hình phân loại ngay trên máy. Hai chế độ cùng thử vùng ảnh gần hơn khi lượt phát hiện toàn ảnh không tìm thấy chó. Tệp `yolox_s.onnx` của app lấy từ [bản phát hành YOLOX chính thức](https://github.com/Megvii-BaseDetection/YOLOX/releases/tag/0.1.1rc0). Script tải của backend kiểm tra SHA-256 của cùng tệp này.

## Dachshund

Sau khi phát hiện chó, Android và backend kiểm tra Dachshund bằng một bộ phân loại tuyến tính trên vector ảnh 512 chiều từ [CLIP ViT-B/32 ONNX INT8](https://huggingface.co/Xenova/clip-vit-base-patch32). Nếu điểm không đạt ngưỡng, hai chế độ tiếp tục với bộ phân loại giống hiện có.

`dachshund-training-sources.json` lưu nguồn 71 ảnh Dachshund. Bộ đối chứng gồm 96 ảnh giống khác trong danh mục DogLens mà YOLOX phát hiện được. Kiểm tra chéo 4 phần trên tập đã chọn cho kết quả 50/71 ảnh Dachshund vượt ngưỡng và 0/96 ảnh đối chứng bị gán Dachshund. Hai ảnh gốc do người dùng cung cấp được giữ ngoài tập huấn luyện. Đây là phép thử trên tập nhỏ, chưa đo được độ chính xác trên toàn bộ giống hoặc các biến thể hiếm.

Phần thị giác lấy từ `onnx/vision_model_quantized.onnx` của kho trên, SHA-256 `583fd1110a514667812fee7d684952aaf82a99b959760c8d7dca7e0ab9839299`. Mô hình gốc mang giấy phép MIT, được đóng gói cùng APK để hai luồng không cần gửi ảnh tới dịch vụ thứ ba.

## Chạy thử trên Windows

Chạy từ thư mục gốc dự án với Python 3.12.

```powershell
py -3.12 -m venv recognition_backend/.venv
.\recognition_backend\.venv\Scripts\python.exe -m pip install -r recognition_backend/requirements-dev.txt
.\recognition_backend\.venv\Scripts\python.exe recognition_backend/fetch_detector.py
.\recognition_backend\.venv\Scripts\python.exe -m unittest recognition_backend.test_api
$env:FIREBASE_PROJECT_ID = 'doglens-c2867'
.\recognition_backend\.venv\Scripts\python.exe -m uvicorn recognition_backend.api:app --host 127.0.0.1 --port 8765
```

Mở terminal thứ hai, chạy các lệnh sau rồi mở APK debug và đăng nhập.

```powershell
adb reverse tcp:8765 tcp:8765
.\gradlew.bat :app:assembleDebug '-PDOG_RECOGNITION_API_URL=http://127.0.0.1:8765/recognize'
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Cấu hình để chia sẻ APK

Build container từ thư mục gốc.

```text
docker build -f recognition_backend/Dockerfile -t doglens-recognition .
```

Triển khai container lên dịch vụ có HTTPS, cấu hình `FIREBASE_PROJECT_ID` trùng với dự án Firebase của app và kiểm tra `GET /health`. Với Cloud Run, cho phép request vào container ở lớp Cloud Run để API tự kiểm tra Firebase ID token. Build lại APK với `-PDOG_RECOGNITION_API_URL=https://<host>/recognize`. Bản build bỏ trống thuộc tính này dùng mô hình trên thiết bị.

API trả `200` kèm `dogId` và `confidence` khi có kết quả. `422/no_dog` là chưa phát hiện được chó. `422/uncertain_breed` là đã phát hiện chó nhưng chưa xác định được giống đủ tin cậy. Điểm tin cậy mô hình phụ thuộc ảnh và danh mục giống hỗ trợ.
