<h1 align="center">PojavLauncher</h1>

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

*[Boardwalk](https://github.com/zhuowei/Boardwalk)-এর ধ্বংসাবশেষ থেকে PojavLauncher-এর উদ্ভব!*

PojavLauncher হল একটি লঞ্চার যা আপনাকে আপনার Android এবং [iOS](https://github.com/PojavLauncherTeam/PojavLauncher_iOS) ডিভাইসে Minecraft: Java Edition খেলতে দেয়।

বিস্তারিত তথ্যের জন্য আমাদের [উইকি](https://pojavlauncher.app/) দেখুন!

> [!NOTE]
> এটি আসল PojavLauncher নয়। মূল প্রকল্পটি বন্ধ হয়ে গেছে। এই রিপোজিটরিটি [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher)  কমিটগুলোর উপর ভিত্তি করে একটি রিরাইট। আমরা PojavLauncherTeam এর সাথে কোনোভাবে সম্পর্কিত নই।
>
> আমরা Discord ছাড়া অন্য কোনো সোশ্যাল মিডিয়া পরিচালনা করি না। PojavLauncherTeam-এর সব অফিসিয়াল সোশ্যাল চ্যানেল আর্কাইভ করা হয়েছে।
>
> - সর্বশেষ আপডেট ও খবরের জন্য [Discord](https://discord.gg/2HYpzs4gZT) যোগ দিন।
> - মূল PojavLauncher সোর্স কোড দেখতে [এখানে](https://github.com/PojavLauncherTeam/PojavLauncher) যান।

## বিষয়সূচি

* [পরিচিতি](#পরিচিতি)
* [PojavLauncher পেতে](#pojavlauncher-পেতে)
* [বিল্ড করা](#বিল্ড-করা)
    * [দ্রুত বিল্ড (সুপারিশকৃত)](#দ্রুত-বিল্ড-সুপারিশকৃত)
    * [বিস্তারিত বিল্ড](#বিস্তারিত-বিল্ড)
* [বর্তমান অবস্থা](#বর্তমান-অবস্থা)
* [পরিচিত সমস্যা](#পরিচিত-সমস্যা)
* [প্রায়শই জিজ্ঞাসিত প্রশ্ন](#প্রায়শই-জিজ্ঞাসিত-প্রশ্ন)
* [অবদান](#অবদান)
* [সহায়তা](#সহায়তা)
* [লাইসেন্স](#লাইসেন্স)
* [ক্রেডিট এবং নির্ভরতা](#ক্রেডিট--তৃতীয়-পক্ষের-উপাদান-এবং-তাদের-লাইসেন্স)
    * [মূল উপাদান](#মূল-উপাদান)
    * [ফ্রেমওয়ার্ক এবং লাইব্রেরি সাপোর্ট](#ফ্রেমওয়ার্ক-এবং-লাইব্রেরি-সাপোর্ট)
    * [গ্রাফিক্স এবং রেন্ডারিং](#গ্রাফিক্স-এবং-রেন্ডারিং)
    * [Java এবং গেম লাইব্রেরি](#java-এবং-গেম-লাইব্রেরি)
    * [নিরাপত্তা এবং সিস্টেম](#নিরাপত্তা-এবং-সিস্টেম)
    * [অডিও](#অডিও)
    * [অন্যান্য সেবা](#অন্যান্য-সেবা)
* [রোডম্যাপ](#রোডম্যাপ)

## পরিচিতি

* PojavLauncher হল Android এবং iOS-এর জন্য একটি Minecraft: Java Edition লঞ্চার যা [Boardwalk](https://github.com/zhuowei/Boardwalk)-এর উপর ভিত্তি করে তৈরি।
* এই লঞ্চার প্রায় সব Minecraft সংস্করণ চালাতে পারে, rd-132211 থেকে শুরু করে 26.x স্ন্যাপশট পর্যন্ত (Combat Test সংস্করণ সহ)।
* Forge এবং Fabric ব্যবহার করে মডিং সমর্থিত।
* এই রেপোজিটরিতে Android-এর সোর্স কোড রয়েছে। iOS/iPadOS-এর জন্য দেখুন [PojavLauncher_iOS](https://github.com/PojavLauncherTeam/PojavLauncher_iOS)।

## PojavLauncher পেতে

PojavLauncher তিনটি উপায়ে পেতে পারেন:

1. **রিলিজেস:** সর্বশেষ প্রি-বিল্ট অ্যাপ [nightly.link](https://nightly.link/TeamPojavLauncher/PojavLauncher/workflows/android/v3_openjdk?preview) থেকে ডাউনলোড করুন অথবা আমাদের [স্থিতিশীল রিলিজেস](https://github.com/TeamPojavLauncher/PojavLauncher/releases) বা [স্বয়ংক্রিয় বিল্ডস](https://github.com/TeamPojavLauncher/PojavLauncher/actions) থেকে নির্বাচন করুন।
2. **Google Play:** এই ব্যাজে ক্লিক করে Google Play থেকে পান: [![Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=net.kdt.pojavlaunch)
3. **সোর্স থেকে বিল্ড করুন:** নীচের [বিল্ড নির্দেশিকা](#বিল্ড-করা) অনুসরণ করুন।

## বিল্ড করা

### দ্রুত বিল্ড (সুপারিশকৃত)

PojavLauncher বিল্ড করার সবচেয়ে সহজ উপায় হল আমাদের CI দ্বারা প্রদত্ত প্রি-বিল্ট JRE ব্যবহার করা।

1. রেপোজিটরি ক্লোন করুন: `git clone https://github.com/TeamPojavLauncher/PojavLauncher.git`
2. লঞ্চার বিল্ড করুন: `./gradlew :app_pojavlauncher:assembleDebug` (Windows-এ `gradlew.bat` ব্যবহার করুন)

বিল্ড করা APK `app_pojavlauncher/build/outputs/apk/debug/`-এ পাওয়া যাবে।

### বিস্তারিত বিল্ড

যদি আপনি বিল্ড প্রক্রিয়ার উপর আরও নিয়ন্ত্রণ চান, এই ধাপগুলি অনুসরণ করুন:

1. **Java Runtime Environment (JRE):** আমাদের [CI অটো বিল্ডস](https://github.com/TeamPojavLauncher/android-openjdk-build-multiarch/actions) থেকে `jre8-pojav` আর্টিফ্যাক্ট ডাউনলোড করুন। এই প্যাকেজে সমস্ত সমর্থিত আর্কিটেকচারের প্রি-বিল্ট JRE রয়েছে। নিজেই JRE বিল্ড করতে চাইলে [android-openjdk-build-multiarch](https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch) রেপোজিটরির নির্দেশিকা অনুসরণ করুন।
2. **LWJGL:** কাস্টম LWJGL-এর বিল্ড নির্দেশিকা পাওয়া যাবে [LWJGL রেপোজিটরি](https://github.com/PojavLauncherTeam/lwjgl3)-এ।
3. **ভাষার তালিকা:** Crowdin দ্বারা ভাষা স্বয়ংক্রিয়ভাবে যোগ হওয়ায়, বিল্ডের আগে ভাষার তালিকা জেনারেটর চালাতে হবে। প্রজেক্ট ডিরেক্টরিতে চালান:
   * Linux/macOS:
     ```bash
     chmod +x scripts/languagelist_updater.sh
     bash scripts/languagelist_updater.sh
     ```
   * Windows:
     ```batch
     scripts\languagelist_updater.bat
     ```
4. **GLFW স্টাব বিল্ড করুন:** `./gradlew :jre_lwjgl3glfw:build`
5. **লঞ্চার বিল্ড করুন:** `./gradlew :app_pojavlauncher:assembleDebug` (Windows-এ `gradlew.bat` ব্যবহার করুন)।

## বর্তমান অবস্থা

* [x] OpenJDK 8 মোবাইল পোর্ট: ARM32, ARM64, x86, x86_64
* [x] OpenJDK 17 মোবাইল পোর্ট: ARM32, ARM64, x86, x86_64
* [x] OpenJDK 21 মোবাইল পোর্ট: ARM32, ARM64, x86, x86_64
* [x] হেডলেস মড ইনস্টলার
* [x] GUI সহ মড ইনস্টলার
* [x] OpenGL OpenJDK পরিবেশে
* [x] OpenAL (অধিকাংশ ডিভাইসে কাজ করে)
* [x] Minecraft 1.12.2 এবং তার নিচের সমর্থন
* [x] Minecraft 1.13 এবং তার উপরের সমর্থন
* [x] Minecraft 1.17 (22w13a) এবং তার উপরের সমর্থন
* [x] গেম সারফেস জুমিং
* [x] নতুন ইনপুট পাইপ নেটিভ কোডে পুনঃলিখন
* [x] সম্পূর্ণ কন্ট্রোল সিস্টেম পুনঃলিখন
* [x] MobileGlues Renderer যোগ করা হয়েছে
* [x] NG_GL4ES (Krypton Wrapper) Renderer যোগ করা হয়েছে
* [x] প্রোফাইল-ভিত্তিক ইনস্ট্যান্স সিস্টেম
* [x] আউট-অফ-দ্য-বক্স 1.21.5 সমর্থন
* [ ] LTW: Create-এর সমস্যা সমাধান
* [ ] LTW: Compute Shader/ইমেজ এক্সটেনশন সক্রিয় করা
* [ ] LTW: ফ্রেমবাফারের জন্য কালার-রেন্ডারেবল ফরম্যাটে পরিবর্তন
* [ ] Modpack/Mod ম্যানেজমেন্ট টুল
* [x] mrpack/CurseForge zip ইমপোর্ট
* [ ] MMC-সামঞ্জস্যপূর্ণ ইনস্ট্যান্স ইমপোর্ট
* [ ] মড নেটিভ লাইব্রেরির জন্য Patch-on-dlopen
* [ ] আরও আসছে!

## পরিচিত সমস্যা

পরিচিত সমস্যা এবং বর্তমান অবস্থা দেখতে আমাদের [ইস্যু ট্র্যাকার](https://github.com/TeamPojavLauncher/PojavLauncher/issues) দেখুন।

## প্রায়শই জিজ্ঞাসিত প্রশ্ন

বিস্তারিত তথ্যের জন্য আমাদের [উইকি](https://pojavlauncherteam.github.io/) দেখুন।

## অবদান

অবদান স্বাগত! আমরা কেবল কোড নয়, যেকোন ধরনের অবদানকে স্বাগত জানাই। উদাহরণস্বরূপ, আপনি [উইকি](https://pojavlauncherteam.github.io/) উন্নত করতে সাহায্য করতে পারেন, [অনুবাদে](https://crowdin.com/project/pojavlauncher) অবদান রাখতে পারেন, অথবা বাগ রিপোর্ট ও ফিচার রিকোয়েস্ট পাঠাতে পারেন।

যেকোনো কোড পরিবর্তন একটি পুল রিকোয়েস্ট হিসেবে জমা দিন। বর্ণনায় কোডের কাজ এবং এটি চালানোর ধাপ ব্যাখ্যা করতে হবে।

## সহায়তা

সহায়তার জন্য আমাদের [Discord সার্ভারে](https://discord.com/invite/aenk3EUvER) যোগ দিন।

## লাইসেন্স

PojavLauncher [GNU LGPLv3](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE) লাইসেন্সের অধীনে লাইসেন্সকৃত।

## ক্রেডিট এবং তৃতীয় পক্ষের উপাদান এবং তাদের লাইসেন্স

### মূল উপাদান
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 লাইসেন্স](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM লঞ্চার): অজানা লাইসেন্স / [Apache লাইসেন্স 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) অথবা [GNU GPLv2](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE)
- [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher): [GNU LGPLv3 লাইসেন্স](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE)

### ফ্রেমওয়ার্ক এবং লাইব্রেরি সমর্থন
- Android সাপোর্ট লাইব্রেরি: [Apache লাইসেন্স 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 লাইসেন্স](https://openjdk.java.net/legal/gplv2+ce.html)

### গ্রাফিক্স এবং রেন্ডারিং
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [MIT লাইসেন্স](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)
- [MobileGlues](https://github.com/MobileGL-Dev/MobileGlues): [LGPL-2.1 লাইসেন্স](https://github.com/MobileGL-Dev/MobileGlues/blob/dev-es/LICENSE)
- [Krypton Wrapper](https://github.com/BZLZHH/NG-GL4ES): [MIT লাইসেন্স](https://github.com/BZLZHH/NG-GL4ES/blob/main/LICENSE)
- [Mesa 3D গ্রাফিক্স লাইব্রেরি](https://gitlab.freedesktop.org/mesa/mesa): [MIT লাইসেন্স](https://docs.mesa3d.org/license.html)

### Java এবং গেম লাইব্রেরি
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [BSD-3 লাইসেন্স](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
- [GLFW](https://github.com/MojoLauncher/glfw): [জেডলিব (zlib) লাইসেন্স](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): ৩-ক্লজ BSD লাইসেন্স

### নিরাপত্তা এবং সিস্টেম
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java স্যান্ডবক্সিং সিকিউরিটি ম্যানেজার): [Apache লাইসেন্স 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)
- [bhook](https://github.com/bytedance/bhook) (এক্সিট কোড ট্র্যাপিং): [MIT লাইসেন্স](https://github.com/bytedance/bhook/blob/main/LICENSE)
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (ely.by এর মাধ্যমে অনুমোদন): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)

### অডিও
- [OpenAL-Soft](https://github.com/kcat/openal-soft): [GNU GPLv2 লাইসেন্স](app_pojavlauncher/src/main/assets/licenses/OPENAL-SOFT_GPL2)
- [oboe](https://github.com/google/oboe): [Apache লাইসেন্স 2.0](https://github.com/google/oboe/blob/main/LICENSE)

### অন্যান্য সেবা
- [Mineskin](https://mineskin.eu/) এবং [MCHeads](https://mc-heads.net) কে Minecraft অবতার প্রদান করার জন্য ধন্যবাদ

## রোডম্যাপ

আমরা বর্তমানে যে বিষয়গুলিতে মনোযোগ দিচ্ছি:  
* নতুন রেন্ডারিং প্রযুক্তি অন্বেষণ করা।  

ভবিষ্যতের পরিকল্পনায় রয়েছে:  
* স্থিতিশীলতা এবং পারফরম্যান্স উন্নত করা।  
* মড ইনস্টলেশনের অভিজ্ঞতা উন্নত করা।  

আমরা আমাদের রোডম্যাপের জন্য সম্প্রদায়ের মতামত এবং পরামর্শকে স্বাগত জানাই।  
আমাদের [ইস্যু ট্র্যাকার](https://github.com/PojavLauncherTeam/PojavLauncher/issues)-এ কোনো ফিচার রিকোয়েস্ট খুলতে দ্বিধা করবেন না।
