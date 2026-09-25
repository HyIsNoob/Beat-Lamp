# Tài Liệu Bàn Giao Dự Án Mod Beat Lamp (Handover Documentation)

Tài liệu này tổng hợp toàn bộ trạng thái dự án, cấu trúc mã nguồn, tính năng các khối/vật phẩm, chi tiết các sửa đổi vừa thực hiện và hướng dẫn vận hành để bạn có thể tiếp tục làm việc trong các phiên mới mà không bị gián đoạn.

---

## 1. Tổng Quan Dự Án

* **Tên mod:** Beat Lamp
* **Mod ID:** `beatlamp`
* **Phiên bản hiện tại:** `1.2.1`
* **Mục tiêu:** Cung cấp hệ thống thiết bị âm thanh, ánh sáng, hiệu ứng sân khấu chuyên nghiệp trong Minecraft, tự động phân tích và bắt nhịp theo âm nhạc trong game hoặc nhạc ngoài với khả năng đồng bộ nhóm đèn diện rộng.
* **Đường dẫn workspace:** [d:/fileluu/Tools/modMinecraft](file:///d:/fileluu/Tools/modMinecraft)
* **Quy tắc quan trọng của dự án:** Không sử dụng emoji trong toàn bộ giao tiếp và tài liệu. Giải thích kỹ thuật bằng tiếng Việt gần gũi, rõ ràng.

---

## 2. Các Phiên Bản Minecraft Được Hỗ Trợ & Cấu Trúc Dự Án

Dự án áp dụng mô hình multi-project Gradle hỗ trợ nhiều phiên bản và mod loader song song:

1. **Minecraft 1.21.7 (Fabric & NeoForge):**
   * Nằm tại thư mục [fabric-1.21.7/](file:///d:/fileluu/Tools/modMinecraft/fabric-1.21.7)
   * Mã Fabric: [fabric-1.21.7/src/main/](file:///d:/fileluu/Tools/modMinecraft/fabric-1.21.7/src/main)
   * Mã NeoForge: [fabric-1.21.7/src/neoforge/](file:///d:/fileluu/Tools/modMinecraft/fabric-1.21.7/src/neoforge)
   * Tệp xuất xưởng: `beatlamp-fabric-1.21.7-1.2.0.jar` và `beatlamp-neoforge-1.21.7-1.2.0.jar`
2. **Minecraft 1.21.4 (Fabric & NeoForge):**
   * Nằm tại thư mục [fabric-1.21.4/](file:///d:/fileluu/Tools/modMinecraft/fabric-1.21.4)
   * Tệp xuất xưởng: `beatlamp-fabric-1.21.4-1.2.0.jar` và `beatlamp-neoforge-1.21.4-1.2.0.jar`
3. **Minecraft 1.21.1 (Fabric, NeoForge, Forge):**
   * Nằm tại thư mục gốc [src/](file:///d:/fileluu/Tools/modMinecraft/src), [neoforge/](file:///d:/fileluu/Tools/modMinecraft/neoforge), [forge/](file:///d:/fileluu/Tools/modMinecraft/forge)
4. **Minecraft 1.20.1 (Fabric & Forge):**
   * Fabric: [fabric-1.20.1/](file:///d:/fileluu/Tools/modMinecraft/fabric-1.20.1)
   * Forge: [forge-1.20.1/](file:///d:/fileluu/Tools/modMinecraft/forge-1.20.1)
   * Tệp xuất xưởng: `beatlamp-fabric-1.20.1-1.2.0.jar` và `beatlamp-forge-1.20.1-1.2.0.jar`

---

## 3. Hệ Thống Khối, Vật Phẩm & Tính Năng Trong Mod

Tất cả các thiết bị đều có thể cấu hình bằng cách cầm gậy điều khiển (**Lamp Controller**) bấm chuột phải vào khối:

1. **Beat Lamp (`beat_lamp`):**
   * Khối đèn chính cảm ứng theo nhịp điệu bài hát (Spectrum, VU Meter, Oscilloscope, Matrix Rain, Ripple, Wave, Scan, RGB...).
   * Hỗ trợ chế độ có khung (Framed) và không khung (Frameless) tạo màn hình LED vô cực khi ghép nhiều khối.
2. **Rainbow LED Block (`rainbow_led_block`):**
   * Khối đèn LED trang trí đa năng (không phụ thuộc vào bài hát).
   * 11 chế độ hiệu ứng: Static, Rainbow Flow, Pulse, Strobe, Chase, Fire, Wave, Matrix, Breathe, Twinkle, Police.
   * Chế độ **Framed (có khung)** giữ nguyên viền kim loại 2 pixel bao quanh 4 cạnh, ánh sáng LED nằm trong ô 12x12.
   * Chế độ **Frameless (không khung)** làm biến mất khung viền, ánh sáng phát quang tràn viền toàn khối (16x16).
3. **Stage Light (`stage_light`):** Đèn moving head rọi chùm tia xoay theo nhịp hoặc quét tự do, chỉnh được góc quét, màu sắc, tốc độ, độ nhạy.
4. **Laser Projector (`laser_projector`):** Máy chiếu chùm tia laser sân khấu với nhiều dạng chùm tia và màu sắc.
5. **Fountain (`fountain`):** Máy bắn tia nước, pháo hoa sân khấu, tia lửa điện (Sparkler) theo nhịp nhạc.
6. **Fog Generator (`fog_generator`):** Máy tạo khói sân khấu mô phỏng khói mù hoặc khói thấp (Low Fog).
7. **Stage Jukebox (`stage_jukebox`):** Máy phát nhạc sân khấu đồng bộ cho toàn bộ hệ thống đèn trong khu vực.
8. **DMX Console (`dmx_console`):** Bàn điều khiển tổng điều khiển tập trung: Master Dimmer, Master Speed, Blackout tức thì, Strobe All.
9. **DJ Deck (`dj_deck`):** Bàn DJ tương tác âm thanh.
10. **Stage Speaker (`stage_speaker`):** Loa sân khấu tăng cường trường âm thanh và bán kính bắt nhịp.
11. **Công Cụ Hỗ Trợ:**
    * **Controller (`controller`):** Mở giao diện cấu hình trực quan cho từng khối hoặc đồng bộ cụm khối liền kề.
    * **Linker (`linker`):** Chuột phải để liên kết thủ công các khối nằm cách xa nhau vào chung một nhóm điều khiển.
    * **Phím tắt "O":** Mở màn hình Client Settings tổng (bật tắt chống động kinh Anti-strobe, giới hạn khoảng cách vẽ, kích hoạt hiệu ứng sân khấu).

---

## 4. Chi Tiết Các Vấn Đề Đã Được Khắc Phục Trong Phiên Này

### Vấn đề 1: Văng game khi bấm phím "O" trên NeoForge 1.21.7
* **Triệu chứng:** Game bị crash với lỗi `java.lang.IllegalStateException: Can only blur once per frame`.
* **Nguyên nhân:** Từ phiên bản Minecraft 1.21.5, phương thức `super.render` của `Screen` đã tự động xử lý làm mờ màn hình game phía sau. Trong mã nguồn cũ, hàm `render` của [BeatLampClientSettingsScreen.java](file:///d:/fileluu/Tools/modMinecraft/fabric-1.21.7/src/main/java/com/beatlamp/client/gui/BeatLampClientSettingsScreen.java#L179-L184) lại gọi thêm `this.renderBackground(...)` trước `super.render(...)`, dẫn đến yêu cầu bộ làm mờ hoạt động 2 lần trong 1 khung hình.
* **Giải pháp:** Loại bỏ dòng gọi thừa `this.renderBackground(...)`.

### Vấn đề 2: Thiếu khối Rainbow LED trên NeoForge 1.21.7
* **Triệu chứng:** Khối Rainbow LED xuất hiện bình thường ở Fabric 1.21.7 nhưng biến mất hoàn toàn trên bản NeoForge 1.21.7, không có trong tab sáng tạo.
* **Nguyên nhân:** Khối, thực thể khối, vật phẩm khối, gói tin mạng cấu hình và bộ vẽ của Rainbow LED chưa được đăng ký trong hệ sinh thái NeoForge 1.21.7.
* **Giải pháp:**
  * Bổ sung `RAINBOW_LED_BLOCK`, `RAINBOW_LED_BE`, `RAINBOW_LED_ITEM`, thêm vào tab sáng tạo `TAB` và đăng ký bộ giải mã/xử lý `RainbowLedConfigurePayload` trong [BeatLampNeoForge.java](file:///d:/fileluu/Tools/modMinecraft/fabric-1.21.7/src/neoforge/java/com/beatlamp/neoforge/BeatLampNeoForge.java#L76-L140).
  * Đăng ký renderer `RainbowLedRenderer` và hành động mở màn hình `RainbowLedBlockEntity.controllerUser` trong [BeatLampNeoForgeClient.java](file:///d:/fileluu/Tools/modMinecraft/fabric-1.21.7/src/neoforge/java/com/beatlamp/neoforge/BeatLampNeoForgeClient.java#L66-L150).

### Vấn đề 3: Tùy chọn Framed và Frameless của Rainbow LED không khác nhau
* **Triệu chứng:** Khi chuyển đổi giữa Framed và Frameless bằng gậy điều khiển trên Fabric 1.21.7, hình ảnh hiển thị của khối đèn không có sự khác biệt.
* **Nguyên nhân:**
  * Mô hình của khối LED khi có khung (`frameless=false`) chứa viền kim loại 2 pixel bao quanh 4 mặt và ô kính trong suốt ở giữa.
  * Bộ vẽ [RainbowLedRenderer.java](file:///d:/fileluu/Tools/modMinecraft/src/client/java/com/beatlamp/client/render/RainbowLedRenderer.java) trước đó vẽ bề mặt phát quang kích thước đầy đủ 16x16 (`half = 0.503F`) đè lên toàn bộ mặt khối, che lấp hoàn toàn viền kim loại ở cả hai chế độ.
* **Giải pháp:**
  * Cập nhật logic vẽ:
    * Khi `framed`: Tọa độ in-plane giới hạn ở `inset = 0.375F` (đúng bằng kích thước 12x12 pixel ở giữa), chiều sâu `h = 0.499F`. Viền kim loại 2 pixel bên ngoài hoàn toàn lộ rõ, sắc nét.
    * Khi `frameless`: Tọa độ in-plane mở rộng ra `inset = 0.501F` (toàn bộ 16x16 pixel), chiều sâu `h = 0.501F`. Bề mặt đèn phát sáng tràn viền, tạo thành mảng liền khối vô cực.
  * Đã đồng bộ sửa đổi này trên tất cả các renderer trong dự án.

### Vấn đề 4: Tùy chọn Beat Lamp ở Fabric 1.20.1 không hoạt động đúng và chưa có khối LED
* **Triệu chứng:** Trên bản Fabric 1.20.1, khối LED không tồn tại, các tùy chọn chỉnh thông số của Beat Lamp không áp dụng được cho khối.
* **Nguyên nhân:**
  * Instance Fabric 1.20.1 đang chứa file mod cũ `1.1.0` từ tháng 9 trước đó.
  * Gói tin cấu hình trên Fabric 1.20.1 cần được lắng nghe cả thông qua `ServerPlayNetworking.registerGlobalReceiver` để tương thích hoàn hảo.
* **Giải pháp:**
  * Bổ sung đầy đủ mã nguồn khối Rainbow LED vào [fabric-1.20.1/](file:///d:/fileluu/Tools/modMinecraft/fabric-1.20.1).
  * Đăng ký bộ nhận gói tin `LAMP_CONFIGURE_ID` và `RAINBOW_LED_CONFIGURE_ID` qua `ServerPlayNetworking.registerGlobalReceiver` trong [BeatLampFabric.java](file:///d:/fileluu/Tools/modMinecraft/fabric-1.20.1/src/main/java/com/beatlamp/fabric/BeatLampFabric.java).
  * Xóa bỏ file jar 1.1.0 cũ và nạp file jar 1.2.0 mới đã biên dịch.

### Vấn đề 5: Lỗi biên dịch và thiếu file build của các phiên bản Minecraft 1.21.1
* **Triệu chứng:** Thư mục `build/libs` không có các file jar của phiên bản 1.21.1 (Fabric, NeoForge, Forge).
* **Nguyên nhân:**
  * Hai bộ vẽ [BeatLampRenderer.java](file:///d:/fileluu/Tools/modMinecraft/src/client/java/com/beatlamp/client/render/BeatLampRenderer.java) và [RainbowLedRenderer.java](file:///d:/fileluu/Tools/modMinecraft/src/client/java/com/beatlamp/client/render/RainbowLedRenderer.java) dùng cú pháp cũ của 1.20.1 (`new ResourceLocation` và `consumer.vertex(...)`), khiến `:compileClientJava` bị lỗi.
  * `controllerUser` của `RainbowLedBlockEntity` trong [BeatLampNeoForgeClient.java](file:///d:/fileluu/Tools/modMinecraft/neoforge/src/main/java/com/beatlamp/neoforge/BeatLampNeoForgeClient.java) và [BeatLampForgeClient.java](file:///d:/fileluu/Tools/modMinecraft/forge/src/main/java/com/beatlamp/forge/BeatLampForgeClient.java) truyền sai tham số lambda `(level, pos, player)` thay vì `led`.
* **Giải pháp:**
  * Cập nhật cú pháp chuẩn 1.21.1: dùng `ResourceLocation.fromNamespaceAndPath` và `consumer.addVertex(...)`.
  * Sửa lại lambda `controllerUser = led -> { ... new RainbowLedConfigScreen(led) }` trên cả NeoForge và Forge.
  * Bổ sung gán các biến tĩnh `RAINBOW_LED_BLOCK`, `RAINBOW_LED` và `RAINBOW_LED_BLOCK_ITEM` trong `commonSetup` của NeoForge và Forge.
  * Đóng gói thành công `beatlamp-fabric-1.21.1-1.2.0.jar`, `beatlamp-neoforge-1.21.1-1.2.0.jar` và `beatlamp-forge-1.21.1-1.2.0.jar`.

### Vấn đề 6: Lỗi văng game trên Fabric 1.20.1 do sai package MusicDiscMakerDiscMixin & Dọn dẹp WaterMedia/WaterFrames
* **Triệu chứng:** Khi khởi động Fabric 1.20.1 gặp lỗi crash:
  `ClassNotFoundException: The specified mixin 'com.beatlamp.fabric.mixin.musicdiscmaker.MusicDiscMakerDiscMixin' was not found`.
* **Nguyên nhân:**
  * File `MusicDiscMakerDiscMixin.java` được đặt đúng thư mục `fabric-1.20.1/src/main/java/com/beatlamp/fabric/mixin/musicdiscmaker/` nhưng dòng khai báo package bên trong tệp vẫn giữ nguyên `package com.beatlamp.client.mixin.musicdiscmaker;`. Sự sai lệch này khiến Knot ClassLoader của Fabric không thể nạp class.
  * Mã nguồn thử nghiệm WaterMedia / WaterFrames trước đó còn sót lại ở nhiều subproject, trong khi tính năng này không khả thi do WaterMedia xuất luồng PCM trực tiếp từ thư viện C của VLC vào card âm thanh hệ điều hành, bỏ qua Java OpenAL.
* **Giải pháp:**
  * Đã sửa lại dòng khai báo package chuẩn xác trong [MusicDiscMakerDiscMixin.java](file:///d:/fileluu/Tools/modMinecraft/fabric-1.20.1/src/main/java/com/beatlamp/fabric/mixin/musicdiscmaker/MusicDiscMakerDiscMixin.java): `package com.beatlamp.fabric.mixin.musicdiscmaker;`.
  * Xóa bỏ hoàn toàn các tệp `WaterMediaAudioBridge.java`, các gói mixin `watermedia` và `waterframes`, thư mục `videolan/`, đồng thời loại bỏ các lời gọi `WaterMediaAudioBridge` trong toàn bộ các tệp `JukeboxAudioTracker.java`, `BeatLampClientMixinPlugin.java`, `beatlamp.client.mixins.json` và thẻ block `jukebox_sources.json`.

### Vấn đề 7: Triển khai hoàn thiện Rainbow LED cho NeoForge 1.21.4
* **Triệu chứng:** NeoForge 1.21.4 là loader duy nhất chưa được triển khai khối LED Decor (`RainbowLedBlock`).
* **Giải pháp:**
  * Bổ sung đăng ký `RAINBOW_LED_BLOCK`, `RAINBOW_LED_BE`, `RAINBOW_LED_ITEM`, thêm vào Creative Tab, gán biến tĩnh trong `commonSetup` và đăng ký payload mạng `RainbowLedConfigurePayload` trong [BeatLampNeoForge.java](file:///d:/fileluu/Tools/modMinecraft/fabric-1.21.4/src/neoforge/java/com/beatlamp/neoforge/BeatLampNeoForge.java).
  * Đăng ký renderer `RainbowLedRenderer` và hành động mở màn hình `RainbowLedBlockEntity.controllerUser` trong [BeatLampNeoForgeClient.java](file:///d:/fileluu/Tools/modMinecraft/fabric-1.21.4/src/neoforge/java/com/beatlamp/neoforge/BeatLampNeoForgeClient.java).

---

## 5. Danh Sách Các Tệp Đã Biên Dịch & Vị Trí Cài Đặt

Tất cả 9 file mod mới nhất phiên bản `1.2.1` đã được biên dịch thành công và chép trực tiếp vào các instance tương ứng của SKlauncher:

| Phiên bản Minecraft | Mod Loader | Tệp Jar Thành Phẩm | Vị trí cài đặt (SKlauncher) | Trạng thái |
| :--- | :--- | :--- | :--- | :--- |
| **1.21.7** | **NeoForge** | `beatlamp-neoforge-1.21.7-1.2.1.jar` | `C:\Users\khang\AppData\Roaming\.sklauncher\instances\1-21-7-neoforge\mods\` | Hoàn thiện & Đã triển khai |
| **1.21.7** | **Fabric** | `beatlamp-fabric-1.21.7-1.2.1.jar` | `C:\Users\khang\AppData\Roaming\.sklauncher\instances\1-21-7-fabric\mods\` | Hoàn thiện & Đã triển khai |
| **1.21.4** | **NeoForge** | `beatlamp-neoforge-1.21.4-1.2.1.jar` | `C:\Users\khang\AppData\Roaming\.sklauncher\instances\neoforge-1-21-4\mods\` | Hoàn thiện & Đã triển khai |
| **1.21.4** | **Fabric** | `beatlamp-fabric-1.21.4-1.2.1.jar` | `C:\Users\khang\AppData\Roaming\.sklauncher\instances\fabric1-21-4\mods\` | Hoàn thiện & Đã triển khai |
| **1.21.1** | **NeoForge** | `beatlamp-neoforge-1.21.1-1.2.1.jar` | `C:\Users\khang\AppData\Roaming\.sklauncher\instances\neoforge1-21-1\mods\` | Hoàn thiện & Đã triển khai |
| **1.21.1** | **Forge** | `beatlamp-forge-1.21.1-1.2.1.jar` | `C:\Users\khang\AppData\Roaming\.sklauncher\instances\forge1-21-1\mods\` | Hoàn thiện & Đã triển khai |
| **1.21.1** | **Fabric** | `beatlamp-fabric-1.21.1-1.2.1.jar` | `C:\Users\khang\AppData\Roaming\.sklauncher\instances\fabric1-21-1\mods\` | Hoàn thiện & Đã triển khai |
| **1.20.1** | **Forge** | `beatlamp-forge-1.20.1-1.2.1.jar` | `C:\Users\khang\AppData\Roaming\.sklauncher\instances\forge1-20-1\mods\` | Hoàn thiện & Đã triển khai |
| **1.20.1** | **Fabric** | `beatlamp-fabric-1.20.1-1.2.1.jar` | `C:\Users\khang\AppData\Roaming\.sklauncher\instances\fabric1-20-1\mods\` | Hoàn thiện & Đã triển khai |

Kho lưu trữ bản build gốc trong dự án: [build/libs/](file:///d:/fileluu/Tools/modMinecraft/build/libs)

---

## 6. Các Lệnh Build Phổ Biến Cho Phiên Tiếp Theo

Khi tiếp tục phát triển ở phiên mới, bạn có thể sử dụng các lệnh PowerShell sau:

```powershell
# Build toàn bộ các nền tảng và gom tất cả file jar về build/libs:
.\gradlew.bat assemble

# Chỉ build nhanh các bản 1.21.1 (Fabric, NeoForge, Forge):
.\gradlew.bat remapJar neoforgeJar forgeJar

# Chỉ build nhanh các bản 1.21.7 (Fabric & NeoForge):
.\gradlew.bat :fabric-1.21.7:assemble copy1217Jars

# Chỉ build nhanh bản Fabric 1.20.1:
.\gradlew.bat :fabric-1.20.1:assemble copyFabric1201Jar

# Chỉ build nhanh bản Forge 1.20.1:
.\gradlew.bat forge1201Jar
```

---

## 7. Hướng Dẫn Kiểm Tra Nhanh Trong Game (Quick Verification)

1. **Khởi động 1.21.7 NeoForge:**
   * Bấm phím **O**: Menu cài đặt xuất hiện mượt mà, không bị crash.
   * Mở tab sáng tạo Beat Lamp: Thấy khối `Rainbow LED Block`.
2. **Kiểm tra hiệu ứng viền đèn (Fabric hoặc NeoForge 1.21.7):**
   * Đặt 1 khối `Rainbow LED Block`, cầm `Lamp Controller` click phải vào khối.
   * Chuyển đổi giữa `Framed: ON` (có viền kim loại 2 pixel rõ ràng) và `Framed: OFF / Frameless` (đèn phát sáng tràn viền).
3. **Khởi động 1.20.1 Fabric:**
   * Khối `Rainbow LED Block` có mặt trong tab sáng tạo.
   * Đặt khối `Beat Lamp`, click phải bằng gậy điều khiển, thay đổi màu sắc hoặc chế độ nhịp điệu rồi bấm Done: khối đèn lập tức chuyển trạng thái theo cài đặt.
