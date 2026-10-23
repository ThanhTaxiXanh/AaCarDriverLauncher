# CarDriverLauncher

Ứng dụng Android Auto (không thay Launcher mặc định của xe) gồm 2 phần trong **một APK**:

- **App điện thoại** (`mobile/`): cấu hình chế độ chia màn hình, tỷ lệ Bản đồ/Widget, lưới widget.
- **App Android Auto** (`car/`): `CarAppService` vẽ Bản đồ Mapbox 3D + lưới widget theo cấu hình.

Widget: Lịch Âm Dương, Hình ảnh (slide), Media, Telemetry (tốc độ, giới hạn, Distance, AVG, GPS).

## Thiết lập GitHub Secrets (Settings → Secrets and variables → Actions)

| Secret | Nội dung |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | keystore mã hoá Base64 |
| `KEYSTORE_PASSWORD` | mật khẩu keystore |
| `KEY_ALIAS` | alias của key |
| `KEY_PASSWORD` | mật khẩu key |
| `MAPBOX_DOWNLOADS_TOKEN` | Mapbox **secret** token, scope `DOWNLOADS:READ` (để tải SDK) |
| `MAPBOX_ACCESS_TOKEN` | Mapbox **public** token `pk.…` (để hiển thị bản đồ) |

Chưa có keystore? Chạy workflow **Generate keystore** (Actions → Run workflow), tải artifact
`keystore-secrets`, dán 4 giá trị đầu vào Secrets, rồi xoá artifact. **Giữ keystore này mãi mãi**:
mất keystore = không thể cài đè bản cập nhật.

## Build
Push lên `main` hoặc chạy tay workflow **Build signed release APK**; APK nằm trong Artifacts.
`versionCode` = số lần chạy workflow nên luôn tăng dần, cài đè được bản cũ.

## Dùng với Android Auto
Cài APK thủ công cần bật: Android Auto → Cài đặt → bấm 10 lần vào "Phiên bản" → Menu ⋮ →
Cài đặt cho nhà phát triển → **Nguồn không xác định**. Ứng dụng khai báo category NAVIGATION nên
xuất hiện trong danh sách ứng dụng của Android Auto.
