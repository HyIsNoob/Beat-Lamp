# Changelog

All notable changes to the Beat Lamp mod will be documented in this file.

## [1.1.0] - 2026-09-07

### Performance & Engine
- **Zero-Allocation Rendering**: Rebuilt vertex builders for `BeatLampRenderer` and `StageLightRenderer` to eliminate all heap array allocations per frame. This resolves Garbage Collection (GC) pauses and micro-stuttering, boosting FPS significantly (often from ~60 FPS up to 100+ FPS).
- **LAN Host & Server TPS Protection**: Eliminating client-side GC pressure prevents Stop-The-World freezes on integrated LAN servers, keeping multiplayer server TPS stable at 20.

### Audio & Beat Sync
- **Jukebox Pause Beat Synchronization**: Fixed a major desync where pausing the game in Singleplayer caused the Jukebox background decoder to drift ahead of OpenAL audio. The decoder now tracks pause duration and smoothly aligns the metronome upon unpausing.
- **Runaway Catch-Up Clamp**: Added a lag compensation clamp (200ms threshold) to prevent burst decoding if a system freeze or lag spike occurs.

### Features
- **Stage Light Tempo Pulse Option**: Added a configurable `Tempo Pulse` toggle to Stage Lights (Moving Heads). Allows switching between smoothed tempo pulse assist and raw kick transient reactivity.
- **Stage Light GUI & DMX Integration**: Integrated the new `Tempo Pulse` toggle into `StageLightConfigScreen` and remote DMX console configuration with full network synchronization.
- **DreamDisplays Integration**: Seamless real-time audio reactivity with the DreamDisplays screen plugin/mod for online videos and multiplayer watch parties.

### Compatibility
- **Replay Mod Notice**: Added documentation regarding Replay Mod video rendering. Because Replay Mod mutes and stops OpenAL audio during offline headless video rendering, lamps receive no audio signal during export and remain dark.

---

## [1.0.0] - Initial Release

- Initial multi-loader release for Minecraft 1.20.1, 1.21.1, and 1.21.4 (Fabric, NeoForge, Forge).
- Beat Lamp with 9 reactive modes, 16 dye colors + OLED rainbow, and frameless mode.
- Stage Light with 5 moving-head lighting modes and dual-layer atmospheric volumetric beams.
- RGB Laser Projector with 1 to 8 customizable 3D beams and 4 fan sweep modes.
- Stage Fog Generator with low-lying dry ice fog and high-pressure CO2 jet blasts.
- Beat Fountain with stage pyro jets and real firework rockets on music drops.
- Beat Emitter with 6 music-to-redstone modes.
- Titanium Obsidian Stage Jukebox, DJ Deck, and Line Array Speakers.
- Master DMX Console with Blackout, Strobe All, Dimmer, and remote group control.
- Group Linker and universal Lamp Controller tools.
