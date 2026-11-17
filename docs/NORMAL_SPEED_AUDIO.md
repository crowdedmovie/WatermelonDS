# Project Specification: Normal-Speed Audio During Fast-Forward

**Project:** WatermelonDS Android fork  
**Document status:** Proposed / not yet implemented  
**Last researched:** 2026-11-12  
**Primary test game:** Pokémon Black 2 (Nintendo DS)  
**Primary test device:** OnePlus Pad 3 (Android, Snapdragon 8 Elite / Adreno 830)  
**Development environment:** T3 Code with an interchangeable coding agent (e.g., Codex); no agent-specific dependency

> **One-sentence goal:** When Nintendo DS gameplay is fast-forwarded at 2× or 3×, keep in-game music at approximately its original **tempo and pitch**, with stable playback, while game simulation and graphics remain accelerated. The initial implementation should also attempt to keep sound effects at normal speed using the same DS sound-clock approach, subject to the limitations below.

## 1. Background and user experience

The primary motivation is to make Pokémon DS games more enjoyable to replay: navigating the world at 1× is acceptable, but battles, animations, and grinding are too slow without fast-forward. Existing fast-forward audio, even with pitch preservation, can sound warbly, metallic, crackly, or otherwise unnatural.

**Already solved / must preserve:** On the OnePlus Pad 3, WatermelonDS's **Vulkan renderer** runs Pokémon Black 2 without the observed 3D FPS drops, even at elevated internal resolution. This project is **not** about optimizing graphics, replacing Vulkan, or improving 2D sprite scaling.

**Existing alternatives are insufficient:**
- Normal fast-forward advances game audio too quickly, changing tempo and pitch.
- Pitch-preserving time stretching can maintain approximate pitch while still speeding up the musical tempo, and may introduce audible artifacts.
- The desired experience resembles a separate audio clock: **the game runs faster, but the soundtrack keeps normal tempo and pitch.**

## 2. Scope, behavior, and proposed defaults

### Required behavior (MVP)

1. Add an **optional** setting in WatermelonDS audio settings, provisionally named **“Keep audio at normal speed during fast-forward.”**
2. With the setting **on**, fast-forwarded DS gameplay targets normal real-time audio tempo and original pitch, not merely pitch-preserving faster playback. Aim to maintain natural music and sound effects without accumulating latency, pops, or robotic time stretching.
3. With the setting **off**, **preserve the current upstream fast-forward audio path**, including its existing pitch-preservation or mute options. Do not silently change existing behavior.
4. At ordinary **1×** game speed, behavior must remain equivalent to unmodified WatermelonDS, regardless of the setting.
5. Support the existing **hold** and **toggle** fast-forward controls. Restore normal clocks/audio when fast-forward stops; likewise handle pause, resume, reset, ROM change, app background/foreground, and audio-device changes safely.
6. Support target game speeds of **2× and 3×** first, with **5× as a stretch goal**. Scale from **actual measured emulation speed** where needed rather than assuming the requested multiplier is achieved exactly.
7. Keep all existing Vulkan rendering, internal resolution, screen layout, input, and frameskip options untouched and functioning.
8. Default the new setting **off initially** to minimize compatibility surprises. Once tested, the owner may change the default.
9. Restrict experimental DS audio-clock behavior to **DS-mode** initially; DSi-mode DSP behavior may need a separate design and must not be claimed as supported without tests.

### Audio behavior and known trade-offs

- **Normal-speed audio is not the same as pitch-preserving fast audio.** Music should take approximately the same wall-clock duration as it would at 1×. For example, a 30-second musical phrase should still take approximately 30 real-world seconds while world/battle updates are accelerated.
- Sound events triggered by the fast-forwarded game may happen closer together. Keeping each effect at 1× can cause overlapping effects or different relative timing; handle this as a compatibility/design issue rather than hiding it.
- A game waiting for a Pokémon cry, fanfare, or other sound to finish may **block at normal real-time duration**. Therefore, the entire battle may not accelerate uniformly. Measure and document this; do not promise perfect 3× acceleration of every scene.
- Slowing **ARM7 hardware timers** may affect more than music. Test game logic, script waits, and other timer-driven subsystems carefully. A narrow, isolated sound clock may eventually be preferable if broad timer scaling causes regressions.
- At speed above the physical display refresh rate, **not every emulated video frame can be shown**. That is separate from audio-clock behavior and not a goal of this project.

### Explicit non-goals

- Rewriting the DS emulator, adding DS recompilation, or reimplementing the sound engine from scratch.
- Creating a Vulkan backend, improving internal resolution, or altering graphics unless necessary to fix a proven regression.
- Guaranteeing audio perfection for every commercial DS/DSi game.
- Changing or distributing copyrighted ROMs, BIOS images, music assets, or Pokémon game patches.
- Automatically patching Pokémon Black 2's gameplay scripts to bypass sound-completion waits.
- Adding a general-purpose music/SFX separation system unless research demonstrates it is necessary.

## 3. Verified upstream references (as of 2026-11-12)

**Android app:** https://github.com/SapphireRhodonite/WatermelonDS  
**Android native melonDS core:** https://github.com/SapphireRhodonite/melonDS-android-lib  
**Existing desktop proof of concept:** https://github.com/Ssncdevelop/melonDS  
**Patch to study:** https://github.com/Ssncdevelop/melonDS/blob/ci/keep-music-speed/keep-music-speed.patch  
**WatermelonDS 0.8.0 RC3 release:** https://github.com/SapphireRhodonite/WatermelonDS/releases/tag/0.8.0.rc3

At the time of writing:
- WatermelonDS supports Vulkan and internal resolution scaling; RC3 release notes report **fast-forward pitch preservation**. That is distinct from keeping the soundtrack at normal tempo.
- The parent Android repo contains a Git submodule at `melonDS-android-lib`; its `.gitmodules` points to `SapphireRhodonite/melonDS-android-lib`. Resolve the **actual pinned commit** when work begins (do not assume any moving branch reflects the app's build).
- The desktop fork's patch proposes adjusting ARM7 timer and SPU audio-clock rates as emulation speed changes. Its patch documents an Android-relevant limitation: waits for sounds to finish may remain real-time; **DSi DSP is not covered**.
- That patch contains **two commits**. The second introduces a game-visible “speed hint” at an unused DS address for ROM hacks. **Do not implement the speed-hint addition in the MVP without a separate, justified compatibility review**; prioritize porting the first commit's core audio behavior.
- The desktop implementation uses PC-specific Qt/SDL frontend code. **Do not copy desktop UI or callback code unmodified** into Android.

> These are research leads, not proof that the patch will work unchanged on WatermelonDS's pinned Android core. The implementation agent must inspect current code and verify APIs, scheduling, threading, and build constraints.

## 4. Architecture research checklist (required before code changes)

The agent must locate and document **actual file paths, symbols, and data flow in the pinned checkout**, including:

- [ ] Where fast-forward is enabled/disabled, how hold/toggle actions are applied, and where speed targets are enforced.
- [ ] Where actual emulation speed or per-frame timing is measured.
- [ ] Where ARM7 timers and the DS SPU advance in the current native core (likely near `src/NDS.cpp`, `src/NDS.h`, `src/SPU.cpp`, `src/SPU.h`, but verify paths and APIs).
- [ ] How the Android JNI/native bridge configures or communicates with the core.
- [ ] Where audio is produced, queued, resampled/time-stretched, and submitted to Android (including any Oboe or equivalent callback path).
- [ ] How RC3's pitch-preserving fast-forward and mute-during-fast-forward modes interact. Define mutually exclusive behavior, priority, or explicit mode selection; avoid double-processing.
- [ ] How app settings are stored, surfaced in UI, and scoped globally versus per-ROM.
- [ ] How sound clock state behaves through reset, ROM unload, pause, savestate save/load, and app lifecycle.
- [ ] Which threads access clock ratios or audio buffers and how data races will be prevented.
- [ ] Whether DS vs DSi DSP mode requires gating, and what tests or fallbacks are available.

**Deliverable before implementation:** A short architecture note in a PR/agent report with referenced file paths and a proposed minimal patch. Do not start a broad refactor without authorization.

## 5. Proposed implementation strategy

### Phase 0 — Reproducible baseline

1. Fork WatermelonDS on GitHub (owner action), then clone the **fork** recursively with all Git submodules.
2. Record the parent repository commit, core submodule SHA, Android Gradle/NDK/JDK versions, and build variant.
3. Build an **unmodified** debug APK before changing anything.
4. Back up the existing app's Pokémon Black 2 `.sav` and configuration; never test against the only copy of a save.
5. Confirm existing Vulkan/high-resolution behavior on the OnePlus Pad 3 and record baseline audio at 1×, 2× and 3× for comparison.
6. Give debug builds a distinct Android application ID/package name **only after checking manifests, content providers, permissions, file storage paths, and external frontend integrations**. Do not overwrite the working installation.

### Phase 1 — Native sound clock proof of concept

1. Study **commit 1** of `keep-music-speed.patch` and compare it to the **pinned WatermelonDS core version**.
2. Port/adapt only the **needed DS ARM7 timer/SPU clock-scaling mechanisms**. Evaluate any gameplay-side effects before adopting the design wholesale.
3. Expose a safe API through the Android core/native bridge, with a real-time multiplier/ratio input and a reset-to-1× path.
4. Use measured effective speed, bounded/clamped ratios, and appropriate buffering feedback **only if testing confirms it is needed**. Avoid oscillation when speed fluctuates.
5. Guard against invalid multipliers, divide-by-zero, timing discontinuities, stale state, and audio thread races.
6. On disable, restore 1× clocks and ensure buffers transition cleanly (no endless lag or abrupt bursts).
7. Gate experimental behavior to supported DS audio configurations; preserve original fallback path.

### Phase 2 — Android integration and UX

1. Add the audio setting as an explicit, persisted toggle. Proposed default: **off**.
2. Define how this setting interacts with **Mute during fast-forward** and existing pitch-preserving behavior. If mute is active, mute should win; if normal-speed audio is active, prevent a second time-stretch stage from re-speeding audio.
3. Keep hold/toggle fast-forward behavior unchanged.
4. Add limited, opt-in debug instrumentation for requested speed, measured speed, applied sound-clock ratio, buffer fill, underruns/overruns, audio latency, and state transitions.
5. Preserve saves, controls, graphics settings, layout and existing features.

### Phase 3 — Stability and review

1. Implement or adapt automated tests for ratio clamping, clock reset, state transitions, and configuration defaults where the architecture permits.
2. Test on device at 1×, 2×, 3×, and optionally 5×. Do not mark audio quality “verified” from an emulator-only unit test.
3. Compare output to baseline audio recordings and document subjective artifacts honestly.
4. Make a small PR or series of reviewable commits with a clear rollback strategy.
5. If modifying the native core, use a **separately versioned fork of `melonDS-android-lib`**, commit and push it, and update the app's submodule pointer. Never leave it referencing an unpushed local commit.

## 6. Acceptance criteria

The MVP is **complete only when all required tests pass on a physical Android device**:

- [ ] WatermelonDS fork builds successfully from a fresh checkout with submodules.
- [ ] The new setting exists, persists across relaunch, and is **off by default**.
- [ ] With the setting off, upstream fast-forward, pitch-preserving audio, and mute behavior remain intact (verified against baseline).
- [ ] At 1× with the setting on, audio and gameplay appear equivalent to unmodified WatermelonDS.
- [ ] At **2× and 3×**, measured game progress accelerates while continuous Pokémon Black 2 background music maintains **approximately 1× wall-clock tempo and original pitch**; test sustained playback for at least five minutes per mode.
- [ ] No clearly audible repeated pops, buzzing, metallic warble, buffer underruns, or **progressively accumulating delay** in the tested scenes.
- [ ] Enter/exit fast-forward repeatedly (at least 30 times) without stuck speed, silence, output bursts, crashes, or persistent delay.
- [ ] Game pause/resume, app background/foreground, ROM restart/change, and save/load do not leave audio at the wrong speed.
- [ ] **Vulkan and existing higher internal-resolution behavior remain unaffected** in the previously working Pokémon Black 2 title screen and gameplay.
- [ ] Pokémon Black 2 battles, menus, wild encounters, cries, fanfares, battle transitions, and exploration are tested; documented caveats identify any blocked waits.
- [ ] No uncommitted / unpushed native core submodule changes; LICENSE and attribution preserved.

**Measurement guidance:** Record 1× and 2×/3× audio clips of the same repeatable music section, compare elapsed music phrase duration and pitch by ear or analysis, and capture repeated 2×/3× transitions. Any tolerances chosen for automated comparisons must be documented; do not claim bit-perfect identity given real-time sound events and resampling.

### Regression matrix

| Scenario | 1× | 2× | 3× | Notes |
|---|:---:|:---:|:---:|---|
| Overworld looping music | ✓ | ✓ | ✓ | Tempo/pitch, transitions |
| Trainer battle and battle music | ✓ | ✓ | ✓ | Primary user scenario |
| Pokémon cries / move effects | ✓ | ✓ | ✓ | Overlap and waits |
| Victory jingle / fanfare | ✓ | ✓ | ✓ | Check blocking sound waits |
| Hold-to-FF / toggle-to-FF | ✓ | ✓ | ✓ | Rapid switching |
| Save / load and pause / resume | ✓ | ✓ | ✓ | Clock state reset |
| Vulkan at elevated internal resolution | ✓ | ✓ | ✓ | Preserve existing performance |
| Bluetooth/wired speaker route (optional) | ✓ | ✓ | ✓ | Device-dependent |

## 7. Suggested baseline build and repository workflow

Follow the repository's **current** README and Gradle configuration; at the time of writing, they specify Android SDK, NDK **28.0.13004108**, CMake, **JDK 21**, and **Rust (`rustup`/`cargo`)** for librashader. Re-check on the chosen commit.

```bash
# After creating your fork, clone YOUR fork URL (replace placeholder):
git clone --recurse-submodules https://github.com/<YOUR_GITHUB_USERNAME>/WatermelonDS.git
cd WatermelonDS

# Verify the actual pinned core and other submodule versions:
git submodule status

# Unix/macOS:
./gradlew :app:assembleGitHubProdDebug

# Windows alternative:
# gradlew.bat :app:assembleGitHubProdDebug
```

Expected default debug output, subject to build changes: `app/build/outputs/apk/gitHubProd/debug/`.

**Recommended branch name:** `feature/normal-speed-ff-audio` (name only; no branch/fork has been created). Do not commit APKs, proprietary ROMs, BIOS images, test save files, private keys, or device logs containing personal data. Avoid changing upstream branding without reviewing license and naming implications.

## 8. Risks, limitations, and decisions

| Risk | Why it matters | Mitigation |
|---|---|---|
| ARM7 timer scaling affects non-audio behavior | Timers serve other DS purposes | Audit timer consumers; test Pokémon scripts; gate feature and provide fallback |
| Music playing normally but cries blocking battle progress | Gameplay may wait for sound completion | Document as expected limitation; do not silently alter ROM logic |
| Effective speed fluctuates | Requested 3× may be actual 2.6×, producing tempo drift | Measure/clamp effective speed; smooth carefully; monitor output buffer |
| Multiple audio speed conversions | Normal-speed clock plus pitch/time stretch may conflict | Trace Android audio pipeline, explicitly bypass conflicting transformations |
| Audio callback threading | Race can produce crackle, buffer corruption or crashes | Use safe synchronization/atomics, avoid blocking audio callback |
| DSi sound path differs | Desktop patch excludes DSi DSP | DS-only MVP; separate future work |
| Savestate and lifecycle discontinuities | Audio clock may persist across reset/load | Reset/restore ratios and buffer states deliberately |
| Forked core submodule maintenance | Git parent can point at unavailable commits | Maintain/push core fork and pin its submodule SHA |
| Upstream evolves | Releases may alter audio code | Pin baseline commit; rebase only after tests |

### Product decisions / questions to revisit

1. **Music and effects:** Proposed MVP attempts to keep both music **and sound effects** at 1×, because this most directly follows the reference patch. If a separate music-only solution becomes feasible, evaluate it later, not as an assumed capability.
2. **Default:** Proposed setting is **off by default** for safety; user can opt in. Can change after validation.
3. **Fanfare/cry waits:** Prioritize accurate normal-speed sound over artificially ending effects; flag as an explicit trade-off.
4. **Per-game override:** Nice-to-have; not required for MVP. Global toggle first.
5. **Maximum speed:** 2× and 3× required, 5× optional pending stability.
6. **Beyond DS mode:** Separate milestone after DS success.

## 9. Future agent's first task

**Do not immediately apply the desktop patch.** First:

1. Read this spec and the repository-root `AGENTS.md`.
2. Inspect `.gitmodules`, the pinned native core SHA, the **current** Android audio pipeline, and the desktop patch.
3. Build the unmodified app, if the local toolchain is present.
4. Return: (a) architectural map with file paths, (b) smallest viable diff plan, (c) known incompatibilities, (d) testing strategy, and (e) any blocker that needs user approval.
5. Implement only after that review, with small commits and clear evidence.

## 10. Research links and license

- WatermelonDS Android: https://github.com/SapphireRhodonite/WatermelonDS
- WatermelonDS native core: https://github.com/SapphireRhodonite/melonDS-android-lib
- WatermelonDS `.gitmodules`: https://github.com/SapphireRhodonite/WatermelonDS/blob/Develop/.gitmodules
- WatermelonDS RC3 audio release notes: https://github.com/SapphireRhodonite/WatermelonDS/releases/tag/0.8.0.rc3
- Desktop normal-speed audio fork: https://github.com/Ssncdevelop/melonDS
- Desktop patch: https://github.com/Ssncdevelop/melonDS/blob/ci/keep-music-speed/keep-music-speed.patch
- WatermelonDS build instructions: https://github.com/SapphireRhodonite/WatermelonDS#building

Keep copyright notices, license files, and patch attribution. WatermelonDS and its melonDS core are GPL-licensed; confirm the obligations and other bundled dependencies before distributing modified binaries. This document is a project plan, not a claim that compatibility or normal-speed audio has already been achieved on Android.
