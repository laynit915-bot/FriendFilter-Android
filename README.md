# FriendFilter Android v0.3

Bản Android local-first để hỗ trợ rà soát danh sách bạn Facebook trên điện thoại.

## Có trong v0.3
- Facebook chạy trong WebView hiển thị trực tiếp, đăng nhập một lần và giữ session bằng WebView cookie store.
- Chỉ cho điều hướng trong miền facebook.com.
- Ghi nhận hồ sơ đang hiển thị trên trang bạn bè và gộp dữ liệu local.
- Không gửi cookie/token tới server FriendFilter; không dùng locbanbe.com; không bypass Premium/key.
- Không auto-unfriend và không gắn nhãn "không tương tác" khi chưa có bằng chứng tương tác đủ tin cậy.
- GitHub Actions build APK bằng Gradle 8.9/JDK 17.

## Giới hạn quan trọng
Facebook có thể thay đổi DOM và mức dữ liệu hiển thị. v0.3 chưa tuyên bố đo Reaction/Comment/Last interaction chính xác toàn bộ tài khoản. Những dữ liệu đó không được suy đoán từ việc "không nhìn thấy". Đây là chủ đích để tránh xóa nhầm.
