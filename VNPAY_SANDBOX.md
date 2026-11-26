# DogLens Premium trên VNPAY Sandbox

Android lấy project ID từ cấu hình Firebase hiện tại. API phải được triển khai vào cùng dự án, tại khu vực `asia-southeast1`. Các mức kiểm thử là 49.000 ₫/30 ngày và 399.000 ₫/365 ngày, thanh toán một lần.

## Cấu hình Firebase

1. Dùng Node.js 22, Firebase CLI và tài khoản có quyền triển khai dự án. Cài dependencies bằng `npm ci --prefix functions`.
2. Điền thông tin sandbox qua lời nhắc của Secret Manager. Nhập terminal ID được cấp và khóa ký sandbox mới nếu khóa cũ đã bị lộ.

   ```powershell
   firebase functions:secrets:set VNPAY_TMN_CODE --project <project-id>
   firebase functions:secrets:set VNPAY_HASH_SECRET --project <project-id>
   ```

3. Trước khi triển khai, đối chiếu `firestore.rules` với rules hiện tại. Bảo vệ trường `users/{uid}.premiumUntil` khỏi mọi ghi trực tiếp từ app. Chặn toàn bộ quyền truy cập từ client đối với `premiumOrders` và `premiumCheckoutSessions`; Cloud Functions dùng Admin SDK để truy cập.
4. Triển khai rules đã rà soát và hàm thanh toán.

   ```powershell
   firebase deploy --only firestore:rules,functions:premiumPayments --project <project-id>
   ```

5. Đăng ký URL IPN với VNPAY Sandbox.

   ```text
   https://asia-southeast1-<project-id>.cloudfunctions.net/premiumPayments/ipn
   ```

   URL quay lại dành cho trình duyệt là cùng địa chỉ hàm với đường dẫn `/return`.

## Kiểm thử

Chạy `npm test --prefix functions` để kiểm tra mã ký. Trong bản debug, đăng nhập, mở **Cá nhân → Xem gói Premium**, chọn gói và dùng thẻ test của VNPAY Sandbox. Quay lại app và mở **Kiểm tra giao dịch**. Trang quay lại chỉ hướng dẫn về app; trạng thái giao dịch và thời hạn Premium được xác định từ IPN đã xác minh. Thử thêm hủy giao dịch, hết hạn và gửi lại IPN cùng mã đơn để kiểm tra thời hạn chỉ được cộng một lần.
