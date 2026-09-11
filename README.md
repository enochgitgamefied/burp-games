# Burp Games 6.0.0

One Burp **Games** tab with four games: Sonic 3D, Asteroids, Breakout and Turbo Tails. Select a game from its gallery card; **All games** returns to the library. **Mute audio** silences music and effects across the library and remembers the choice after restart.

**Turbo Tails is now a native 3D kart racer built from the working Sonic game's renderer, character rig and movement system.** This replaces the previous HTML/JavaFX kart implementation. All four games live in the same JAR.

## Install

1. Remove/unload the previous Games extension in Burp's Extensions → Installed list.
2. Add `dist/burp-games.jar` as a Java extension. The release copy is named `burp-games-6.0.0.jar`.
3. Open the **Games** tab and select **Turbo Tails**.

Java 17 or later with desktop/audio APIs is required. Burp supplies its Montoya extension API. There is no browser setup, native runtime extraction, Node process, game server or game asset download. An unavailable audio device leaves the games playable silently.

For a standalone preview using a local JDK:

```sh
java -jar dist/burp-games.jar
```

Existing mute, Sonic quiz preferences, Sonic records and Turbo Tails circuit unlocks are preserved. Kart driver choices, player count and new circuit records are saved locally. A new installation starts with Emerald Circuit unlocked.

## Turbo Tails

Choose **Sonic, Tails, Knuckles or Amy**, each in a colour-matched kart. Tails has twin tails and quick steering, Knuckles has red locks and stronger contact recovery, Amy has pink quills and faster drift recharge, and Sonic has the highest straight-line speed. The models are native procedural fan interpretations derived from the existing Sonic model.

- Four racers per race: one human and three computer opponents, or two local players and two computer opponents.
- Five closed circuits: Emerald Circuit, Sunset Mesa, Polar Pass, Neon Marina and Cloud Garden. Each has physical hills and banked corners, with its own palette and scenery.
- Two laps, a three-second countdown, live position, lap time, coins, speed and boost meter. Solo mode includes a circuit minimap.
- Continuous driving. Hold accelerate to reach full speed. Boost consumes energy; coins and drifting replenish it. Cyan road pads grant a short speed burst.
- Karts collide, road edges slow the player and barriers keep the kart on the course. There is no automatic obstacle jump.
- Results show all four finish times. Finish in the top two in solo mode to unlock the next circuit. Solo records are stored separately from the older game's records.
- Two-player mode uses top/bottom split-screen and waits for both players to finish. Multiplayer does not change solo records or unlocks.
- No quiz interrupts kart racing. Sonic 3D retains its optional, remembered cybersecurity quiz setting.

| Action | Player 1 | Player 2 |
| --- | --- | --- |
| Steer | A / D | Left / Right |
| Accelerate / brake | W / S | Up / Down |
| Boost | Shift | Enter |
| Drift | Space | Ctrl |

Solo mode also accepts arrow keys. **P / Esc** pauses or resumes, **Enter** resumes a paused race, **R** restarts and **M** toggles the shared mute preference. The mouse-accessible pause overlay offers resume, race again and driver selection. Keyboard controls work while the race canvas has focus; click the track to focus it. Switching games, leaving the tab or losing focus pauses the race and clears held inputs. Reopening keeps the race paused until resumed.

The garage remembers your driver, second player and player count after starting a race. The Player 2 button cycles among the remaining three drivers.

## Other games

- **Sonic 3D:** Fast running, boost, jumping and bike/car/surfboard pickups, four environments, difficulty and level progression. The entry screen offers quizzes or uninterrupted play and a checkbox to remember the choice. Jumping requires player input. Vehicles are acquired as pickups.
- **Asteroids:** Fixed-facing spacecraft, left/right/forward movement, weapons, enemy squadrons and waves.
- **Breakout:** Paddle-and-ball arcade game with three levels.

Each game displays its own controls. Shared audio and returning to the gallery work across all four games.

## Build and verify

The build uses a JDK 17+ and `curl` for the compile-only Montoya API on the first build. The dependency checksum is pinned. No JavaFX SDK, Maven installation or JavaScript tooling is needed.

```sh
sh build.sh test
sh test-performance.sh
sh test-desktop.sh
```

`build.sh test` builds the JAR and runs simulation, rendering, audio, gallery and process-restart preference checks. It saves PNGs under `screenshots/`. `test-performance.sh` benchmarks the packaged JAR in a headless JVM. `test-desktop.sh` opens temporary Swing test windows to check the packaged game, real timer, component input, mute sync, hide/reopen and extension-style unload/reload. It repeats with the desktop interop and JavaScript bridge modules excluded.

The scripts use POSIX shell syntax and colon-separated Java classpaths (macOS/Linux). On Windows, use a compatible shell or compile/run the same classes with the Windows `;` classpath separator. The game JAR itself uses platform-neutral Java desktop APIs.

## Implementation

`GamesPanel` owns the gallery and shared audio/preferences. `TurboTailsGame` provides the native garage, input and HUD. `KartRace` runs a fixed 120 Hz simulation, with a wall-time countdown. `KartTrack` samples closed, elevated circuits using the Sonic engine's track coordinates. `KartScene` renders solid geometry through `Renderer3D`; `HedgehogRig` supplies the articulated driver and vehicle meshes.

The software renderer uses a depth buffer and clipping, fixed object locations, thick 3D coins and cached camera transforms. Race rendering is capped at 900 pixels wide for solo and 800 per split-screen viewport, then scaled to the panel; menus and HUD use the full panel resolution. Opponent meshes use less detail with distance. Hidden games stop their timers, and unloading releases timers and audio resources.

This is a compact native arcade racer using the existing Sonic art style. See `dist/validation.txt` and `dist/performance-report.md` for measured checks and test scope. See `THIRD-PARTY-ASSETS.md` and `LICENSE` for source/asset information.

<img width="1512" height="950" alt="Screenshot 2026-09-11 at 12 50 21 PM" src="https://github.com/user-attachments/assets/1629c75a-2b29-4c2f-aeab-5991a8f74414" />
<img width="1512" height="919" alt="Screenshot 2026-09-11 at 12 50 30 PM" src="https://github.com/user-attachments/assets/70df1117-17a3-48a2-9dd5-d1b814fccc4e" />

<img width="1512" height="953" alt="Screenshot 2026-09-11 at 12 50 45 PM" src="https://github.com/user-attachments/assets/0b1cc17b-f2f3-4a8b-9483-eed13837c53c" />

<img width="1512" height="950" alt="Screenshot 2026-09-11 at 12 50 56 PM" src="https://github.com/user-attachments/assets/c387d0aa-4e0b-4c1e-a978-2895a21f420d" />





