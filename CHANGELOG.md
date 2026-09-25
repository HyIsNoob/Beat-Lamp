# Changelog

All notable changes to the Beat Lamp mod are documented in this file.

## [1.2.1] - 2026-09-23

- **Vanilla-Style Stage Jukebox**: Stage Jukebox functions smoothly just like the vanilla Jukebox without requiring a GUI. Right-click with any music disc (vanilla or Music Disc Maker) to insert and play; right-click while playing to immediately eject the disc.
- **Smooth Audio Distance Attenuation**: Fixed an issue where music abruptly stopped ("pop") when walking ~64 blocks away and restarted from the beginning when returning. Playback now smoothly attenuates with distance via OpenAL 3D sound positioning without interruptions.

### Fixes and Improvements
- **Music Disc Maker Eject Bug**: Fixed an issue where custom discs from Music Disc Maker continued playing indefinitely if ejected after the chunk was unloaded and reloaded.
- **MDM Golden Jukebox Vanilla Disc Beat Support**: Fixed beat tracking when playing vanilla music discs inside Music Disc Maker's Golden Jukebox.
- **Vanilla Disc Distance Attenuation and Volume Mute Fix**: Corrected sound falloff calculations for vanilla discs and prevented sound from breaking when muting/unmuting the vanilla Jukebox volume slider.
- **Multiplayer Stage Sync**: Fixed an issue in multiplayer where other players could not see equipment working or jukebox music playing. Jukebox status now immediately syncs across all players on the server.
- **Extended Particle Distance**: Stage particles from Fountains, Fog Generators, and Beat Lamps now remain visible from far away (up to 512 blocks) instead of disappearing after a short distance.
- **Mute -> Unmute Beat Tracking Recovery**: Fixed an issue where muting the game via volume sliders (Master or Jukebox/Note Blocks) prevented Beat Lamp from resuming its beat reactions when the volume slider was unmuted or raised back up.
- **Multiplayer Jukebox Chunk Arrival Sync (MC-120780 Fix)**: Fixed a vanilla Minecraft multiplayer flaw where players loading a chunk after a disc started playing (e.g., teleporting from 1000 blocks away) heard no music and saw no light reactions. Added server-side block entity packet synchronization and client-side catch-up scanning.
- **Audio Playback Range**: Music no longer cuts out prematurely when walking away from the jukebox, respecting the maximum audio radius configuration.

---

## [1.2.0] - 2026-09-19

### New Features
- **Rainbow LED Block**: Added a continuous ambient LED block that shines 24/7 without needing music. Includes 11 animated lighting modes (Rainbow Cycle, Breathing, Fire, Wave, Matrix, Police, and more), adjustable speed and brightness, and full Group Linker support.
- **Framed and Frameless Styles**: Rainbow LED blocks can quickly switch between a framed metallic casing and a seamless frameless glow (perfect for massive display walls). Crouch and right-click to toggle anytime.
- **Music Disc Maker Support**: Stage lights, visualizers, and lasers now dance to custom songs from the Music Disc Maker mod across all supported versions.
- **Stage Isolation for Custom Discs**: Custom music discs now respect Jukebox Binding. Fixtures bound to a specific jukebox will only react to music played in that jukebox, preventing sound bleed across different stages.

### Fixes and Improvements
- **Settings Screen Crash**: Fixed a game crash when pressing the 'O' settings key on NeoForge 1.21.7.
- **Fabric 1.20.1 Startup Crash**: Fixed a crash on game launch when running alongside Music Disc Maker on Fabric 1.20.1.
- **DMX Console Mute Fix**: Muting a group in the DMX Console no longer resets or erases your fixture's sensitivity settings.
- **DMX Master Dimmer**: The Master Dimmer on the DMX Console now smoothly adjusts brightness for all lights, lasers, and Rainbow LED blocks.
- **Stage Jukebox Disc Swapping**: Stage Jukeboxes now immediately start playing when inserting or removing custom music discs.
- **Full Translations**: Added complete language support for English, Korean, and Vietnamese for all new blocks, tooltips, and menus.

---

## [1.1.0] - 2026-09-07

### Performance and Settings
- **Client Settings Menu (Key O)**: Press 'O' in-game to adjust graphics settings, beam quality, particle density, and render distance.
- **Higher Framerates**: Optimized light rendering to eliminate lag spikes and maintain high FPS during concerts.
- **Anti-Strobe Mode**: Added a safety option in settings that softens rapid flashes into gentle fades for light-sensitive players.
- **Better Jukebox Sync**: Pausing the game in singleplayer no longer causes lights to desync from the song.

### Gameplay and Tools
- **Box Group Linking**: The Group Linker tool now lets you select a 3D box around your stage to instantly link all lights inside at once.
- **Moving Head Tempo Mode**: Added a Tempo Pulse toggle to Stage Lights for smoother beat tracking.
- **Korean Translation**: Added complete native Korean language support.

---

## [1.0.0] - Initial Release

- Initial release for Minecraft 1.20.1, 1.21.1, and 1.21.4 (Fabric, NeoForge, Forge).
- Beat Lamp visualizers with 9 responsive music modes and 16 colors.
- Moving-head Stage Lights with 3D volumetric beams.
- RGB Laser Projectors with customizable beam patterns.
- Stage Fog Generators and Music-Triggered Fountains.
- Beat Emitter for converting music beats into redstone power.
- Master DMX Console, Stage Jukebox, DJ Deck, and Stage Speakers.
- Group Linker and Lamp Controller tools.
