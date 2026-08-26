# Beat Lamp

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Modloader-Fabric-blue.svg)](https://fabricmc.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Beat Lamp** is an audio-reactive Fabric mod for Minecraft 1.21.1 that brings your music discs to life! Lamps and redstone emitters analyze the music playing from nearby jukeboxes in real-time using fast Fourier transform (FFT) audio processing.

Compatible with **vanilla music discs**, **custom resource packs**, and **modded music discs**!

---

## Features

### Beat Lamp
* **6 Audio-Reactive Visual Modes**:
  * **Pulse**: Lamps dynamically scale and flash to the bass beat.
  * **RGB**: Smooth rainbow color transitions synchronized with song energy.
  * **Spectrum**: Equalizer visualizer columns that smoothly fill up.
  * **Ripple**: Circular waves of light expanding outward from group centers.
  * **Wave**: Energetic ripples sweeping along connected rows/walls.
  * **Scan**: Neon beams scanning back and forth across lamp arrays.
* **16 Dye Colors + OLED Mode**: Tint lamps with vanilla dyes or select OLED mode for deep blacks with vivid rainbow reactivity.
* **Frameless & Solid Back Options**: Seamless visual walls when placed side-by-side.
* **Interactive Particle Effects**: Choose between Note, End Rod, Firework, Glow, or Mixed particles shooting from active faces.
* **Dynamic In-game Lighting**: Emits real light when music plays (`LIT` blockstate).

### Beat Emitter
* **Redstone Audio Reactive**: Outputs analog redstone power (**0 to 15**) in real-time according to music energy and bass drops.
* Perfect for automating light shows, piston dancing floors, TNT cannons, fireworks, and contraptions synced to jukeboxes!

### Lamp Controller
* **Live Selection Box**: Click two opposite corners (up to 48 blocks apart, max 512 lamps) to synchronize entire structures as one connected group.
* **Source Binding**: Sneak + right-click any Jukebox to bind specific lamps or emitters to that jukebox only (multi-track / multi-stage support).
* **In-game Configuration GUI**: Sneak + right-click a lamp with the controller to tune Sensitivity, Effect Speed, Orientation, Particles, Quality, and Visual Modes.

---

## Crafting Recipes

### 1. Beat Lamp *(Yields 4)*
```text
[ Glass ] [    Glass    ] [ Glass ]
[ Glass ] [Redstone Lamp] [ Glass ]
[ Glass ] [ Note Block  ] [ Glass ]
```

### 2. Beat Emitter *(Yields 1, Shapeless)*
```text
[ Note Block ] + [ Redstone Dust x4 ]
```

### 3. Lamp Controller *(Yields 1)*
```text
[Iron Ingot] [ Beat Lamp  ] [Iron Ingot]
[Iron Ingot] [Redstone Dust] [Iron Ingot]
[          ] [Iron Ingot  ] [          ]
```

---

## How to Use

### Basic Interaction (Without Controller)
* **Right-Click** lamp: Cycle through visual modes (*Pulse → RGB → Spectrum → Ripple → Wave → Scan*).
* **Sneak + Right-Click** lamp: Cycle base color palette.
* **Right-Click with Dye**: Instantly apply that dye color to the lamp.

### Using the Lamp Controller
* **Link Lamps in an Area**:
  1. Hold the Lamp Controller and right-click on the first corner lamp.
  2. Walk to the opposite diagonal corner and right-click the second lamp.
  3. All lamps within the bounding box are now linked into a synchronized group!
  4. *(Right-click empty air at any time to cancel selection).*
* **Open Settings GUI**:
  * Sneak + right-click any lamp with the controller to open the full configuration screen.
* **Bind to a Specific Jukebox**:
  1. Sneak + right-click a Jukebox with the controller to select it as the active audio source.
  2. Sneak + right-click a lamp or emitter to bind that group to this Jukebox.

---

## Performance & Compatibility

* **Low CPU/GPU Footprint**: Topology indexing is cached and optimized ($O(n)$ count queries with zero per-tick array allocations).
* **Shader & Rendering Safe**: Custom Block Entity Renderer with `NO_OVERLAY` neutrality (fully compatible with Vanilla rendering, Iris, and Sodium).
* **Client & Server Separated**: Client-only audio FFT decoding keeps dedicated servers 100% lightweight and lag-free.

---

## Requirements

* **Minecraft**: `1.21.1`
* **Fabric Loader**: `>= 0.19.3`
* **Fabric API**: `>= 0.116.15+1.21.1`
* **Java**: `21`

---

## License

This project is licensed under the [MIT License](LICENSE).
