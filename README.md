# Thazh Share (`com.thazh.share`)

Ứng dụng chia sẻ tệp ngang hàng (Peer-to-Peer) tốc độ cao qua **Wi-Fi Direct**, **Bluetooth BLE**, **NFC HCE** và **Chia sẻ qua liên kết đám mây (Catbox.moe)** dành cho Android, được thiết kế với hệ thống giao diện thủy tinh lỏng **Glasmio (Liquid Glass)** hỗ trợ đầy đủ chế độ Sáng / Tối.

## Tính năng nổi bật

- **Radar không gian 2D không viền (Send & Receive)** — Quét trực quan các thiết bị xung quanh theo khoảng cách thời gian thực, chạm trực tiếp vào nút thiết bị trên Radar để kết nối và gửi tệp ngay.
- **3 chế độ hiển thị thiết bị & Phím tắt Control Center (Quick Settings Tile)**:
  - **Ẩn (`HIDDEN`)**, **Tìm thấy trong 10 phút (`TEMPORARY_10_MIN`)** (kèm đồng hồ đếm ngược tự động ẩn), và **Luôn cho phép tìm thấy (`ALWAYS_VISIBLE`)**.
  - Hỗ trợ thêm phím tắt **Thazh Share** trực tiếp vào **Control Center (Quick Settings)** của Android chỉ với 1 chạm ngay trong ứng dụng để chuyển đổi nhanh trạng thái hiển thị mọi lúc.
- **Hộp thoại xác nhận kết nối khi đang dùng ứng dụng khác** — Khi vẫn bật trạng thái hiển thị của thiết bị trong Thazh Share và đang mở ứng dụng khác, nếu có người gửi tệp tới, hệ thống hiển thị hộp thoại xác nhận nổi (`IncomingConnectionActivity`) cho phép **Chấp nhận**, **Nhận & mở ứng dụng** hoặc **Từ chối**.
- **Chạm để kết nối qua NFC HCE & Phát hiện nhanh qua BLE** — Đặt mặt lưng hai máy chạm nhau để tự động trao đổi định danh và bắt tay Wi-Fi Direct tức thì; BLE giúp phát hiện thiết bị gần đó và đo khoảng cách ước tính theo mét.
- **Chia sẻ qua liên kết (Catbox.moe + Litterbox)** — Tải tệp lên đám mây tạo liên kết trực tiếp kèm mã QR (hỗ trợ lưu vĩnh viễn hoặc tự hủy sau 1h / 12h / 24h / 72h). Không bắt buộc đăng nhập; nếu đăng nhập bằng Google (`CredentialManager`), lịch sử liên kết sẽ tự động đồng bộ qua **Cloud Firestore**.
- **Màn hình Chào mừng & Chuẩn bị cấp quyền theo từng bước** — Mở đầu bằng trang **Chào mừng** cùng nút **Bắt đầu**, tiếp nối bởi 3 trang **Chuẩn bị** riêng biệt cho từng nhóm quyền (Thiết bị Wi-Fi lân cận, Bluetooth BLE, Thông báo) kèm giải thích ngắn gọn tại sao cần quyền đó.
- **Vuốt cạnh trái để quay lại kiểu iOS (`IosSwipeBackContainer`)** — Vuốt từ mép trái qua phải để đóng các trang phụ mượt mà.
- **Quản lý tệp đã nhận** — Tìm kiếm, lọc theo danh mục (Ảnh, Video, Âm thanh, Tài liệu, Tệp nén, APK), sắp xếp, lưu vào `Tải xuống/Thazh Share`, đổi tên hoặc gửi tiếp qua Wi-Fi Direct.

## Kiến trúc thư mục

```text
app/src/main/java/com/thazh/share/
├── auth/            Đăng nhập Google qua Jetpack Credential Manager + Firebase Auth
├── data/            Mô hình dữ liệu, DataStore Preferences, ReceivedFilesRepository, SharedLinkRepository (Firestore)
├── network/         WifiDirectManager, BleDiscoveryManager, NfcConnectionManager, SocketFrameCodec, CatboxUploader
├── service/         WifiDirectService (Foreground Service), ShareNfcHceService (NFC HCE), ShareQuickSettingsTileService (Control Center Tile)
├── ui/              ShareViewModel + điều hướng Glasmio Compose
│   ├── components/  InteractiveRadarCard (không viền), VisibilitySelector, NfcTapCard, IosSwipeBackContainer, ...
│   ├── screens/     PermissionScreen (Chào mừng + Chuẩn bị từng bước), DiscoveryScreen, ReceiveScreen, LinkShareScreen, ReceivedFilesScreen, SettingsScreen
│   └── theme/       ThazhShareTheme (Light / Dark / System), ThazhIcons (SF Symbols)
└── util/            PermissionUtils, FileActions, FormatUtils, BackgroundOptimizationHelper
```

---

## Hướng dẫn Setup Keystore để Build Release cố định SHA-1 & SHA-256

### 1. Tại sao SHA-1 và SHA-256 bị đổi nếu không cố định Keystore?
Trên Android, mã băm **SHA-1** và **SHA-256** của bản APK/AAB phụ thuộc hoàn toàn vào tệp **Keystore (`.keystore` / `.jks`)** dùng để ký ứng dụng:
- Nếu mỗi lần build (trên máy mới, container mới hoặc GitHub Actions) bạn chạy lại lệnh `keytool -genkeypair`, một cặp khóa RSA mới sẽ được sinh ra -> **SHA-1 và SHA-256 sẽ bị thay đổi hoàn toàn**.
- Khi SHA-1 / SHA-256 bị đổi:
  1. Thiết bị đã cài bản cũ sẽ báo lỗi `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (không thể cài đè bản cập nhật).
  2. **Google Sign-In (`CredentialManager`)** và **Firebase Authentication / Firestore** sẽ từ chối xác thực vì chứng chỉ APK không khớp với SHA-1 / SHA-256 đã đăng ký trong `google-services.json`.

Để **mỗi lần build release đều giữ nguyên đúng 1 mã SHA-1 và SHA-256 duy nhất**, bạn chỉ cần **tạo tệp keystore đúng 1 lần duy nhất** và tái sử dụng đúng tệp đó cho mọi lần build cục bộ lẫn CI/CD.

---

### 2. Các bước tạo và cố định Keystore cho dự án `com.thazh.share`

#### Bước 1: Tạo tệp Keystore cố định (Chỉ chạy 1 lần duy nhất)
Project hiện được cấu hình trong `app/build.gradle.kts` (`signingConfigs.release`) đọc tệp `release-debug.keystore` ở thư mục gốc của project.
- **Cách A (Giữ nguyên tệp `release-debug.keystore` có sẵn trong repo):**
  Nếu tệp `release-debug.keystore` đã được commit cố định trong repository và bạn **không chạy lệnh tạo lại**, thì mọi lần build cục bộ hoặc trên GitHub Actions đều sẽ luôn có **cùng một SHA-1 và SHA-256**.
- **Cách B (Tạo Keystore riêng của bạn và ghi đè một lần duy nhất vào `release-debug.keystore`):**
  Chạy lệnh sau tại thư mục gốc của project (chỉ chạy **1 lần duy nhất**):
  ```bash
  rm -f release-debug.keystore
  keytool -genkeypair -v \
    -keystore release-debug.keystore \
    -alias wifidirectchat \
    -storepass wifidirectchat \
    -keypass wifidirectchat \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Thazh Share,O=Thazh,C=VN"
  ```
  *(Lưu ý: Giữ nguyên `-alias wifidirectchat`, `-storepass wifidirectchat`, `-keypass wifidirectchat` để tương thích trực tiếp với cấu hình `signingConfigs.release` hiện tại trong `app/build.gradle.kts` mà không cần sửa file Gradle).*

#### Bước 2: Lấy mã SHA-1 và SHA-256 cố định của Keystore
Chạy lệnh sau để xem mã `SHA-1` và `SHA-256` của tệp `release-debug.keystore`:
```bash
keytool -list -v \
  -keystore release-debug.keystore \
  -alias wifidirectchat \
  -storepass wifidirectchat
```
Kết quả sẽ hiển thị trong mục **Certificate fingerprints**:
```text
Certificate fingerprints:
     SHA1:   XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
     SHA256: YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY:YY
```
Ngoài ra, bạn cũng nên lấy thêm `SHA-1` và `SHA-256` của `debug.keystore` nếu muốn test bản Debug:
```bash
keytool -list -v \
  -keystore debug.keystore \
  -alias androiddebugkey \
  -storepass android
```

#### Bước 3: Thêm SHA-1 và SHA-256 vào Firebase Console
Để tính năng đăng nhập Google (`CredentialManager`) hoạt động trên bản Release:
1. Mở [Firebase Console](https://console.firebase.google.com/) -> chọn dự án của bạn.
2. Vào **Project Settings (Cài đặt dự án)** -> tab **General (Chung)** -> cuộn xuống phần **Your apps** -> chọn ứng dụng Android **`com.thazh.share`**.
3. Nhấn **Add fingerprint (Thêm dấu vân tay)** và dán lần lượt cả mã **SHA-1** và **SHA-256** vừa lấy ở Bước 2 vào.
4. Tải lại tệp **`google-services.json`** mới nhất và đặt đè vào `app/google-services.json`.

#### Bước 4: Đảm bảo CI/CD (GitHub Actions) luôn dùng đúng Keystore này
Có 2 cách để GitHub Actions luôn ký APK bằng đúng Keystore cố định của bạn:
1. **Commit trực tiếp `release-debug.keystore` vào Git (Mặc định):**
   Chỉ cần commit tệp `release-debug.keystore` vào nhánh `main` và không bao giờ xóa/tạo lại tệp này.
2. **Lưu trong GitHub Secrets (`RELEASE_KEYSTORE_BASE64`) — Bảo mật cao hơn:**
   - Mã hóa tệp keystore sang chuỗi Base64:
     ```bash
     base64 -w 0 release-debug.keystore
     # (Trên macOS dùng: base64 -i release-debug.keystore | tr -d '\n')
     ```
   - Vào kho GitHub của bạn -> **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**:
     - Name: `RELEASE_KEYSTORE_BASE64`
     - Secret: *(Dán chuỗi Base64 vừa copy)*
   - Workflow `.github/workflows/build.yml` đã được tích hợp sẵn bước tự động giải mã `RELEASE_KEYSTORE_BASE64` ra `release-debug.keystore` trước khi chạy `./gradlew assembleRelease`. Nhờ đó mọi bản build từ GitHub Actions sẽ luôn giữ cố định 100% mã **SHA-1** và **SHA-256**.

#### Bước 5: Tự động tạo lại tệp `app/google-services.json` từ GitHub Secret
Workflow `.github/workflows/build.yml` hỗ trợ tự động khôi phục tệp `app/google-services.json` từ **GitHub Secrets** trong mỗi lần chạy CI/CD (hỗ trợ cả định dạng JSON thuần và chuỗi Base64):
1. Vào kho GitHub của bạn -> **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**.
2. Tạo một trong hai Secret sau:
   - **Cách 1 (Dán trực tiếp nội dung JSON):**
     - Name: `GOOGLE_SERVICES_JSON`
     - Secret: *(Dán toàn bộ nội dung tệp `app/google-services.json` hoặc chuỗi Base64 của tệp)*
   - **Cách 2 (Mã hóa Base64):**
     - Chạy lệnh: `base64 -w 0 app/google-services.json` (hoặc trên macOS: `base64 -i app/google-services.json | tr -d '\n'`)
     - Name: `GOOGLE_SERVICES_JSON_BASE64`
     - Secret: *(Dán chuỗi Base64 vừa copy)*
3. Khi GitHub Actions chạy, bước `Restore google-services.json from GitHub Secret` sẽ tự động tạo lại `app/google-services.json` trước khi biên dịch APK Debug và Release.

---

## Lệnh Build

```bash
./gradlew assembleDebug      # Build APK Debug
./gradlew assembleRelease    # Build APK Release (đã ký bằng release-debug.keystore)
```

APK sau khi build nằm tại:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`
