# Beat Lamp

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.1%20%7C%201.21.4-brightgreen.svg)](https://minecraft.net/)
[![Modloaders](https://img.shields.io/badge/Modloaders-Fabric%20%7C%20NeoForge%20%7C%20Forge-blue.svg)](https://fabricmc.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Beat Lamp** is a premier audio-reactive concert lighting, stage production, and redstone synchronization mod for **Minecraft 1.20.1, 1.21.1, and 1.21.4**. It turns jukebox music into breathtaking live concert visuals using real-time audio FFT analysis, Dual-Band Spectral Flux, and Beat-Grid Metronome tracking.

Compatible with **vanilla music discs**, **custom resource packs**, and **modded jukeboxes**.

---

## Features & Stage Equipment

### 1. Beat Lamp
* **9 Audio-Reactive Visualizer Modes**:
  * **Pulse**: Dynamic scaling and flashing to the bass beat (with intelligent breakdown decay).
  * **RGB**: Smooth rainbow color transitions synchronized with song energy.
  * **Spectrum**: Equalizer visualizer columns that smoothly fill up.
  * **VU Meter**: Studio-grade volume decibel meter display.
  * **Oscilloscope**: Real-time oscilloscope audio waveform tracing.
  * **Matrix Rain**: Cyberpunk digital matrix waterfall stream.
  * **Ripple**: Circular waves of light expanding outward from group centers.
  * **Wave**: Energetic ripples sweeping along connected rows/walls.
  * **Scan**: Neon beams scanning back and forth across lamp arrays.
* **16 Dye Colors + OLED Mode**: Tint lamps with vanilla dyes or select OLED mode for deep blacks with vivid rainbow reactivity.
* **Frameless & Solid Back**: Create seamless giant video walls when placed side-by-side.

### 2. Stage Light (Concert Moving Head)
* **5 Concert Lighting Modes**: `Sweep`, `Beat Step`, `Static Beam`, `Strobe`, and `Chase`.
* **Atmospheric Dual-Layer 3D Beams**: Inner core beam + soft outer atmospheric glow cone sweeping smoothly up to 34 blocks!
* **Beat Color Cycles**: Color changes dynamically with the rhythm.

### 3. RGB Laser Projector
* **4 Laser Fan Modes**: `Fan Sweep`, `3D Cone Spin`, `Static Fan`, and `Beat Burst`.
* **Multi-Beam Laser Engine**: 1 to 8 customizable 3D laser beams with adjustable fan spread angles.

### 4. Stage Fog Generator
* **Low-Lying Dry-Ice Fog & High-Pressure CO2 Jets**: Emits dense ground fog and erupts powerful vertical CO2 blasts on drops and heavy bass beats.
* **Configurable Density & Radius**: Low, Medium, or High density covering up to 16 blocks radius.

### 5. Beat Fountain
* **Stage Pyro Jets & Fireworks Eruptions**: Continuously shoots stage flame sparks on beats and fires real firework rockets on structural music drops.
* **7 Particle Styles**: `Flame`, `Soul Flame`, `Firework Sparks`, `Glow Sparkles`, `Electric Spark`, `Colored Dust`, and `Mixed`.

### 6. Beat Redstone Emitter
* **6 Music-to-Redstone Modes**:
  * `Pulse (Beat Clock)`: 1-tick Redstone 15 pulse on every beat.
  * `Drop Pulse`: Redstone 15 burst only on major music drops (perfect for TNT cannons & pyro igniters).
  * `Continuous (0-15 Energy)`: Analog signal proportional to volume (for redstone lamp arrays and comparators).
  * `Kick Drum (Bass)`: Pulses on kick drum transients.
  * `Snare / Clap`: Pulses on snare / clap transients.
  * `Hi-Hat (Treble)`: Pulses on hi-hat / cymbal frequencies.
* **Signal Inversion & Sensitivity**: Fully adjustable sensitivity ($0.10x - 0.95x$) and inverted signal support.

### 7. Master DMX Console
* **Centralized Stage Lighting Desk**: Controls all stage equipment in a 64-block radius.
* **Master Overrides**: Instant **[BLACKOUT]** killswitch, **[STROBE ALL]** audience blinder, **Master Dimmer**, and **Master Speed**.
* **Remote Group Management**: Pin important groups to the top, toggle modes, adjust sensitivity, and mute individual fixtures remotely.

### 8. Stage Jukebox, DJ Deck & Line Array Speaker
* **Stage Jukebox**: Titanium-obsidian jukebox with 100% disc compatibility and visual vinyl disc slot.
* **DJ Performance Deck**: Dual-jog turntable performance desk with RGB pads.
* **Stage Line Array Speaker**: Stackable concert speaker cabinets for realistic stage setups.

### 9. Group Linker & Lamp Controller
* **Group Linker**: Click Corner 1, then Corner 2 to link entire structures into synchronized groups in seconds.
* **Lamp Controller**: Right-click any stage device to open its dedicated configuration GUI; Sneak + Right-Click a Jukebox to bind targeted audio sources.

---

## Performance & Compatibility

* **Zero-Allocation Rendering**: Render pipelines for Beat Lamp and Stage Light are completely allocation-free at runtime, eliminating Garbage Collection (GC) pauses, reducing micro-stutter, and boosting framerates (often 100+ FPS).
* **DreamDisplays Support**: Fully compatible with the DreamDisplays plugin/mod, reacting to web video and audio streams in real time across multiplayer servers.
* **Server Tick Friendly**: Client and server architectures are strictly decoupled. Audio DSP and vertex processing run on the client side, keeping server TPS at a smooth 20.
* **Shader, Sodium & Iris Safe**: Standard neutral blend states and compatible vertex formats.
* **LITE & STUDIO Engine Profiles**: Toggle between lightweight LITE and high-precision STUDIO FFT modes in DMX console settings.
* **Replay Mod Compatibility Notice**: Video rendering within Replay Mod is currently incompatible. Replay Mod mutes and disables the Minecraft audio engine during offline video rendering, resulting in no audio stream for the lamps to react to. Standard in-game replay playback without offline video export remains functional.

---

## Supported Versions & Requirements

| Minecraft Version | Supported Modloaders | Java Version |
|---|---|---|
| **1.20.1** | Fabric, Forge | Java 17+ |
| **1.21.1** | Fabric, NeoForge, Forge | Java 21+ |
| **1.21.4** | Fabric, NeoForge | Java 21+ |

---

## License

This project is licensed under the [MIT License](LICENSE).
