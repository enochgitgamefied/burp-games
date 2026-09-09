# Assets and dependencies — 6.0.0

All character geometry, karts, ships, track geometry, scenery and gallery covers are built procedurally from the Java source. Music, engine sounds and effects are synthesized locally by `AudioEngine`. The JAR contains no downloaded game models, sprites, textures, commercial recordings or web fonts.

Sonic, Tails, Knuckles and Amy are Sega characters. These models are original procedural fan renditions based on this project's Sonic rig. This is an unofficial fan project.

The PortSwigger Montoya API 2026.7 is a compile-only dependency downloaded from Maven Central, verified by SHA-256 in `build.sh`, and provided by Burp at runtime. The API JAR is not bundled in the extension.

Version 6.0.0 uses standard Java desktop and audio APIs. It contains no JavaFX/OpenJFX, WebKit, embedded browser, JavaScript bridge or extracted native runtime. The earlier HTML Turbo Tails game and its third-party assets are no longer part of the deliverable.

Project source and original generated assets: see `LICENSE`.
