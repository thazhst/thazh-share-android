# P2P Chat WiFi Direct

Ứng dụng nhắn tin thời gian thực ngang hàng (peer-to-peer) qua WiFi Direct, không cần internet hay máy chủ trung gian. Viết bằng Kotlin + Jetpack Compose.

## Tính năng

- **Kết nối trực tiếp qua WiFi Direct** — tìm và kết nối với thiết bị Android khác gần đó, không cần mạng WiFi chung hay dữ liệu di động.
- **Đổi tên hiển thị** — đặt tên bạn muốn đối phương nhìn thấy.
- **Trạng thái kết nối theo thời gian thực** — 5 mức: Mạnh, Trung bình, Yếu, Kém, Mất kết nối — đo bằng ping/pong định kỳ qua socket đang hoạt động.
- **Truyền tệp** — ảnh, video, ghi âm giọng nói, và tệp bất kỳ, truyền theo từng đoạn (chunk) trực tiếp qua socket, có thanh tiến trình và nút thử lại khi gửi thất bại.
- **Lưu thiết bị đã trò chuyện** — tự động lưu lại thiết bị sau khi kết nối thành công để kết nối lại nhanh, có thể đổi tên gợi nhớ hoặc xóa khỏi danh sách.
- **Tab Truyền tệp (P2P qua WiFi Direct)** — thanh điều hướng dưới có hai tab: *Nhắn tin* và *Truyền tệp*. Tab Truyền tệp dùng cùng danh sách thiết bị và cùng kết nối với tab Nhắn tin, nhưng sau khi kết nối sẽ mở màn hình gửi/nhận tệp thay vì khung chat: chọn nhiều tệp (hoặc ảnh/video) để gửi, các tệp được xếp hàng và gửi lần lượt, hiển thị tiến trình, tốc độ, thời gian còn lại, hủy hoặc gửi lại từng tệp.
- **Quản lý tệp đã nhận** — màn hình riêng (mở từ biểu tượng thư mục trên tab Truyền tệp, hoặc bằng cách chạm vào thông báo "Đã nhận tệp mới") liệt kê mọi tệp nhận qua tab Truyền tệp: tìm kiếm, lọc theo loại (ảnh, video, âm thanh, tài liệu, tệp nén, ứng dụng), sắp xếp, chọn nhiều tệp (nhấn giữ), mở, chia sẻ, lưu vào thư mục *Tải xuống/DirectChat*, đổi tên, xem chi tiết và xóa.

## Kiến trúc

```
app/src/main/java/com/p2pchat/wifidirect/
├── data/           Mô hình dữ liệu + lưu trữ (DataStore, JSON lịch sử chat)
├── network/         Lớp WiFi Direct + giao thức socket + truyền tệp
├── service/         Foreground Service điều phối toàn bộ kết nối
├── ui/               ViewModel + điều hướng Compose
│   ├── components/   Các thành phần UI dùng lại được
│   ├── screens/      Màn hình: khám phá, chat, truyền tệp, tệp đã nhận, cài đặt, quyền
│   └── theme/        Màu sắc, kiểu chữ, Material 3 theme
└── util/             Định dạng, xử lý quyền
```

**Luồng kết nối:** `WifiDirectManager` bọc quanh `android.net.wifi.p2p.WifiP2pManager` của hệ thống để khám phá và thương lượng nhóm P2P. Khi nhóm hình thành, `WifiDirectService` mở một socket TCP ứng dụng (cổng `8988`) trên subnet mà WiFi Direct gán — chủ nhóm (`group owner`) lắng nghe trên tất cả giao diện cục bộ (`0.0.0.0`), phía còn lại tự dò địa chỉ chủ nhóm bằng cách đọc giao diện mạng `p2p*` của chính thiết bị (dự phòng về `192.168.49.1` nếu không dò được — đây là subnet mặc định phổ biến của WiFi Direct trên Android). Giao thức khung tin (`Frame` trong `Protocol.kt`) là JSON có tiền tố độ dài, gửi qua `SocketFrameCodec`; tệp lớn được chia thành các đoạn 64KB gửi dưới dạng byte thô (không bọc JSON) để tránh phình dung lượng do base64.

### Tab Truyền tệp & tệp đã nhận

Hai tab dùng **chung một kết nối WiFi Direct** (nhóm P2P và socket do `WifiDirectService` giữ), nên nhắn tin và truyền tệp chạy song song trên cùng một đường truyền. Điểm khác nhau là cách xử lý tệp nhận được:

- `Frame.FileOffer` có thêm trường `channel` (`CHAT` mặc định, hoặc `TRANSFER`). Tệp thuộc kênh `TRANSFER` được ghi vào thư mục riêng `files/received_files/` thay vì hiện thành bong bóng chat. Bản cũ không biết trường này vẫn tương thích: sẽ coi tệp là đính kèm chat.
- Dữ liệu đang nhận được ghi tạm vào `received_files/.incoming/` và chỉ được chuyển sang thư mục chính khi nhận đủ đoạn cuối, nên tệp dở dang không bao giờ xuất hiện như một tệp hoàn chỉnh. Khi kết nối bị ngắt giữa chừng, tệp dở bị xóa và các dòng đang chạy được đánh dấu thất bại (có thể gửi lại).
- Hủy hai chiều qua `Frame.FileAck(success = false, cancelled = true)`: bên gửi hoặc bên nhận bấm hủy đều khiến phía kia dừng ngay (không tiếp tục đẩy dữ liệu, hoặc xóa tệp dở).
- `ReceivedFilesRepository` giữ chỉ mục JSON (`received_files_index.json`) mô tả từng tệp (ai gửi, khi nào, đã xem chưa). Khi khởi động, chỉ mục được đối chiếu với thư mục: bỏ mục có tệp đã mất và nhận lại các tệp chưa có mục.
- Tên tệp do thiết bị kia gửi được làm sạch (`FileNameUtils`) trước khi tạo tệp, để một tên như `../../x` không thể ghi ra ngoài thư mục nhận.

## Yêu cầu

- Android Studio Koala trở lên (hoặc chỉ cần JDK 17 + Android SDK nếu build bằng dòng lệnh)
- Hai thiết bị Android thật chạy Android 8.0 (API 26) trở lên có hỗ trợ WiFi Direct — WiFi Direct **không hoạt động trên trình giả lập** vì cần phần cứng WiFi thật.

## Build

### Bằng GitHub Actions (khuyến nghị)

Mọi lần push hoặc tạo pull request vào nhánh `main`/`master` sẽ tự động kích hoạt workflow tại `.github/workflows/build-apk.yml`, build cả bản debug lẫn bản release đã ký, rồi tải lên dưới dạng artifact của lần chạy đó (vào **Actions → chọn lần chạy → Artifacts**). Cũng có thể kích hoạt thủ công qua tab **Actions → Build APK → Run workflow**.

Muốn đính kèm APK trực tiếp vào một GitHub Release, chỉ cần tạo tag bắt đầu bằng `v` rồi đẩy lên:

```bash
git tag v1.0.0
git push --tags
```

### Build cục bộ

```bash
./gradlew assembleDebug      # APK debug, cài trực tiếp lên thiết bị test
./gradlew assembleRelease    # APK release đã ký, dùng cấu hình ký sẵn trong repo
```

APK xuất ra tại `app/build/outputs/apk/{debug,release}/`.

> **Lưu ý về gradle-wrapper.jar:** repo này chứa `gradlew`/`gradlew.bat` chuẩn, nhưng do môi trường tạo ra dự án không có quyền truy cập mạng để tải file nhị phân `gradle-wrapper.jar` chính chủ từ máy chủ Gradle, workflow CI tự tái tạo file này từ một bản cài Gradle 8.7 đã xác thực trước mỗi lần build (bước "Regenerate Gradle wrapper..."). Nếu bạn build cục bộ lần đầu và gặp lỗi liên quan đến wrapper, hãy chạy (khi máy đã cài Gradle 8.7 trở lên):
> ```bash
> gradle wrapper --gradle-version 8.7 --distribution-type bin
> ```
> để tái tạo lại các file wrapper trước khi chạy `./gradlew`.

## Ký ứng dụng (signing)

Repo này cố tình đính kèm sẵn `release-debug.keystore` — một keystore tự ký, mật khẩu công khai (`wifidirectchat`/`wifidirectchat`) — để CI có thể build ra bản release **cài đặt được** mà không cần thiết lập secrets. Điều này phù hợp để build thử nghiệm, chia sẻ nội bộ, hoặc học tập.

**Không dùng keystore này để phát hành lên Play Store hoặc phân phối rộng rãi.** Trước khi phát hành thật, hãy tạo keystore riêng và mật khẩu bí mật:

```bash
keytool -genkeypair -v -keystore your-release.keystore -alias your-alias \
  -keyalg RSA -keysize 2048 -validity 10000
```

rồi cập nhật khối `signingConfigs` trong `app/build.gradle.kts` để đọc mật khẩu từ biến môi trường hoặc GitHub Secrets thay vì hard-code.

## Quyền cần thiết

| Quyền | Lý do |
|---|---|
| Vị trí (`ACCESS_FINE_LOCATION`) — Android ≤ 12 | Android yêu cầu bắt buộc để quét WiFi Direct, dù ứng dụng không dùng GPS |
| Thiết bị WiFi lân cận (`NEARBY_WIFI_DEVICES`) — Android 13+ | Thay thế quyền vị trí cho việc quét/kết nối WiFi Direct, khai báo `neverForLocation` vì ứng dụng không suy ra vị trí thực |
| Máy ảnh, micro | Chỉ dùng khi người dùng chủ động chụp ảnh/quay video/ghi âm để gửi |
| Thông báo | Hiển thị trạng thái kết nối đang chạy nền (foreground service) |

## Giới hạn đã biết

- WiFi Direct **không hoạt động trên trình giả lập (emulator)** — cần thiết bị thật.
- Chỉ hỗ trợ kết nối **1-1** (một cặp thiết bị mỗi lần), đúng theo giới hạn nhóm WiFi Direct của Android khi dùng ở chế độ đơn giản này.
- Lịch sử trò chuyện chỉ lưu **cục bộ trên từng thiết bị**, không đồng bộ qua tài khoản hay nhiều thiết bị.
- Tệp nhận qua tab Truyền tệp được **tự động nhận** (không hỏi xác nhận từng tệp) và nằm trong bộ nhớ riêng của ứng dụng; dùng *Lưu vào Tải xuống* hoặc *Chia sẻ* trong màn hình Tệp đã nhận để đưa ra ngoài. Tệp nhận qua khung chat vẫn nằm trong cuộc trò chuyện như trước và không hiển thị trong màn hình Tệp đã nhận.
- Lưu vào *Tải xuống* trên Android 8–9 cần cấp quyền lưu trữ (hỏi khi cần); từ Android 10 không cần quyền.
- Đây là dự án tham khảo/khởi điểm — trước khi dùng cho mục đích nghiêm túc, nên bổ sung kiểm thử tự động (unit test, instrumentation test) mà bản hiện tại chưa có.

## Giấy phép

Chưa chỉ định — thêm file `LICENSE` phù hợp với mục đích sử dụng của bạn trước khi công khai hoặc phân phối.
