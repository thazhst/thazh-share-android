# Thazh Share

<p align="center">
  <strong>Truyền tệp siêu tốc ngang hàng (P2P) & Chia sẻ đám mây đa giao thức với giao diện Glasmio Liquid Glass</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Package-com.thazh.share-0A84FF?style=for-the-badge" alt="Package Name" />
  <img src="https://img.shields.io/badge/Version-1.2.0%20(3)-30D158?style=for-the-badge" alt="Version" />
  <img src="https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-5AC8FA?style=for-the-badge" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target%20SDK-36-FF9F0A?style=for-the-badge" alt="Target SDK" />
  <img src="https://img.shields.io/badge/UI-Glasmio%20Liquid%20Glass-BF5AF2?style=for-the-badge" alt="Glasmio UI" />
  <img src="https://img.shields.io/badge/Store%20Ready-Uptodown%20%7C%20APK-34C759?style=for-the-badge" alt="Uptodown Ready" />
</p>

---

## 1. Tổng quan (Overview)

**Thazh Share** (`com.thazh.share`) là ứng dụng chia sẻ tệp thế hệ mới dành cho Android, kết hợp sức mạnh của **Wi-Fi Direct (P2P TCP Socket)**, **Bluetooth Low Energy (BLE Fast Beacon)**, **NFC Host Card Emulation (HCE Tap-to-Pair)** và **Chia sẻ qua liên kết đám mây** trong một trải nghiệm người dùng nguyên bản được xây dựng hoàn toàn trên hệ thống thiết kế **[Glasmio Liquid Glass](https://github.com/NEORUAA/Glasmio)** và bộ biểu tượng **SF Symbols**.

Ứng dụng không phụ thuộc vào các thành phần Material Design 3 truyền thống hay Material Icons, mang lại hiệu ứng kính mờ quang học thời gian thực (`Liquid Glass`), thanh tìm kiếm nổi (`GlasmioFloatingSearchBar`), thanh điều hướng nổi (`GlasmioFloatingNavBar`) và cử chỉ vuốt cạnh trái về trang trước mượt mà theo chuẩn iOS (`IosSwipeBackContainer`).

---

## 2. Tính năng nổi bật (Key Features)

### Truyền tệp đa giao thức (Multi-Protocol P2P Engine)
- **Wi-Fi Direct tốc độ cao**: Truyền tải ảnh gốc, video 4K, tài liệu và tệp dung lượng lớn trực tiếp giữa hai thiết bị Android thông qua luồng TCP Socket đa luồng mà không tiêu tốn dữ liệu di động (4G/5G).
- **Radar không gian tương tác (Borderless Spatial Radar)**: Giao diện Radar 2D không viền trên cả hai trang **Gửi** và **Nhận**, hiển thị khoảng cách ước tính (`1m`, `3m`, `10m`) từ cường độ tín hiệu RSSI và trạng thái kết nối trực quan.
- **BLE Fast Discovery**: Quảng bá và quét gói tin nhận diện thiết bị tức thì qua Bluetooth Low Energy (`0000FE9F-0000-1000-8000-00805F9B34FB`), cho phép phát hiện thiết bị lân cận trong tích tắc trước khi bắt tay qua Wi-Fi Direct.
- **NFC Tap-to-Pair (HCE)**: Chạm lưng hai điện thoại có bật NFC để trao đổi thông tin định danh APDU và tự động khởi tạo kết nối P2P không cần dò tìm thủ công.

### Kiểm soát quyền riêng tư & Nhận tệp thông minh
- **3 chế độ hiển thị linh hoạt**:
  1. **Ẩn thiết bị (`HIDDEN`)**: Hoàn toàn ẩn khỏi mạng quét xung quanh.
  2. **10 phút (`TEMPORARY_10_MIN`)**: Cho phép tìm thấy trong 10 phút với bộ đếm ngược trực tiếp và tự động ẩn khi hết giờ.
  3. **Luôn hiển thị (`ALWAYS_VISIBLE`)**: Luôn sẵn sàng nhận tệp không giới hạn thời gian.
- **Hộp thoại xác nhận kết nối nổi (`IncomingConnectionActivity`)**: Khi bạn đang sử dụng ứng dụng khác và vẫn bật chế độ hiển thị, Thazh Share hiển thị hộp thoại xác nhận kết nối trực quan với chuông báo tuỳ chọn và tự động từ chối sau 30 giây nếu không phản hồi.
- **Ô phím tắt Cài đặt nhanh (`VisibilityQuickTileService`)**: Bật/tắt chế độ hiển thị của Thazh Share ngay từ thanh Control Center / Quick Settings của Android.

### Chia sẻ qua liên kết & Đồng bộ đám mây
- **Tạo liên kết tải xuống tức thì**: Tải tệp lên đám mây và nhận ngay liên kết rút gọn kèm **mã QR trực quan** với tuỳ chọn thời hạn lưu trữ (`1 giờ`, `12 giờ`, `24 giờ`, `3 ngày`, hoặc `Vĩnh viễn`).
- **Đồng bộ đám mây qua Google Sign-In (Tuỳ chọn)**: Đăng nhập bằng tài khoản Google thông qua Jetpack `CredentialManager` để lưu trữ lịch sử liên kết đã tạo, chỉnh sửa ghi chú liên kết và đồng bộ tên hiển thị thiết bị xuyên suốt các thiết bị của bạn.

### Hệ thống thiết kế Glasmio Liquid Glass & SF Symbols
- **100% Glasmio UI & SF Symbols**: Sử dụng hoàn toàn các thành phần của thư viện [Glasmio](https://github.com/NEORUAA/Glasmio) (`GlasmioScaffold`, `GlasmioPinnedListPage`, `GlasmioCard`, `GlasmioGroupedList`, `GlasmioFloatingSearchBar`, `GlasmioFloatingNavBar`, `GlasmioBottomSheet`, `GlasmioAlertDialog`, `GlasmioSegmentedControl`) và bộ biểu tượng vector chuẩn **SF Symbols** (`SfSymbol`, `SfIcon`, `SfSymbolCatalog`).
- **Cử chỉ vuốt cạnh trái thông minh (`IosSwipeBackContainer`)**: Vuốt từ mép trái sang phải để quay lại **trang hoặc tab liền trước đó** trong lịch sử điều hướng (bao gồm cả từng trang con trong Cài đặt, các bước cấp quyền và lịch sử chuyển tab), kèm hiệu ứng thị sai (parallax) và bóng đổ thời gian thực.
- **Cài đặt phân trang khoa học**: Trang Cài đặt được chia thành 6 chuyên mục riêng biệt (*Tài khoản & thiết bị*, *Hiển thị & kết nối*, *Nhận tệp & bảo mật*, *Giao diện*, *Dữ liệu*, *Giới thiệu*) kèm thanh tìm kiếm nổi thông minh ở đáy màn hình.

---

## 3. Kiến trúc hệ thống (Architecture)

```text
Thazh Share
├── app/                                  # Ứng dụng chính (com.thazh.share)
│   ├── data/                             # Lưu trữ cục bộ, Preferences, Firestore Repository & Models
│   ├── network/                          # Giao thức truyền tệp TCP Socket & Cloud Link Uploader
│   ├── service/                          # WifiDirectService, BleDiscoveryManager, NfcHceService, QuickTile
│   ├── ui/
│   │   ├── auth/                         # Xác thực Google qua Jetpack CredentialManager
│   │   ├── components/                   # GlasmioPrimitives, InteractiveRadarCard, VisibilitySelector, v.v.
│   │   ├── screens/                      # Discovery, Receive, LinkShare, ReceivedFiles, Settings (6 trang con), About
│   │   └── theme/                        # ThazhTheme (Sáng/Tối/Hệ thống) & ThazhIcons (SF Symbols)
│   └── util/                             # Tiện ích định dạng, chia sẻ tệp và tạo dữ liệu
├── glasmio-core/                         # Hệ thống màu sắc, Typography, Theme & IosSwipeBackContainer
├── glasmio-glass/                        # Hiệu ứng khúc xạ kính mờ Liquid Glass & Backdrop
├── glasmio-icons/                        # Bộ giải mã và hiển thị SF Symbols (SfSymbol, SfIcon)
└── glasmio-ui/                           # Hệ thống thành phần giao diện Glasmio (Cards, Sheets, NavBars, SearchBars)
```

---

## 4. Hướng dẫn cài đặt & Build (Getting Started)

### Yêu cầu môi trường
- **Android Studio**: Ladybug (2024.2.1) trở lên hoặc JDK 17.
- **Android SDK**: `minSdk = 26` (Android 8.0), `targetSdk = 36`, `compileSdk = 36`.
- **Gradle**: Sử dụng Gradle 8.x (Kotlin DSL).

### Các lệnh Build cơ bản

```bash
# Kiểm tra và biên dịch bản Debug APK
gradle assembleDebug

# Biên dịch bản Release APK (đã bật R8/ProGuard thu gọn tài nguyên, sẵn sàng phát hành lên Uptodown)
gradle assembleRelease

# Chạy kiểm thử đơn vị (Unit & Robolectric Tests)
gradle :app:testDebugUnitTest
```

Sau khi chạy `gradle assembleRelease`, tệp APK phát hành sẽ nằm tại:
`app/build/outputs/apk/release/app-release.apk`

---

## 5. Thiết lập Keystore cố định SHA-1 / SHA-256 qua `RELEASE_KEYSTORE_BASE64`

Kho mã nguồn không lưu trữ bất kỳ tệp keystore nào để đảm bảo an toàn bảo mật. Để mỗi lần build trên GitHub Actions **luôn giữ nguyên chứng chỉ ký số SHA-1 và SHA-256** (giúp Google Sign-In, Firebase Authentication và cập nhật ứng dụng trên **Uptodown** hoạt động liền mạch):

### Bước 1: Tạo tệp Keystore cố định (Chỉ thực hiện 1 lần duy nhất)
Chạy lệnh sau trên máy tính của bạn để tạo tệp `release.keystore`:

```bash
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias wifidirectchat \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass wifidirectchat \
  -keypass wifidirectchat \
  -dname "CN=Thazh Share, OU=Mobile, O=Thazh, L=Hanoi, S=HN, C=VN"
```

### Bước 2: Lấy mã băm SHA-1 và SHA-256 cố định
```bash
keytool -list -v \
  -keystore release.keystore \
  -alias wifidirectchat \
  -storepass wifidirectchat
```
Sao chép hai giá trị **SHA1** và **SHA256** trong phần `Certificate fingerprints` và thêm vào cấu hình ứng dụng Android (`com.thazh.share`) trong **Firebase Console > Project Settings > Your apps**.

### Bước 3: Mã hoá Keystore sang Base64 và lưu vào GitHub Secret
```bash
base64 -w 0 release.keystore > release.keystore.base64
```
- Mở kho lưu trữ trên GitHub: **Settings > Secrets and variables > Actions > New repository secret**.
- Tạo biến bí mật **`RELEASE_KEYSTORE_BASE64`** và dán toàn bộ nội dung của `release.keystore.base64`. Workflow `.github/workflows/build.yml` sẽ tự động giải mã `RELEASE_KEYSTORE_BASE64` để ký số cho các bản dựng APK.

---

## 6. Tự động khôi phục `google-services.json` từ GitHub Secret

> 📄 **Hướng dẫn chi tiết từng bước bằng tiếng Việt**: Xem tệp [`HUONG_DAN_GITHUB_SECRETS.txt`](./HUONG_DAN_GITHUB_SECRETS.txt) tại thư mục gốc của dự án để thiết lập nhanh các biến `GOOGLE_SERVICES_JSON` và `RELEASE_KEYSTORE_BASE64` trên GitHub.

Vì lý do bảo mật, bạn có thể không muốn đưa tệp `app/google-services.json` công khai lên kho mã nguồn, hoặc muốn tự động tạo lại tệp này khi chạy CI/CD trên GitHub Actions:

1. Mở kho lưu trữ trên GitHub: **Settings > Secrets and variables > Actions > New repository secret**.
2. Tạo Secret tên là **`GOOGLE_SERVICES_JSON`**:
   - Bạn có thể dán **toàn bộ nội dung JSON gốc** của tệp `google-services.json`, **hoặc** chuỗi đã mã hoá **Base64** (`base64 -w 0 app/google-services.json`).
3. Quy trình `.github/workflows/android-release.yml` tích hợp sẵn trong dự án sẽ tự động kiểm tra định dạng (JSON thuần hoặc Base64), khôi phục chính xác tệp `app/google-services.json` và xác thực cấu trúc JSON trước khi thực hiện lệnh `gradle assembleRelease`.

Bạn cũng có thể khôi phục nhanh trên môi trường terminal bằng lệnh:
```bash
# Nếu GOOGLE_SERVICES_JSON chứa nội dung JSON thuần:
printf '%s\n' "$GOOGLE_SERVICES_JSON" > app/google-services.json

# Hoặc nếu GOOGLE_SERVICES_JSON được mã hoá Base64:
printf '%s' "$GOOGLE_SERVICES_JSON" | base64 --decode > app/google-services.json
```

---

## 7. Danh sách kiểm tra phát hành lên Store (Uptodown Readiness)

Thazh Share đã được tối ưu hoá toàn diện để sẵn sàng phân phối trên **Uptodown** và các kho ứng dụng Android quốc tế:

- **Định danh gói chuẩn (`applicationId`)**: `com.thazh.share`
- **Phiên bản phát hành**: `versionName = "1.2.0"`, `versionCode = 3`
- **Biểu tượng thích ứng (Adaptive Launcher Icon)**: Hỗ trợ đầy đủ `mipmap-anydpi-v26` với lớp nền chuyển sắc và biểu tượng sắc nét trên mọi trình khởi chạy Android.
- **Tối ưu hoá R8 / ProGuard (`proguard-rules.pro`)**: Bật `isMinifyEnabled = true` và `isShrinkResources = true` cho bản Release kèm tập quy tắc bảo toàn đầy đủ cho Firebase Firestore, Credential Manager, Kotlinx Serialization và các dịch vụ phần cứng (Wi-Fi Direct, BLE, NFC HCE).
- **Tuân thủ quyền truy cập (Least-Privilege Permissions)**:
  - Màn hình chuẩn bị cấp quyền minh bạch giải thích rõ lý do cho từng quyền (*Thiết bị ở gần*, *Vị trí cho Wi-Fi Direct*, *Thông báo*, *Hiển thị hộp thoại nhận tệp*).
  - Sử dụng **Android Photo Picker** (`PickMultipleVisualMedia`) không yêu cầu quyền truy cập bộ nhớ rộng khi chọn ảnh/video.

---

## 8. Ghi nhận & Giấy phép nguồn mở (Credits & License)

- **[Glasmio UI Library](https://github.com/NEORUAA/Glasmio)**: Hệ thống thiết kế Liquid Glass, SF Symbols và các thành phần giao diện hiện đại cho Jetpack Compose bởi **NEORUAA**, phát hành theo giấy phép **Apache License 2.0**.
- **Hạ tầng lưu trữ đám mây**: Hỗ trợ tạo liên kết chia sẻ tệp nhanh chóng với tuỳ chọn thời hạn linh hoạt.

 Toàn bộ văn bản giấy phép **Apache License 2.0** của Glasmio được tích hợp trực tiếp trong ứng dụng tại mục **Cài đặt > Giới thiệu > Giấy phép nguồn mở**.
