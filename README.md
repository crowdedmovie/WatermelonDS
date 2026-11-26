# WatermelonDS Fork

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin&logoColor=white)
![C++17](https://img.shields.io/badge/C%2B%2B-17-00599C?logo=cplusplus&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-GPL--3.0-blue)

A personal fork of [SapphireRhodonite/WatermelonDS](https://github.com/SapphireRhodonite/WatermelonDS) (built on top of [melonDS](https://melonds.kuribo64.net/) and the [melonDS Android port](https://github.com/rafaelvcaetano/melonDS-android) by rafaelvcaetano).

WatermelonDS adds RetroAchievements (including offline play), a Vulkan renderer, RetroArch shader presets, local multiplayer, and external display support. This fork tracks upstream while adding quality-of-life enhancements and builds side-by-side (`WatermelonDS Fork`, package `me.magnum.melondualds.nightly`) so you can keep it installed alongside the official release.

[<img src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" alt="Get it on GitHub" height="80">](https://github.com/crowdedmovie/WatermelonDS/releases)

<p align="center">
   <img width="450" height="400" alt="WaterMelon1" src="https://github.com/user-attachments/assets/187ea254-877e-4efd-a8dd-93a6023684ad" />
   <img width="450" height="400" alt="WaterMelon1" src="https://github.com/user-attachments/assets/01e82877-a138-44a3-bc71-c24d48cabab6" />
   <img width="450" height="400" alt="WaterMelon1" src="https://github.com/user-attachments/assets/27868058-ce89-470d-afac-b812dea5d1de" />
</p>


# What this fork adds
Built on top of upstream WatermelonDS (`0.8.0.rc4`):

### Normal-speed DS audio during fast-forward (optional, off by default)
*  Adds **Keep audio at normal speed during fast-forward** in **Settings › Audio** (`audio_keep_normal_speed_on_fast_forward`), disabled by default so upstream behavior is preserved unless enabled.
*  Scales Nintendo DS ARM7 hardware sound timers (`tid >= 4`) and SPU mixing/capture/microphone/blip-buf cycles in [melonDS-android-lib](https://github.com/crowdedmovie/melonDS-android-lib/tree/normal-speed-ff-audio) proportionally to measured emulation speed with adaptive SPU buffer feedback, keeping DS music near normal 1× tempo and pitch while gameplay and graphics stay accelerated.
*  Leaves DSi mode at the standard 1.0× clock, preserves binary savestate compatibility, and defers to **Mute on fast-forward** when enabled (without overwriting your saved preference).
*  *Limitation:* because ARM7 sound timers run slower relative to ARM9 during fast-forward, DS titles with tight ARM7-to-ARM9 timing sync or tight streamed audio may experience quirks, which is why the toggle defaults to off.

### In-app language selector & Android per-app locales
*  Adds **App language** in **Settings › General** (`app_language`) alongside Android 13+ **App languages** system support (`localeConfig` + `AppCompatDelegate.setApplicationLocales`).
*  Supports switching cleanly between **System default** and any of the 8 bundled locales (`en`, `es`, `fr`, `id`, `it`, `pt-BR`, `ru`, `zh-Hans`) to avoid mixed translations on partially localized languages.
*  Locked while launched in-game, handles unmatched system regional overrides cleanly, and adds `locale|layoutDirection` configuration handling so active Emulator, Phantom multiplayer, and Layout Editor sessions are not disrupted by external locale changes.

### Side-by-side install & fork update checks
*  Published as `gitHubNightlyRelease` (**WatermelonDS Fork**, `me.magnum.melondualds.nightly`), installing alongside official WatermelonDS (`me.magnum.melondualds`) without overwriting your existing setup.
*  In-app GitHub update checks target [crowdedmovie/WatermelonDS/releases](https://github.com/crowdedmovie/WatermelonDS/releases) rather than upstream.


# What WatermelonDS adds
Everything from the upstream Android port, plus:

### Local Multiplayer:
* Local DS multiplayer over Wi-Fi Direct, including Download Play, PictoChat, and cross-play with SeedlessDS. [Setup guide](https://github.com/SapphireRhodonite/WatermelonDS/releases/tag/0.8.0.rc4#set-up-your-first-multiplayer-room)

### RetroAchievements
*  Achievements, leaderboards and Hardcore mode, with a shared card style for the in-game popups
*  Your profile and score are shown on load and in the settings screen
*  **Offline play**: unlocks earned without a connection are stored and submitted later. Two providers are available
   the one built into WatermelonDS, or [RAOfflineProxy](https://github.com/SapphireRhodonite) if you prefer to run it
   yourself. Note that RAOfflineProxy does not support Hardcore mode
*  Achievements can be disabled without logging out, and sessions start even for games that have no achievement set

### Renderers
*  Software, OpenGL, Vulkan and compute renderers, with internal resolution scaling
*  Vulkan gained a *fastpath* presentation profile, optimized texture uploads and dynamic scissor clipping
*  **RetroArch shader presets** on every renderer, through [librashader](https://github.com/SnowflakePowered/librashader)
*  Adrenotools driver support on the `gitHub` flavor, for custom Adreno drivers

### External displays
*  Play on a second screen (TV, dock, devices such as the Odin 2), with the touch screen still usable on the handheld
*  Per-ROM external screen and layout settings, screen rotation, backgrounds, video filtering and aspect ratio control
*  Achievements are shown on the external display while you browse your ROM list

### Library and layouts
*  DSiWare titles in the ROM list, box art on the "continue" shelf, and custom ROM names
*  Layout editor with screen transparency, aspect ratio options and separate centering controls
*  Settings backup and restore (requires a restart to apply), with internal and external layouts backed up separately
*  Alternate input bindings and a hold-to-fast-forward button

The core is kept in sync with melonDS 1.0 and later upstream changes.

# Missing Features
*  DSi SD card support
*  Customizable button skins

# Performance
Performance is solid on 64 bit devices with thread rendering and JIT enabled, and should run at full speed on flagship
devices. Performance on older devices, specially 32 bit devices, is very poor due to the lack of JIT support.

# Integration with third-party frontends
It's possible to launch WatermelonDS from third party frontends. For that, you will need to have the ROMs you want to
launch already scanned by WatermelonDS. Then, you can configure your third-party frontend with the following
configuration:
*  Package name: `me.magnum.melondualds` (nightly builds use `me.magnum.melondualds.nightly`)
*  Activity name: `me.magnum.melonds.ui.emulator.EmulatorActivity`
*  Parameters (choose one):
    * Intent data (preferred) - a URI of the NDS ROM (ZIP and 7z files are supported). Ensure [read permission is granted](https://developer.android.com/reference/android/content/Intent#FLAG_GRANT_READ_URI_PERMISSION)
    * `uri` (deprecated) - a string with the [SAF](https://developer.android.com/guide/topics/providers/create-document-provider) URI of the NDS ROM (ZIP and 7z files are supported)
    * `PATH` (deprecated) - a string with the absolute path to the NDS ROM (ZIP and 7z files are supported)


### Info regarding save files
When launching ROMs from third-party frontends, if WatermelonDS hasn't scanned that particular ROM previously, it won't
be able to create the save file next to the ROM file if the option "Save next to ROM file" is enabled in the settings or
the save file directory is not set. Instead, WatermelonDS will create a save file in
`Android/data/me.magnum.melondualds/files/saves`

# Releases

Builds for this fork are published [here](https://github.com/crowdedmovie/WatermelonDS/releases) as `WatermelonDS Fork` (`me.magnum.melondualds.nightly`), allowing side-by-side installation next to official WatermelonDS builds.

# Building
To build the project you will need the Android SDK, NDK, CMake, a JDK 21 toolchain and a Rust toolchain
(`cargo` and `rustup`), which is used to build librashader for each ABI.

## Build steps:
1.  Clone the project, including submodules with:
    
    `git clone --recurse-submodules https://github.com/crowdedmovie/WatermelonDS.git`
2.  Install the Android SDK, NDK (`28.0.13004108`) and CMake
3.  Install Rust with [rustup](https://rustup.rs/). If Android Studio does not inherit your shell `PATH`, set the
    `CARGO` and `RUSTUP` environment variables to the executable paths
4.  Build with:
    1.  Unix: `./gradlew :app:assembleGitHubProdDebug`
    2.  Windows: `gradlew.bat :app:assembleGitHubProdDebug`
5.  The generated APK can be found at `app/build/outputs/apk/gitHubProd/debug`

There are two flavor dimensions, so build tasks combine both: `gitHub` or `playStore` (the former enables adrenotools)
and `prod` or `nightly`. For example, `:app:assembleGitHubNightlyDebug` or `:app:assembleGitHubProdRelease`.

If you want to create a release build, you will need to modify your `local.properties` file to include the following fields:  
*  `MELONDS_KEYSTORE=<path_to_your_keystore>`
*  `MELONDS_KEYSTORE_PASSWORD=<keystore_password>`
*  `MELONDS_KEY_ALIAS=<name_of_your_key_alias>`
*  `MELONDS_KEY_PASSWORD=<key_alias_password>`

# Credits
*  [WatermelonDS](https://github.com/SapphireRhodonite/WatermelonDS) by SapphireRhodonite — upstream fork base
*  [melonDS](https://github.com/melonDS-emu/melonDS) by Arisotura and contributors — the emulator core
*  [melonDS Android](https://github.com/rafaelvcaetano/melonDS-android) by rafaelvcaetano
*  [librashader](https://github.com/SnowflakePowered/librashader) — RetroArch shader preset support
*  [rcheevos](https://github.com/RetroAchievements/rcheevos) — RetroAchievements support
