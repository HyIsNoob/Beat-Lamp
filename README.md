# Beat Lamp

Beat Lamp is a multi-loader stage lighting, concert effects, and audio-reactive visualizer mod for Minecraft. Powered by real-time client-side FFT audio processing, it dynamically synchronizes stage lights, volumetric beams, laser projectors, pyro fountains, fog, and redstone signals with nearby jukebox music and video displays.

Compatible with vanilla discs, modded music discs, and resource packs.

---

### Important Compatibility Notice
* **Replay Mod**: In-game playback functions normally. However, offline video rendering is incompatible because Replay Mod mutes sound engine output during export, leaving lamps without an audio signal.

---

## Features

### Stage Fixtures
* **Beat Lamp**: Matrix lamp visualizer with 9 reactive display modes (Pulse, RGB, Spectrum, VU Meter, Oscilloscope, Matrix Rain, Ripple, Wave, Scan), 16 dye colors, OLED rainbow mode, and frameless styling.
* **Stage Light**: Moving-head concert spotlights projecting dual-layer 3D volumetric light beams. Features 5 modes (Sweep, Beat Step, Static, Strobe, Chase) and a configurable Tempo Pulse option.
* **RGB Laser Projector**: High-intensity 3D laser system with 1 to 16 beams and 4 sweep/burst modes.
* **Beat Fountain**: Music-triggered particle fountain with customizable spray height and explosive firework bursts on bass drops.
* **Stage Fog Generator**: Low-lying atmospheric dry ice fog and high-pressure CO2 stage jets.
* **Beat Emitter**: Redstone controller outputting 0 to 15 analog signal strength mapped to live music frequency bands, kicks, snares, and drops.
* **Master DMX Console**: Central control station with real-time audio monitor, instant Blackout, Strobe All, Master Dimmer, and remote fixture group management.
* **Stage Props**: Titanium Obsidian Stage Jukebox, Neon DJ Deck, and Line Array Stage Speakers.

### Setup Tools
* **Group Linker (`beatlamp:linker`)**: Click a stage device, then click any block to define the opposite corner of a 3D bounding box. All matching fixtures within the box are instantly linked into a synchronized group.
* **Lamp Controller (`beatlamp:controller`)**: Right-click fixtures to open their dark-mode configuration GUI. Sneak-click any jukebox to bind fixtures exclusively to that audio source.

---

## Client Settings & Optimization (Default Key: `O`)

Beat Lamp includes a dedicated client configuration menu to ensure smooth performance on multiplayer servers and low-end hardware:

* **Hotkey**: Press **`O`** (rebindable in Controls) to open the settings screen anytime.
* **Stage Effects (Master Switch)**: Instantly toggle all stage lighting and visual effects on or off.
* **Audio Engine Profile**:
  * **STUDIO**: Full multi-band Fourier frequency analysis.
  * **LITE**: Fast envelope detection optimized for maximum FPS.
  * **OFF**: Stops audio decoders entirely, consuming 0% audio CPU.
* **Volumetric Beams**: Choose between **HIGH** (dual-layer glow cone), **MEDIUM** (single-layer, 50% fillrate reduction), or **OFF**.
* **3D Lasers**: Toggle laser beam rendering on or off.
* **Particle Density**: Adjust stage particles to **100%**, **50%**, **25%**, or **OFF**.
* **Anti-Strobe (Photosensitivity Protection)**: Converts harsh strobing into smooth gradual fading for light-sensitive players.
* **Render Distance**: Adjustable distance slider (16 to 128 blocks) to cull distant fixtures.

---

## Crafting Recipes

* **Beat Lamp (x4)**: 7 Glass surrounding 1 Redstone Lamp and 1 Note Block.
* **Beat Emitter (x1)**: 1 Note Block + 4 Redstone Dust (shapeless).
* **Stage Light (x1)**: 1 Sea Lantern + 1 Glass + 1 Redstone Dust (shapeless).
* **Laser Projector (x1)**: 1 Amethyst Shard + 1 Glowstone Dust + 1 Iron Ingot + 1 Redstone Dust (shapeless).
* **Beat Fountain (x1)**: 1 Dispenser + 1 Firework Rocket + 1 Redstone Dust (shapeless).
* **Fog Generator (x1)**: 1 Campfire + 1 Smooth Stone + 1 Redstone Dust (shapeless).
* **Master DMX Console (x1)**: 1 Redstone Comparator + 1 Iron Ingot + 1 Smooth Stone + 1 Redstone Dust (shapeless).
* **Stage Jukebox (x1)**: 1 Jukebox + 1 Obsidian + 1 Amethyst Shard + 1 Redstone Dust (shapeless).
* **DJ Deck (x1)**: 1 Iron Bars + 1 Obsidian + 1 Redstone Dust + 1 Glowstone Dust (shapeless).
* **Stage Speaker (x1)**: 1 Note Block + 1 Iron Ingot + 1 Oak Planks + 1 Redstone Dust (shapeless).
* **Lamp Controller (x1)**: 5 Iron Ingots, 1 Beat Lamp, 1 Redstone Dust (shaped).
* **Group Linker (x1)**: 3 Iron Ingots, 2 Gold Ingots, 1 Redstone Dust (shaped).

---

## Integrations

* **DreamDisplays**: Fully compatible. Stage fixtures synchronize in real-time with audio from DreamDisplays video screens during multiplayer watch parties.

---

## Supported Languages

* **English (`en_us`)** - 100% complete
* **Korean (`ko_kr` / 한국어)** - 100% complete
* **Vietnamese (`vi_vn` / Tiếng Việt)** - 100% complete

---

## Supported Platforms

* **Minecraft 1.21.4**: Fabric, NeoForge
* **Minecraft 1.21.1**: Fabric, NeoForge, Forge
* **Minecraft 1.20.1**: Fabric, Forge