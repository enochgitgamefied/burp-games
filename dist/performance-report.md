# Native kart performance and verification — 6.0.0

Measured on macOS Apple silicon (Darwin arm64), Temurin OpenJDK 17.0.19+10. Tests use the real game view, four racers and the same software renderer used by the extension.

| Mode | Panel size | Median frame | 95th percentile |
| --- | --- | --- | --- |
| 1 player | 1280x750 | 21.17 ms | 24.51 ms |
| 2 player | 1280x850 | 22.05 ms | 26.68 ms |

Each sample includes simulation and a full UI paint, with 15 warm-up frames and 90 measured frames. Rendering buffers are capped at 900 pixels wide in solo and 800 per viewport in split-screen; HUD text remains at panel resolution. Measurements are local render costs, not a guarantee of FPS on every Burp host.

Real Swing-window tests passed for the packaged JAR with the ordinary JDK and with `--limit-modules java.se,jdk.unsupported`. The countdown completed near its intended three seconds; race simulation advanced approximately one second during a one-second wall-time interval while painting. The tests also exercised real component handlers for driver selection, acceleration, boost, pause, mute, focus loss, gallery switching and extension-style unload/reload.

All five circuits completed full simulated races with four actual finish times. Tests cover track seam continuity, hills/corners, fixed-step timing at different frame rates, independent two-player controls, collisions, high-speed coin pickup, pads, unlocks and records. Identical game states produce identical images; scenery locations are deterministic and coins use thick geometry instead of sprites.

Original Sonic, Asteroids, Breakout, audio and preference tests pass. Separate JVMs verified remembered no-quiz and mute choices plus kart roster, local player count, unlocks and new race records. Full output is in `validation.txt`.

The JAR contains only project Java classes, manifest and notices; no JavaFX, WebKit, HTML game, JavaScript bridge or native runtime. It was checked in a standalone native Swing window, not inside the user's running Burp process. Windows and Linux execution were not tested in this session.
