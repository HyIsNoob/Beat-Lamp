# Changelog

All notable changes to the Beat Lamp mod are documented in this file.

## [1.1.0] - 2026-09-07

### Performance & Engine
- **Zero-Allocation Rendering**: Rebuilt vertex builders for `BeatLampRenderer` and `StageLightRenderer` to eliminate heap array allocations per frame. Resolves garbage collection pauses and significantly boosts framerates (up to 100+ FPS).
- **Multiplayer TPS Protection**: Client-side audio decoding and rendering optimizations prevent tick stalls on integrated LAN hosts and dedicated servers.
- **Client Settings Menu (Key `O`)**: Added a dedicated client configuration GUI accessible via the configurable `O` hotkey.
- **Client Quality Scaling**: Added toggles for Master Stage Effects (instant on/off), Audio Engine modes (`STUDIO`, `LITE`, `OFF`), Volumetric Beam quality (`HIGH`, `MEDIUM`, `OFF`), 3D Lasers, Particle Density (`100%`, `50%`, `25%`, `OFF`), and Render Distance culling (16m to 128m).
- **Anti-Strobe Mode**: Added a photosensitivity protection option that smoothly dims strobe flashes instead of rapid on/off flickering.

### Audio & Beat Sync
- **Jukebox Pause Synchronization**: Fixed beat drift when pausing and resuming in Singleplayer by calculating pause duration and realigning audio metronomes.
- **Runaway Catch-Up Clamp**: Added a 200ms lag threshold to prevent burst audio decoding after lag spikes.
- **DreamDisplays Support**: Added direct audio synchronization with the DreamDisplays screen mod/plugin for multiplayer video sessions.

### Features & Usability
- **Stage Light Tempo Pulse**: Added a configurable `Tempo Pulse` toggle to Moving Head lights for alternating between smoothed tempo tracking and raw kick drum response.
- **Flexible Bounding Box Linking**: The Group Linker now accepts any target block (including vanilla blocks) as the second corner of the selection box. All matching fixtures inside the 3D volume are automatically grouped.
- **100% Korean Localization**: Complete native Korean (`ko_kr`) translations for all blocks, items, GUI screens, DMX console, guide tooltips, and chat messages.
- **Full Translation Parity**: Synchronized all 215 translation keys across English, Korean, and Vietnamese.
- **Multi-Loader Parity**: Clean builds and feature parity across Minecraft 1.20.1 (Fabric/Forge), 1.21.1 (Fabric/NeoForge/Forge), and 1.21.4 (Fabric/NeoForge).

### Compatibility
- **Replay Mod Notice**: Documented that offline video rendering with Replay Mod is unsupported due to sound engine muting during export (normal in-game playback remains unaffected).

---

## [1.0.0] - Initial Release

- Initial multi-loader release for Minecraft 1.20.1, 1.21.1, and 1.21.4 (Fabric, NeoForge, Forge).
- Beat Lamp with 9 reactive modes, 16 dye colors, OLED rainbow mode, and frameless options.
- Stage Light with 5 moving-head lighting modes and dual-layer atmospheric volumetric beams.
- RGB Laser Projector with 1 to 16 customizable 3D beams and 4 sweep modes.
- Stage Fog Generator with low-lying dry ice fog and high-pressure CO2 jet blasts.
- Beat Fountain with stage pyro jets and firework rocket launches on music drops.
- Beat Emitter with 6 music-to-redstone conversion modes.
- Titanium Obsidian Stage Jukebox, DJ Deck, and Line Array Speakers.
- Master DMX Console with Blackout, Strobe All, Dimmer, and remote group controls.
- Group Linker and universal Lamp Controller tools.
