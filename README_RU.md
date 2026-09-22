<h1 align="center">PojavLauncher (также известный как Pojav Reborn)</h1>

<a href="./README.md">Readme in English</a>

<a href="./README_BN.md">Readme বাংলা ভাষায়</a>

<img src="https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/assets/pojavlauncher.png" align="left" width="130" height="150" alt="Логотип PojavLauncher">

[![Android CI](https://github.com/TeamPojavLauncher/PojavLauncher/workflows/Android%20CI/badge.svg)](https://github.com/TeamPojavLauncher/PojavLauncher/actions)
[![Github commit activity](https://img.shields.io/github/commit-activity/m/TeamPojavLauncher/PojavLauncher.svg?style=flat)](https://github.com/TeamPojavLauncher/PojavLauncher/graphs/commit-activity)
[![Crowdin](https://badges.crowdin.net/pojavlauncher/localized.svg)](https://crowdin.com/project/pojavlauncher)
[![Discord](https://img.shields.io/discord/1355213558631366897?color=5865F2&logo=discord&logoColor=white&label=&style=flat)](https://discord.gg/2HYpzs4gZT)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue)](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
[![Twitter Follow](https://img.shields.io/twitter/follow/PLaunchTeam.svg?style=social)](https://x.com/PLaunchTeam)

*Из пепла [Boardwalk](https://github.com/zhuowei/Boardwalk) рождается PojavLauncher!*

PojavLauncher — это лаунчер, позволяющий вам играть в Minecraft: Java Edition на вашем Android и [iOS](https://github.com/PojavLauncherTeam/PojavLauncher_iOS) устройстве.

Для получения дополнительной информации посетите наш [вики](https://pojavlauncher.app/)!

> [!IMPORTANT]
> Это не оригинальный PojavLauncher. Этот проект основан на [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher). Для получения дополнительной информации, пожалуйста, см. [DISCLAIMER.md](DISCLAIMER.md).

## Навигация

* [Введение](#введение)
* [Получение PojavLauncher](#получение-pojavlauncher)
* [Сборка](#сборка)
    * [Быстрая сборка (рекомендуется)](#быстрая-сборка-рекомендуется)
    * [Подробная сборка](#подробная-сборка)
* [Текущая дорожная карта](#текущая-дорожная-карта)
* [Известные проблемы](#известные-проблемы)
* [Часто задаваемые вопросы](#часто-задаваемые-вопросы)
* [Участие в разработке](#участие-в-разработке)
* [Поддержка](#поддержка)
* [Лицензия](#лицензия)
* [Благодарности и компоненты](#благодарности-и-компоненты)
* [Дорожная карта](#дорожная-карта)

## Введение

* PojavLauncher — это лаунчер Minecraft: Java Edition для Android и iOS основанный на [Boardwalk](https://github.com/zhuowei/Boardwalk)
* Этот лаунчер может запускать почти все доступные версии Minecraft в диапазоне от rd-132211 до снапшотов 26.x (включая версии Combat Test).
* Моддинг через Forge и Fabric так же поддерживается.
* Этот репозиторий содержит исходный код для Android. Для iOS/iPadOS посетите [PojavLauncher_iOS](https://github.com/PojavLauncherTeam/PojavLauncher_iOS).

## Получение PojavLauncher

Вы можете получить PojavLauncher пятью способами:

1. **Релизы:** Загрузите последний release.apk из наших [стабильных релизов](https://github.com/TeamPojavLauncher/PojavLauncher/releases).
2. **Google Play (устаревший):** Загрузите из Google Play, нажав на эту кнопку: [![Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=net.kdt.pojavlaunch)
3. **Nightly.link:** Получите последний готовый билд с [nightly.link](https://nightly.link/TeamPojavLauncher/PojavLauncher/workflows/android/v3_openjdk?preview).
4. **Автоматические сборки:** Загрузите последний debug.apk из [автоматических сборок](https://github.com/TeamPojavLauncher/PojavLauncher/actions).
5. **Сборка из исходного кода:** Следуйте [инструкциям по сборке](#сборка) ниже.

## Сборка

### Быстрая сборка (рекомендуется)

Самый простой способ собрать PojavLauncher — это использовать предварительно собранные JRE, предоставляемые нашей CI.

1. Клонируйте репозиторий: `git clone https://github.com/TeamPojavLauncher/PojavLauncher.git`
2. Соберите лаунчер: `./gradlew :app_pojavlauncher:assembleDebug` (Используйте `gradlew.bat` на Windows)

Собранный APK будет находиться в `app_pojavlauncher/build/outputs/apk/debug/`.

### Подробная сборка

Если вам нужен больший контроль над процессом сборки, выполните следующие шаги:

1. **Java Runtime Environment (JRE):** Загрузите артефакт `jre8-pojav` с наших [автоматических сборок CI](https://github.com/MojoLauncher/android-openjdk-build-multiarch/actions).  Этот пакет содержит предварительно собранные JRE для всех поддерживаемых архитектур.  Если вам нужно собрать JRE самостоятельно, следуйте инструкциям в репозитории [android-openjdk-build-multiarch](https://github.com/MojoLauncher/android-openjdk-build-multiarch).

2. **LWJGL:** Инструкции по сборке пользовательского LWJGL доступны в [репозитории LWJGL](https://github.com/MojoLauncher/lwjgl3).

3. **Список языков:** Поскольку языки автоматически добавляются Crowdin, вам необходимо запустить генератор списка языков перед сборкой. В каталоге проекта выполните:
   * Linux/macOS:
     ```bash
     chmod +x scripts/languagelist_updater.sh
     bash scripts/languagelist_updater.sh
     ```
   * Windows:
     ```batch
     scripts\languagelist_updater.bat
     ```

4. **Сборка GLFW stub:** `./gradlew :jre_lwjgl3glfw:build`

5. **Сборка лаунчера:** `./gradlew :app_pojavlauncher:assembleDebug`
   * При сборке на Windows:
     * Замените `./gradlew` на `.\gradlew.bat`
     * Убедитесь, что у вас стоит symlink `mojoexec`, `sdl`, `glfw` в `app_pojavlauncher/src/main/jni/`

## Текущая дорожная карта

- [x] Мобильный порт OpenJDK 8: ARM32, ARM64, x86, x86_64
- [x] Мобильный порт OpenJDK 17: ARM32, ARM64, x86, x86_64
- [x] Мобильный порт OpenJDK 21: ARM32, ARM64, x86, x86_64
- [x] Headless установщик модов
- [x] Установщик модов с графическим интерфейсом
- [x] OpenGL в среде OpenJDK
- [x] OpenAL (работает на большинстве устройств)
- [x] Поддержка Minecraft 1.12.2 и ниже
- [x] Поддержка Minecraft 1.13 и выше
- [x] Поддержка Minecraft 1.17 (22w13a) и выше
- [x] Масштабирование игровой поверхности
- [x] Переписанная система входных потоков в нативный код
- [x] Полностью переписанная система управления
- [x] Добавлен рендерер MobileGlues
- [x] Добавлен рендерер NG_GL4ES (Krypton Wrapper) как бэкенд GL4ES
- [x] Добавлен Freedreno для устройств Adreno
- [x] Система экземпляров вместо профилей
- [x] Поддержка 1.21.5 из коробки
- [x] Импорт mrpack/CurseForge zip
- [x] LTW: включить расширения вычислительных шейдеров/изображений
- [ ] LTW: решить проблемы с Create
- [ ] LTW: переключиться на цветопередающий формат для буферов кадров
- [ ] Добавить [MobileGL](https://github.com/MobileGL-Dev/MobileGL) для устройств Mali
- [ ] Инструмент управления модпаками/модами
- [ ] Совместимый с MMC импорт экземпляров
- [ ] Поддержка Vintage Story
- [ ] Внедрить общий стандарт нативной библиотеки
- [ ] Patch-on-dlopen для нативных библиотек модов
- [ ] Еще многое предстоит!

## Известные проблемы

Список известных проблем и их текущий статус можно найти в нашей [системе отслеживания проблем](https://github.com/TeamPojavLauncher/PojavLauncher/issues).

## Часто задаваемые вопросы

Больше информации можно найти в нашем [вики](https://pojav.ru/).

## Участие в разработке

Мы приветствуем желающих внести свой вклад в проект! Нам не помешает любая помощь, не только код. Например, вы можете помочь в разработке и формировании вики. Вы так же можете помочь [перевести проект](https://crowdin.com/project/pojavlauncher) на ваш язык!

Любые изменения в коде этого репозитория должны быть отправлены в виде pull request-а. Описание должно объяснять что делает код и предоставлять шаги для его запуска.

## Поддержка

Для поддержки пожалуйста присоединитесь к нашему [серверу Discord](https://discord.gg/2HYpzs4gZT).

## Лицензия

PojavLauncher лицензирован под [GNU LGPLv3](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE).

## Благодарности и компоненты

- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Лицензия неизвестна / [Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) или [GNU GPLv2](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE)
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [GNU LGPLv3 License](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher): [GNU LGPLv3 License](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE)
- [Amethyst](https://github.com/AngelAuraMC/Amethyst-Android): [GNU LGPLv3 License](https://github.com/AngelAuraMC/Amethyst-Android/blob/v3_openjdk/LICENSE)
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [GNU GPLv2 License](https://openjdk.java.net/legal/gplv2+ce.html)
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [MIT License](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)
- [Holy GL4ES](https://github.com/PojavLauncherTeam/holy-gl4es): [MIT License](https://github.com/PojavLauncherTeam/holy-gl4es/blob/main/LICENSE)
- [MobileGlues](https://github.com/MobileGL-Dev/MobileGlues): [LGPL-2.1 License](https://github.com/MobileGL-Dev/MobileGlues/blob/dev-es/LICENSE)
- [Krypton Wrapper](https://github.com/BZLZHH/NG-GL4ES): [MIT License](https://github.com/BZLZHH/NG-GL4ES/blob/main/LICENSE)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [MIT License](https://docs.mesa3d.org/license.html)
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [BSD-3 License](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
- [GLFW](https://github.com/MojoLauncher/glfw): [zlib license](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [SDL](https://github.com/MojoLauncher/MojoSDL): [zlib license](https://github.com/MojoLauncher/MojoSDL/blob/main/LICENSE.txt)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): 3-Clause BSD license
- [mojoexec](https://github.com/MojoLauncher/mojoexec): [MIT License](https://github.com/MojoLauncher/mojoexec/blob/master/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Unknown License / [Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) или [GNU GPLv2](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE)
- [pro-grade](https://github.com/pro-grade/pro-grade) (Java sandboxing security manager): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)
- [bhook](https://github.com/bytedance/bhook) (Exit code trapping): [MIT License](https://github.com/bytedance/bhook/blob/main/LICENSE)
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (Authorization via ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)
- [OpenAL-Soft](https://github.com/kcat/openal-soft): [GNU LIBRARY GENERAL PUBLIC LICENSE](https://github.com/kcat/openal-soft/blob/master/COPYING) и [modified PFFFT](https://github.com/kcat/openal-soft/blob/master/LICENSE-pffft)
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE)
- Спасибо [Mineskin](https://mineskin.eu/) и [MCHeads](https://mc-heads.net) за предоставление аватаров Minecraft

## Дорожная карта

Мы в настоящее время сосредоточены на:

* Исследовании новых технологий визуализации.

Планы на будущее включают:

* Повышение стабильности и производительности.
* Улучшение опыта установки модов.

Мы приветствуем отзывы сообщества и предложения по нашей дорожной карте. Не стесняйтесь открыть запрос на новую функцию в нашей [системе отслеживания проблем](https://github.com/TeamPojavLauncher/PojavLauncher/issues).
