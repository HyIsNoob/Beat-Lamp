# Beat Lamp — Tech Documentation

## Tổng quan

Mod Fabric cho **Minecraft 1.21.1** (Java 21, Mojang mappings). Block `beat_lamp` phát sáng **theo nhạc đang phát từ jukebox**: client tự decode file .ogg của đĩa nhạc, chạy FFT realtime, rồi render hiệu ứng sáng/đổi màu lên lamp.

- Mod ID: `beatlamp`, version `0.1.0`
- Fabric Loader >= 0.19.3, Fabric API 0.116.15+1.21.1, Loom 1.17-SNAPSHOT
- Mapping: **official Mojang** (tên class là tên Mojang: `BlockEntity`, `Level`, `useOn`...)
- Split sourcesets: `src/main` (common) + `src/client` (client-only) — bật trong build.gradle qua `splitEnvironmentSourceSets()`

## Cấu trúc code

### Common (`src/main/java/com/beatlamp/`)

| File | Vai trò |
|---|---|
| `BeatLamp.java` | Entry point chính. Đăng ký network receiver C2S `LampConfigurePayload` + `LampSourcePayload`. Chứa logic **link nhóm lamp** (`handleLink`: click 2 góc với Lamp Controller để link mọi lamp trong box, max `MAX_GROUP_SIZE=512`, range `LINK_RANGE=48`), `floodFill()` (tự nhóm các lamp liền kề 6 chiều nếu không có manual group), và **bind jukebox nguồn** (`handleSourceSelect`: sneak+click jukebox → toggle `SOURCE_POS` trên controller; `bindSource`: sneak+click lamp khi có nguồn chờ → set `sourcePos` cho cả nhóm + xóa nguồn chờ). |
| `JukeboxTracker.java` | **Server-side** registry các jukebox đang phát: `PLAYING: Map<ResourceKey<Level>, Set<BlockPos>>`. Mixin `JukeboxSongPlayer.play/stop/tick` + `JukeboxBlockEntity.setRemoved` cập nhật. `isPlayingNear(level, pos)` check bán kính 64 block — dùng cho `idleLight`. |
| `mixin/JukeboxSongPlayerMixin.java` | Inject TAIL `play`/`stop`/`tick` của `JukeboxSongPlayer` (class vanilla, field `blockPos`) → tracker. **Lưu ý descriptor**: `play(LevelAccessor, Holder<JukeboxSong>)` nên handler phải có tham số `Holder<?>` (erased `Lnet/minecraft/core/Holder;`), thiếu là crash `Invalid descriptor` ngay lần đầu chạm jukebox. Lý do hook class này thay vì BE: `JukeboxBlockEntity.tick` là **static** và chỉ chạy khi HAS_RECORD=true; `setRemoved` không được khai báo trong `JukeboxBlockEntity` (nằm ở class cha `BlockEntity`) nên mixin không target được. Phá jukebox giữa chừng vẫn ổn: `onRemove` → `popOutTheItem` → `setTheItem(empty)` → `JukeboxSongPlayer.stop`. |
| `BeatLampBlocks.java` | Đăng ký block `BEAT_LAMP`: strength 0.3, sound GLASS, **lightLevel động theo property LIT** (`state -> LIT ? 15 : 0` — trước đây hardcode 15 khiến idleLight không tắt được ánh sáng), noOcclusion. + `BEAT_EMITTER` (strength 0.8, sound WOOD). |
| `BeatLampBlockEntities.java` | Đăng ký `BlockEntityType<BeatLampBlockEntity>` + `BlockEntityType<BeatEmitterBlockEntity>`. |
| `BeatLampItems.java` | Đăng ký `BlockItem BEAT_LAMP`, `BlockItem BEAT_EMITTER` (tab REDSTONE_BLOCKS), `LampControllerItem CONTROLLER` (stacksTo 1), và **DataComponent** `ANCHOR_POS` (BlockPos — lưu góc thứ nhất khi link) + `SOURCE_POS` (BlockPos — jukebox nguồn chờ bind). Thêm vào tab COLORED_BLOCKS + FUNCTIONAL_BLOCKS + TOOLS_AND_UTILITIES. |
| `block/BeatLampBlock.java` | BaseEntityBlock, property `FRAMELESS` (bool) + `LIT` (bool, điều khiển lightLevel 15/0 — bật theo nhạc khi idleLight=false). `useItemOn`: controller → handleLink; dye → setColor (tiêu dye nếu không creative). `useWithoutItem`: không shift → cycleMode, shift → cycleColor. Ticker: client (`clientTick`) + **server** (`serverTick`, mỗi 10 tick set LIT = idleLight \|\| JukeboxTracker.isPlayingNear). `onRemove` gọi `onRemovedFromWorld()` để dọn manual group. |
| `block/BeatLampBlockEntity.java` | **Trái tim của mod**. Lưu config: `color` (0 = COLOR_OLED tức đen/cầu vồng khi có nhạc), `mode`, `sensitivity`, `speed`, `frameless`, `blackback`, `idleLight` (true = block luôn phát sáng; false = chỉ sáng khi có nhạc gần), `particles`, `orientation`, `manualGroup` (List\<BlockPos\>), `sourcePos` (BlockPos nullable — jukebox nguồn đã bind; null = phản ứng với jukebox to nhất gần nhất). Public fields đọc bởi renderer mỗi tick: `pulse`, `beatPulse`, `smoothLevel`, `barValue`, `spectrumLevel`, `displayColor`, `groupIndex/groupSize`, `groupDistance`, `columnIndex/columnSize`, `bandIndex/bandCount`. Pattern quan trọng: vì class này nằm ở common nhưng cần tick/render riêng client → dùng **static hook interface** `clientTicker` + `controllerUser` mà client inject lúc khởi động (tránh ClassLoading crash dedicated server). Sync NBT thủ công qua `getUpdateTag`/`getUpdatePacket`; `markUpdated()` gửi `ClientboundBlockEntityDataPacket` cho player trong bán kính 64 block (4096²). NBT key: color, mode, sensitivity, speed, frameless, blackback, idleLight, particles, orientation, group (ListTag LongTag), source (LongTag). |
| `block/LampMode.java` | Enum: PULSE, RGB, SPECTRUM, RIPPLE, WAVE, SCAN (+`next()`, `byName`). |
| `block/LampParticles.java` | Enum: OFF, NOTE, END_ROD, FIREWORK, GLOW, MIXED. |
| `block/LampOrientation.java` | Enum: AUTO, EAST_WEST(X), NORTH_SOUTH(Z), VERTICAL(Y) — ép trục tính group thay vì auto-detect. **File mới chưa commit.** |
| `item/LampControllerItem.java` | `useOn`: shift+click **jukebox** → `handleSourceSelect` (toggle nguồn chờ trên controller); shift+click **emitter** khi có nguồn chờ → `bindEmitterSource`; shift+click lamp → nếu controller có nguồn chờ → `bindSource`, ngược lại mở config screen (qua static `controllerUser`, chỉ client side); click thường → server `BeatLamp.handleLink`. `use` (click air): cancel anchor nếu đang chọn. Tooltip hiện toạ độ anchor từ DataComponent. |
| `block/BeatEmitterBlock.java` + `block/BeatEmitterBlockEntity.java` | **Phase B — Beat Emitter**: block xuất **redstone signal 0–15 theo nhạc** (isSignalSource + getSignal/getDirectSignal đọc `signalLevel` từ BE). Client tick (hook `clientTicker` như lamp) tính energy từ jukebox đã bind (`sourcePos`) rồi gửi C2S `EmitterSignalPayload(pos, signal)` mỗi ≥2 tick khi đổi / keep-alive 40 tick. Server: clamp 0–15, check player trong 64 block, `updateNeighborsAt` khi đổi; serverTick timeout 40 tick không có update → signal=0 (client out of range). Model dùng texture `minecraft:block/note_block`. Recipe: note block + 4 redstone (shapeless). |
| `network/EmitterSignalPayload.java` | C2S `beatlamp:emitter_signal`: pos + signal byte. |
| `network/LampConfigurePayload.java` | CustomPacketPayload C2S `beatlamp:configure_lamp`. Gửi: pos, mode, sensitivity, speed, color, frameless, blackback, idleLight, particles, orientation, unlink. Server nhận: nếu `unlink=true` → `unlinkGroup`; ngược lại áp config lên manual group (nếu size>=2) hoặc flood fill. |
| `network/LampSourcePayload.java` | CustomPacketPayload C2S `beatlamp:clear_lamp_source`. Gửi pos lamp → server xóa `sourcePos` của cả nhóm (manual group hoặc flood fill). Dùng bởi nút Source trong config screen. |

### Client (`src/client/java/com/beatlamp/client/`)

| File | Vai trò |
|---|---|
 | `BeatLampClient.java` | Client entrypoint. RenderType cutout cho block; đăng ký BER; inject `clientTicker`/`controllerUser` vào BE; `WorldRenderEvents.AFTER_TRANSLUCENT` → outline renderer; `ClientTickEvents.END_CLIENT_TICK` → `JukeboxAudioTracker.clientTick()`; disconnect → clear songs; load `BeatLampClientConfig` lúc init. **`tickLamp()` là vòng lặp hiệu ứng chính mỗi tick mỗi lamp**: lấy level/beat/spectrum từ tracker theo khoảng cách (có lọc theo `sourcePos` nếu lamp đã bind jukebox), smooth (attack nhanh/thả chậm), rồi switch theo mode để ra `displayColor` và `barValue`. `resolveColor()`: energy < 0.03 → màu 0 (tắt); OLED → hue cầu vồng theo vị trí + thời gian; màu dye → giảm brightness theo energy. `countRun()`: đếm chuỗi lamp liên tiếp tối đa 64 theo 1 hướng. **TỐI ƯU TOPOLOGY (đừng bỏ)**: `updateGroupInfo()` (tính groupIndex/Size/Distance + band/column) là O(n)‑O(n²) theo nhóm, chỉ chạy khi `needsTopology(mode)` (SPECTRUM/RIPPLE/WAVE/SCAN) VÀ `shouldRefreshTopology()` (mỗi `TOPOLOGY_REFRESH_TICKS=10` tick, lệch pha theo vị trí). PULSE/RGB bỏ qua hoàn toàn. `applyManualGroup` tính index bằng **đếm O(n) qua `compareByAxis`**, KHÔNG sort/cấp phát ArrayList (sort mỗi tick từng lamp từng gây tụt FPS 100→10). SPECTRUM dùng `getBandAt(pos,band,source)` (không cấp phát mảng). **Particles**: spawn trên **mặt lộ** (face không kề lamp khác — `randomExposedFace`, reservoir sampling) với velocity hướng ra ngoài, tỷ lệ spawn theo beatPulse (lerp threshold 30→8). |
 | `BeatLampClientConfig.java` | Config client đơn giản qua file `config/beatlamp-client.properties` (`audioQuality=high|low`). `highQualityBeat` chọn FFT 2048 (High) hay 1024 (Low) cho analyzer — áp dụng với bài mới bắt đầu phát. Được toggle từ nút Beat Quality trong LampConfigScreen. |
| `audio/JukeboxAudioTracker.java` | Singleton map tĩnh `ACTIVE_SONGS: Map<BlockPos, ActiveSong>`. Mixin `SoundEngine.play/stop` gọi `onSoundPlayed/onSoundStopped` — chỉ quan tâm `SoundSource.RECORDS`. Khi phát: resolve Sound thật (đi qua chain SOUND_EVENT tối đa 8 lần), mở stream bằng **`JOrbisAudioStream`**, spawn thread daemon `BeatLamp-Decode-*` chạy `decodeLoop`: đọc chunk → toMono → `analyzer.push()`; tự throttle sleep để bám timeline thật (xử lý pause game bằng cộng dồn nanoTime); bỏ qua lỗi decode, give up sau 64 lỗi liên tiếp. Truy vấn: `getLevelAt/getBeatPulseAt/getSpectrumAt(Vec3)` — chọn bài "to" nhất theo falloff tuyến tính bán kính 64 block. |
| `audio/AudioAnalyzer.java` | FFT 1024 điểm (ring buffer), Hann window. 16 band log từ 40Hz–12kHz, bass flux beat detection 25–160Hz: **local flux average theo cửa sổ 1.2s** (ring buffer 64 entry theo totalSamples) + **rising-edge check** (`flux > previousFlux`) + adaptive threshold (`localAvg*1.35 + 0.0012`, bass gate 0.008, min gap 0.15s), envelope follower cho level tổng. `consumeBeat()` một-lần-per-tick. `push` có overload `(samples, offset, count)` — decodeLoop push slice 1024 mẫu để beat timing ~23ms. |
| `audio/Fft.java` | radix-2 iterative FFT thuần, không thư viện ngoài. |
| `gui/LampConfigScreen.java` | Screen cấu hình (mở bằng shift+click controller lên lamp): mode, sensitivity slider 0.25–3, speed slider 0.25–3, màu (palette DyeColor + OLED), frameless, blackback, idle light, orientation, particles, reset, unlink. **Chỉ gửi packet trong `onClose()`** (Done/unlink/Esc đều qua đây). |
| `render/BeatLampRenderer.java` | BER vẽ **1 cube duy nhất** quanh block (đã bỏ shell/halo/bloom — shader tự có glow, các lớp giả glow gây overlap giữa lamp liền kề). Core cube texture `beat_lamp_core.png` (trắng đặc): blackback → `entityCutoutNoCull` (panel đặc), ngược lại → `entityTranslucentEmissive`. PULSE co giãn half 0.3→0.46; các mode khác full-size 0.503. Brightness = energy × 1.5 **không floor** → im lặng = đen thật. Skip render nếu intensity <= 0.02 và không blackback. Lưu ý `resolveColor()` ở BeatLampClient cũng đã bỏ floor 0.2 cho màu dye (`value = hsb[2] * clamp(energy*1.25)`) — nếu thêm lại floor nào vào brightness/màu thì blackback sẽ bị "sáng mờ" lúc im lặng. |
| `render/LampOutlineRenderer.java` | Giữ controller trong tay: không có anchor → viền cyan preview flood-fill/manual-group lamp đang nhìn (cache 5 tick); có anchor → khung vàng vùng chọn + fill quad debug. |
| `mixin/SoundEngineMixin.java` | Inject TAIL `play` và HEAD `stop(SoundInstance)` của `SoundEngine` → hook tracker. |

### Resources

- `fabric.mod.json`: entrypoints main=`com.beatlamp.BeatLamp`, client=`com.beatlamp.client.BeatLampClient`; mixins `beatlamp.mixins.json` (rỗng) + `beatlamp.client.mixins.json`.
- Assets: blockstates/models `beat_lamp.json` (4 variants: frameless×lit) & `beat_lamp_frameless.json` theo property `frameless`+`lit`; textures `beat_lamp.png`, `beat_lamp_core.png`, `beat_lamp_frameless.png`; lang `en_us.json`.
- Data: recipe `beat_lamp.json`, `controller.json`; loot table drop chính nó.
- Lưu ý: `build.gradle` vẫn để `"modid"` trong block `mods` (hoạt động nhưng nên đổi thành `"beatlamp"`).

## Luồng dữ liệu chính (audio-reactive)

```
Jukebox phát nhạc (client)
  └─ SoundEngineMixin.play → JukeboxAudioTracker.startSong
       └─ Thread decode: JOrbisAudioStream → mono → AudioAnalyzer.push
            └─ FFT 1024 → envelope level + 16 bands + beat flag
ClientTick (mỗi lamp trong range render)
  └─ BeatLampClient.tickLamp
       ├─ JukeboxAudioTracker.getLevelAt/getBeatPulseAt (falloff 64 blocks)
       ├─ updateGroupInfo: manual group hoặc countRun auto-detect
       └─ switch(mode) → displayColor + barValue
             └─ BeatLampRenderer.render vẽ core/shell/halo/bloom
```

Config sync: GUI → `LampConfigurePayload` (C2S) → server áp cho cả nhóm → `markUpdated()` → BE data packet về client.

## Build / chạy

```powershell
.\gradlew.bat build          # build jar (output build/libs/)
.\gradlew.bat runClient      # chạy client test
.\gradlew.bat compileJava compileClientJava   # check compile nhanh
```

## Việc còn dang dở / đã biết

- `build.gradle`: `"modid"` chưa đổi thành `"beatlamp"`.
- README.md vẫn là template mặc định.
- **Lỗi "nền đỏ" vanilla đã fix bằng cách ép đen tuyệt đối**: renderer vẽ cube đen thuần (0,0,0) khi intensity <= 0.02 + blackback (shader vanilla chỉ NHÂN màu nên đen×gì=cũng đen — nếu thấy đỏ nghĩa là đang chạy build cũ, phải `gradlew build` lại).
- **Lỗi "nền đỏ" vanilla — ROOT CAUSE là `.setOverlay(0)`**: phải dùng `OverlayTexture.NO_OVERLAY` (= `pack(0,10)`, sample hàng v=10 trung tính). `setOverlay(0)`=`pack(0,0)` sample hàng v=0 nằm trong vùng đỏ (hurt-flash) của overlay texture → fragment shader vanilla `mix(overlayColor.rgb, color.rgb, overlayColor.a)` trộn cube đen sang đỏ. Shader-pack bỏ qua overlay vanilla nên mới thấy đen. **Mọi BER tự vẽ vertex phải `.setOverlay(OverlayTexture.NO_OVERLAY)`, KHÔNG dùng `0`.**
- `LampOrientation.java` là file mới (chưa commit).
