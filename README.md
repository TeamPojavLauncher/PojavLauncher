<h1 align="center">PojavLauncher</h1>

<a href="./README_RU.md">Readme на русском</a>

<a href="./README_BN.md">Readme বাংলা ভাষায়</a>

<img src="https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/assets/pojavlauncher.png" align="left" width="130" height="150" alt="PojavLauncher logo">

[![Android CI](https://github.com/TeamPojavLauncher/PojavLauncher/workflows/Android%20CI/badge.svg)](https://github.com/TeamPojavLauncher/PojavLauncher/actions)
<img src="https://img.shields.io/badge/platform-Android-green" alt="Platform"/>
<img src="https://img.shields.io/badge/minSdk-21-blue" alt="Min SDK"/>
[![Project Status](https://img.shields.io/badge/status-active-brightgreen.svg?style=flat)](https://github.com/TeamPojavLauncher/PojavLauncher/commits)
[![Monthly commit activity for PojavLauncher](https://img.shields.io/github/commit-activity/m/TeamPojavLauncher/PojavLauncher.svg?style=flat)](https://github.com/TeamPojavLauncher/PojavLauncher/graphs/commit-activity)
[![GitHub issues](https://img.shields.io/github/issues/TeamPojavLauncher/PojavLauncher.svg?style=flat&color=%23FFA500)](https://github.com/TeamPojavLauncher/PojavLauncher/issues)
[![Crowdin](https://badges.crowdin.net/pojavlauncher/localized.svg)](https://crowdin.com/project/pojavlauncher)
[![GitHub contributors](https://img.shields.io/github/contributors/TeamPojavLauncher/PojavLauncher.svg?style=flat)](https://github.com/TeamPojavLauncher/PojavLauncher/graphs/contributors)
<a href="https://discord.gg/2HYpzs4gZT"><img src="https://img.shields.io/discord/1355213558631366897?color=5865F2&logo=discord&logoColor=white&label=&style=flat" alt="Discord"></a>
<a href="https://github.com/TeamPojavLauncher"><img src="https://img.shields.io/badge/github-PojavLauncher-green?logo=github" alt="GitHub"></a>
[![Latest Release](https://img.shields.io/github/v/tag/TeamPojavLauncher/PojavLauncher?sort=semver)](https://github.com/TeamPojavLauncher/PojavLauncher/releases "View latest release")
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue)](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
[![Twitter Follow](https://img.shields.io/twitter/follow/PLaunchTeam.svg?style=social)](https://x.com/PLaunchTeam)

*From [Boardwalk](https://github.com/zhuowei/Boardwalk)'s ashes here comes PojavLauncher!*

PojavLauncher is a launcher that allows you to play Minecraft: Java Edition on your Android and [iOS](https://github.com/PojavLauncherTeam/PojavLauncher_iOS) devices.

For more details, check out our [wiki](https://pojavlauncher.app/)!

> [!NOTE]
> This is not the original PojavLauncher. The original project has been discontinued. This repository is a rewrite based on [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher) commits. We are not affiliated with the PojavLauncherTeam in any way.
>
> We do not maintain any social media accounts except Discord. All official PojavLauncherTeam social channels have been archived.
>
> - Join [Discord](https://discord.gg/2HYpzs4gZT) for the latest updates and news.
> - Check out the [original PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher) source code.

## Table of Contents

* [Introduction](#introduction)
* [Getting PojavLauncher](#getting-pojavlauncher)
* [Building](#building)
    * [Quick Build (Recommended)](#quick-build-recommended)
    * [Detailed Build](#detailed-build)
* [Current Status](#current-status)
* [Known Issues](#known-issues)
* [FAQ](#faq)
* [Contributing](#contributing)
* [Support](#support)
* [License](#license)
* [Credits & Dependencies](#credits--third-party-components-and-their-licenses)
    * [Core Components](#core-components)
    * [Framework & Library Support](#framework--library-support)
    * [Graphics & Rendering](#graphics--rendering)
    * [Java & Game Libraries](#java--game-libraries)
    * [Security & System](#security--system)
    * [Audio](#audio)
    * [Other Services](#other-services)
* [Roadmap](#roadmap)

## Introduction

* PojavLauncher is a Minecraft: Java Edition launcher for Android and iOS based on [Boardwalk](https://github.com/zhuowei/Boardwalk)
* This launcher can launch almost all available Minecraft versions ranging from rd-132211 to 26.x snapshots (including Combat Test versions)
* Modding via Forge and Fabric are also supported.
* This repository contains source code for Android. For iOS/iPadOS, check out [PojavLauncher_iOS](https://github.com/PojavLauncherTeam/PojavLauncher_iOS).

## Getting PojavLauncher

You can get PojavLauncher via three methods:

1. **Releases:** Download the latest prebuilt app from [nightly.link](https://nightly.link/TeamPojavLauncher/PojavLauncher/workflows/android/v3_openjdk?preview) or select from our [stable releases](https://github.com/TeamPojavLauncher/PojavLauncher/releases) or [automatic builds](https://github.com/TeamPojavLauncher/PojavLauncher/actions).
2. **Google Play:** Get it from Google Play by clicking on this badge: [![Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=net.kdt.pojavlaunch)
3. **Build from Source:** Follow the [building instructions](#building) below.

## Building

### Quick Build (Recommended)

The easiest way to build PojavLauncher is to use the pre-built JREs provided by our CI.

1. Clone the repository: `git clone https://github.com/TeamPojavLauncher/PojavLauncher.git`
2. Build the launcher: `./gradlew :app_pojavlauncher:assembleDebug` (Use `gradlew.bat` on Windows)

The built APK will be located in `app_pojavlauncher/build/outputs/apk/debug/`.

### Detailed Build

If you need more control over the build process, follow these steps:

1. **Java Runtime Environment (JRE):** Download the `jre8-pojav` artifact from our [CI auto builds](https://github.com/TeamPojavLauncher/android-openjdk-build-multiarch/actions).  This package contains pre-built JREs for all supported architectures.  If you need to build the JRE yourself, follow the instructions in the [android-openjdk-build-multiarch](https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch) repository.

2. **LWJGL:** The build instructions for the custom LWJGL are available over the [LWJGL repository](https://github.com/PojavLauncherTeam/lwjgl3).

3. **Language List:** Because languages are auto-added by Crowdin, you need to run the language list generator before building. In the project directory, run:
   * Linux/macOS:
     ```bash
     chmod +x scripts/languagelist_updater.sh
     bash scripts/languagelist_updater.sh
     ```
   * Windows:
     ```batch
     scripts\languagelist_updater.bat
     ```

4. **Build GLFW stub:** `./gradlew :jre_lwjgl3glfw:build`

5. **Build the launcher:** `./gradlew :app_pojavlauncher:assembleDebug` (Replace `gradlew` with `gradlew.bat` on Windows).

## Current Roadmap
- [x] OpenJDK 8 Mobile port: ARM32, ARM64, x86, x86_64
- [x] OpenJDK 17 Mobile port: ARM32, ARM64, x86, x86_64
- [x] OpenJDK 21 Mobile port: ARM32, ARM64, x86, x86_64
- [x] Headless mod installer
- [x] Mod installer with GUI
- [x] OpenGL in OpenJDK environment
- [x] OpenAL (works on most devices)
- [x] Support for Minecraft 1.12.2 and below
- [x] Support for Minecraft 1.13 and above
- [x] Support for Minecraft 1.17 (22w13a) and above
- [x] Game surface zooming
- [x] New input pipe rewritten to native code
- [x] Rewritten entire controls system
- [x] Added MobileGlues Renderer
- [x] Added NG_GL4ES (Krypton Wrapper) Renderer
- [x] Instance system in favor of profiles
- [x] Out-of-the-box 1.21.5 support
- [ ] LTW: resolve issues with Create
- [ ] LTW: enable compute shader/image extensions
- [ ] LTW: switch to a color-renderable format for framebuffers
- [ ] Modpack/mod management tool
- [x] mrpack/CurseForge zip import
- [ ] MMC-compatible instance import
- [ ] Patch-on-dlopen for mod native libraries
- [ ] More to come!

## Known Issues

See our [issue tracker](https://github.com/TeamPojavLauncher/PojavLauncher/issues) for a list of known issues and their current status.

## FAQ

See our [wiki](https://pojavlauncherteam.github.io/) for more information.

## Contributing

Contributions are welcome! We welcome any type of contribution, not only code. For example, you can help improve the [wiki](https://pojavlauncherteam.github.io/), contribute to the [translations](https://crowdin.com/project/pojavlauncher), or submit bug reports and feature requests.

Any code change should be submitted as a pull request. The description should explain what the code does and give steps to execute it.

## Support

For support, please join our [Discord server](https://discord.com/invite/aenk3EUvER).

## License

PojavLauncher is licensed under [GNU LGPLv3](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE).

## Credits & Third Party Components and Their Licenses

### Core Components
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Unknown License / [Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) or [GNU GPLv2](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE)
- [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher): [GNU LGPLv3 License](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE)

### Framework & Library Support
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html)

### Graphics & Rendering
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)
- [MobileGlues](https://github.com/MobileGL-Dev/MobileGlues): [LGPL-2.1 License](https://github.com/MobileGL-Dev/MobileGlues/blob/dev-es/LICENSE)
- [Krypton Wrapper](https://github.com/BZLZHH/NG-GL4ES): [MIT License](https://github.com/BZLZHH/NG-GL4ES/blob/main/LICENSE)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html)

### Java & Game Libraries
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
- [GLFW](https://github.com/MojoLauncher/glfw): [zlib license](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): 3-Clause BSD license

### Security & System
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java sandboxing security manager): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)
- [bhook](https://github.com/bytedance/bhook) (Exit code trapping): [MIT License](https://github.com/bytedance/bhook/blob/main/LICENSE)
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (Authorization via ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)

### Audio
- [OpenAL-Soft](https://github.com/kcat/openal-soft): [GNU GPLv2 License](app_pojavlauncher/src/main/assets/licenses/OPENAL-SOFT_GPL2)
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE)

### Other Services
- Thanks to [Mineskin](https://mineskin.eu/) and [MCHeads](https://mc-heads.net) for providing Minecraft avatars

## Roadmap

We are currently focusing on:

* Exploring new rendering technologies.

Future plans include:

* Improving stability and performance.
* Enhancing the mod installation experience.

We welcome community feedback and suggestions for our roadmap.  Please feel free to open a feature request in our [issue tracker](https://github.com/PojavLauncherTeam/PojavLauncher/issues).
