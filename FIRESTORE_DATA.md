# Dữ liệu DogLens trên Firestore

Android đọc danh mục trực tiếp qua Firebase SDK. Các repository trả lại model cũ để RecyclerView và layout hiện tại tiếp tục hiển thị bình thường.

- `dogs`, `dogImages`, `traits`, `dogTraits`, `careTips`, `dogCareTips` và `dogSizeCategories` chứa danh mục giống chó.
- `articles`, `articleCategories`, `articleSections`, `explore`, `exploreCategories`, `featuredTopics` và `dogUseCases` chứa nội dung công khai.
- `app/src/main/assets/published-articles.json` là bản chụp ba collection bài viết công khai trên Firestore. App dùng bản này khi cả máy chủ lẫn bộ nhớ Firestore trên thiết bị đều chưa trả được nội dung. Khi cập nhật bài viết, có thể làm mới bản chụp bằng cách xuất lại các collection `articles`, `articleCategories` và `articleSections`, giữ nguyên mã tài liệu.
- `users/{uid}` có các collection con `favorites`, `recognitions`, `articleViews`, `articleBookmarks` và `notifications`. Người dùng chỉ đọc và cập nhật dữ liệu riêng của họ theo Firestore Rules. Khi bật tính năng xóa tài khoản, cấu hình dọn dữ liệu Firestore theo chế độ recursive.
- Ảnh danh mục hỗ trợ `imageUrl` HTTPS. Bộ nhập dùng tên `imageKey` để tìm ảnh drawable hiện có, tránh lưu ID Android thay đổi giữa các bản build.

## Nhập danh mục

Dữ liệu ban đầu trong `functions/seed-catalog.json` cung cấp các mã bài, mục và giống đã dùng trong app. Bản biên tập có nguồn nằm trong `functions/verified-breeds.json` và `functions/editorial-articles.json`. Một mã giống được giữ ổn định giữa mô hình, danh mục, lịch sử và yêu thích.

Với Application Default Credentials và dependencies Firebase đã cài, chạy:

```powershell
node functions/seed-catalog.js doglens-c2867 --verified
node functions/seed-catalog.js doglens-c2867 --verified --write
```

Trên Windows đã đăng nhập Firebase CLI cài qua npm, có thể đặt `$env:DOG_LENS_FIREBASE_CLI_PACKAGE = Join-Path $env:APPDATA 'npm/node_modules/firebase-tools'` để script dùng phiên đăng nhập hiện tại. Lệnh đầu báo số document sẽ tạo hoặc cập nhật. Lệnh thứ hai ghi danh mục công khai. Với `--verified`, bản ghi hiện có được cập nhật ở các trường còn trống hoặc còn nguyên giá trị từ lần nhập đầu. Bản ghi đã sửa thủ công được giữ ở các trường có giá trị riêng. Các liên kết mẫu về ảnh, tính khí và chăm sóc được đánh dấu để app ẩn khi bản ghi còn khớp bản nhập đầu.

## Nguồn và cách đọc số liệu

- Xuất xứ, chiều cao, cân nặng tiêu chuẩn và màu lông tham chiếu ưu tiên tài liệu của FCI. Một số cân nặng được quy đổi từ pound của AKC và có nhãn riêng. Số đo trong tiêu chuẩn có thể theo giới tính, kích cỡ hoặc là mốc mong muốn. [FCI](https://www.fci.be/en/nomenclature/)
- Tuổi thọ trung vị lấy từ Bảng bổ sung 3 của McMillan và cộng sự (2024), trên quần thể chó tại Anh. Khi thiếu cân nặng tiêu chuẩn, một số giống dùng Bảng 1 của nghiên cứu VetCompass hoặc cân nặng trung vị từ Danish Kennel Club được công bố trong phụ lục 2 của Egeriis và cộng sự (2026). Các số trung vị được ghi nhãn trong ứng dụng. [Nghiên cứu tuổi thọ](https://pubmed.ncbi.nlm.nih.gov/38302530/) · [VetCompass](https://doi.org/10.1371/journal.pone.0288081) · [Danish Kennel Club](https://doi.org/10.1186/s13028-026-00862-9)
- Ảnh được đối chiếu tên giống và giấy phép theo từng trang tệp Wikimedia Commons. App đóng gói bản thu nhỏ của 123 ảnh danh mục. Mục Thông tin ảnh trong hồ sơ ghi tác giả, tên giấy phép và liên kết gốc. Ảnh Collie minh họa biến thể lông dài. [Hướng dẫn tái sử dụng ảnh](https://commons.wikimedia.org/wiki/Commons:Reusing_content_outside_Wikimedia)
- Danh mục có 122 hồ sơ với nguồn cho xuất xứ, chiều cao, cân nặng, tuổi thọ, màu lông và ảnh. Cân nặng ở giữa khoảng tham chiếu từ bộ dữ liệu Bannasch được ghi riêng với cân nặng trung vị. Khi thiếu tuổi thọ trung vị, app hiển thị khoảng hoặc mốc tuổi thọ từ tổ chức giống chó hay nguồn thú y. [Bannasch và cộng sự](https://doi.org/10.1186/s40575-021-00111-4) · [Royal Kennel Club](https://www.royalkennelclub.com/breeds/)
- Blenheim Spaniel dùng ảnh biến thể Blenheim và số liệu chung của English Toy Spaniel. Chinese Rural Dog có ảnh minh họa một cá thể và ghi chú về tính đa dạng của quần thể. Eskimo Dog giữ nhãn mô hình cùng ghi chú về cách dùng tên. Hai hồ sơ sau để trống số đo và tuổi thọ vì thiếu dữ liệu phù hợp với phạm vi nhãn.
- Bộ lọc cỡ chó có thể dùng cân nặng trung vị hoặc điểm giữa khoảng cân nặng tham chiếu. Điểm giống tương tự được tính từ tỷ lệ giữa các số đo có thể so sánh. Đây là phép so sánh kích thước trong app.

## Nhận diện ảnh chụp và ảnh trong thư viện

App mặc định chạy hai mô hình ONNX trên Android. SSD-MobileNet tìm vùng chứa chó. Dog-O-Matic phân loại vùng ảnh đó theo 127 nhãn. Bảng `breed-catalog.json` ánh xạ 124 nhãn đủ rõ sang ID Firestore. Các dòng `null` là nhãn bị bỏ qua do chưa xác định được giống tương ứng.

Ảnh cần có vùng chó đạt điểm phát hiện ít nhất 0,45. Kết quả được chấp nhận khi điểm phân loại đứng đầu đạt 0,65 và hơn kết quả thứ hai ít nhất 0,15. Đây là ngưỡng ban đầu. Điểm hiển thị là điểm tin cậy cho ảnh hiện tại. Để đánh giá độ chính xác thực tế, cần đo trên bộ ảnh đại diện, gồm giống đã hỗ trợ, giống ngoài danh mục, chó lai và ảnh không có chó.

App yêu cầu đăng nhập để ghi lịch sử. Khi nhận diện trên thiết bị, Firestore nhận mã giống, điểm tin cậy và thời điểm. Bản JPEG dùng để xem lại nằm trong vùng lưu trữ riêng của app.

### Dùng API riêng

Thiết lập `DOG_RECOGNITION_API_URL=https://<api-cua-ban>/recognize` trong Gradle properties cục bộ hoặc truyền `-PDOG_RECOGNITION_API_URL=...` khi build. Bỏ trống giá trị này để dùng hai mô hình tích hợp.

App gửi `POST` với ảnh JPEG và Firebase ID token trong header `Authorization: Bearer …`. API kiểm tra token, xử lý ảnh và trả JSON có `dogId` trùng ID trong Firestore và `confidence` nguyên 0–100. API trả HTTP 422 khi không nhận diện được.

### Nguồn mô hình

- Bộ phân loại Dog-O-Matic INT8 của 10cdizzle, phiên bản `a21109ce534c543d53c4560b985e3dfb07788680`, giấy phép MIT. [Model card](https://huggingface.co/10cdizzle/dog-o-matic/blob/a21109ce534c543d53c4560b985e3dfb07788680/README.md)
- Bộ phát hiện SSD-MobileNetV1-12 INT8 từ ONNX Model Zoo, phiên bản `c5cae0f1942c8d7727372c7b1f6b485aa39e4421`, giấy phép Apache-2.0 của kho mô hình. [Model card](https://github.com/onnx/models/tree/c5cae0f1942c8d7727372c7b1f6b485aa39e4421/validated/vision/object_detection_segmentation/ssd-mobilenetv1)
- Ảnh Labrador trong bộ kiểm thử Android là ảnh của Karen Arnold, CC0. [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Labrador_dog.jpg)
