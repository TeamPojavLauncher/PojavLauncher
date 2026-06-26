<h1 align="center">PojavLauncher</h1>

<img src="https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/assets/pojavlauncher.png" align="left" width="130" height="150" alt="Логотип PojavLauncher">

[![Android CI](https://github.com/TeamPojavLauncher/PojavLauncher/workflows/Android%20CI/badge.svg)](https://github.com/TeamPojavLauncher/PojavLauncher/actions)
<img src="https://img.shields.io/badge/platform-Android-green" alt="Платформа"/>
<img src="https://img.shields.io/badge/minSdk-21-blue" alt="Min SDK"/>
[![Статус проекта](https://img.shields.io/badge/status-active-brightgreen.svg?style=flat)](https://github.com/TeamPojavLauncher/PojavLauncher/commits)
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/TeamPojavLauncher/PojavLauncher.svg?style=flat)](https://github.com/TeamPojavLauncher/PojavLauncher/graphs/commit-activity)
[![Проблемы на GitHub](https://img.shields.io/github/issues/TeamPojavLauncher/PojavLauncher.svg?style=flat&color=%23FFA500)](https://github.com/TeamPojavLauncher/PojavLauncher/issues)
[![Crowdin](https://badges.crowdin.net/pojavlauncher/localized.svg)](https://crowdin.com/project/pojavlauncher)
[![Контрибьютеры GitHub](https://img.shields.io/github/contributors/TeamPojavLauncher/PojavLauncher.svg?style=flat)](https://github.com/TeamPojavLauncher/PojavLauncher/graphs/contributors)
<a href="https://discord.gg/2HYpzs4gZT"><img src="https://img.shields.io/discord/1355213558631366897?color=5865F2&logo=discord&logoColor=white&label=&style=flat" alt="Discord"></a>
<a href="https://github.com/TeamPojavLauncher"><img src="https://img.shields.io/badge/github-PojavLauncher-green?logo=github" alt="GitHub"></a>
[![Последний выпуск](https://img.shields.io/github/v/tag/TeamPojavLauncher/PojavLauncher?sort=semver)](https://github.com/TeamPojavLauncher/PojavLauncher/releases "Посмотреть последний выпуск")
[![Лицензия: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue)](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
[![Twitter Follow](https://img.shields.io/twitter/follow/PLaunchTeam.svg?style=social)](https://x.com/PLaunchTeam)

*Из пепла [Boardwalk](https://github.com/zhuowei/Boardwalk) рождается PojavLauncher!*

PojavLauncher — это лаунчер, позволяющий вам играть в Minecraft: Java Edition на вашем Android и [iOS](https://github.com/PojavLauncherTeam/PojavLauncher_iOS) устройстве.

Для получения дополнительной информации посетите наш [вики](https://pojavlauncher.app/)!

> [!NOTE]
> Это не оригинальный PojavLauncher. Оригинальный проект был прекращён. Данный репозиторий является переписанной версией на основе коммитов [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher). Мы не связаны с командой PojavLauncherTeam в какой-либо форме.
>
> Мы не ведём никаких социальных сетей, кроме Discord. Все официальные социальные каналы PojavLauncherTeam были архивированы.
>
> - Присоединяйтесь к [Discord](https://discord.gg/2HYpzs4gZT) для получения последних обновлений и новостей.
> - Исходный код оригинального PojavLauncher доступен [здесь](https://github.com/PojavLauncherTeam/PojavLauncher).

## Оглавление

* [Введение](#введение)
* [Получение PojavLauncher](#получение-pojavlauncher)
* [Сборка](#сборка)
    * [Быстрая сборка (рекомендуется)](#быстрая-сборка-рекомендуется)
    * [Подробная сборка](#подробная-сборка)
* [Текущий статус](#текущий-статус)
* [Известные проблемы](#известные-проблемы)
* [Часто задаваемые вопросы](#часто-задаваемые-вопросы)
* [Участие в разработке](#участие-в-разработке)
* [Поддержка](#поддержка)
* [Лицензия](#лицензия)
* [Благодарности и зависимости](#благодарности-и-компоненты-от-третьих-сторон-с-их-лицензиями)
    * [Основные компоненты](#основные-компоненты)
    * [Фреймворк и библиотеки поддержки](#фреймворк-и-библиотеки-поддержки)
    * [Графика и отрисовка](#графика-и-отрисовка)
    * [Java и игровые библиотеки](#java-и-игровые-библиотеки)
    * [Безопасность и система](#безопасность-и-система)
    * [Звук](#звук)
    * [Другие сервисы](#другие-сервисы)
* [Дорожная карта](#дорожная-карта)

## Введение

* PojavLauncher — это лаунчер Minecraft: Java Edition для Android и iOS, основанный на [Boardwalk](https://github.com/zhuowei/Boardwalk)
* Этот лаунчер может запускать практически все доступные версии Minecraft, начиная от rd-132211 до 26.x снимков (включая версии Combat Test)
* Также поддерживается модификация через Forge и Fabric.
* Этот репозиторий содержит исходный код для Android. Для iOS/iPadOS посетите [PojavLauncher_iOS](https://github.com/PojavLauncherTeam/PojavLauncher_iOS).

## Получение PojavLauncher

Вы можете получить PojavLauncher тремя способами:

1. **Выпуски:** Загрузите последнюю предварительно собранную версию приложения с [nightly.link](https://nightly.link/TeamPojavLauncher/PojavLauncher/workflows/android/v3_openjdk?preview) или выберите из наших [стабильных выпусков](https://github.com/TeamPojavLauncher/PojavLauncher/releases) или [автоматических сборок](https://github.com/TeamPojavLauncher/PojavLauncher/actions).
2. **Google Play:** Загрузите из Google Play, нажав на этот значок: [![Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=net.kdt.pojavlaunch)
3. **Сборка из исходного кода:** Следуйте [инструкциям по сборке](#сборка) ниже.

## Сборка

### Быстрая сборка (рекомендуется)

Самый простой способ собрать PojavLauncher — это использовать предварительно собранные JRE, предоставляемые нашей CI.

1. Клонируйте репозиторий: `git clone https://github.com/TeamPojavLauncher/PojavLauncher.git`
2. Соберите лаунчер: `./gradlew :app_pojavlauncher:assembleDebug` (Используйте `gradlew.bat` на Windows)

Собранный APK будет находиться в `app_pojavlauncher/build/outputs/apk/debug/`.

### Подробная сборка

Если вам нужен больший контроль над процессом сборки, выполните следующие шаги:

1. **Java Runtime Environment (JRE):** Загрузите артефакт `jre8-pojav` с наших [автоматических сборок CI](https://github.com/TeamPojavLauncher/android-openjdk-build-multiarch/actions). Этот пакет содержит предварительно собранные JRE для всех поддерживаемых архитектур. Если вам нужно собрать JRE самостоятельно, следуйте инструкциям в репозитории [android-openjdk-build-multiarch](https://github.com/PojavLauncherTeam/android-openjdk-build-multiarch).

2. **LWJGL:** Инструкции по сборке пользовательского LWJGL доступны в [репозитории LWJGL](https://github.com/PojavLauncherTeam/lwjgl3).

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

5. **Сборка лаунчера:** `./gradlew :app_pojavlauncher:assembleDebug` (Замените `gradlew` на `gradlew.bat` на Windows).

## Текущая дорожная карта
- [x] OpenJDK 8 Mobile port: ARM32, ARM64, x86, x86_64
- [x] OpenJDK 17 Mobile port: ARM32, ARM64, x86, x86_64
- [x] OpenJDK 21 Mobile port: ARM32, ARM64, x86, x86_64
- [x] Установщик модов без интерфейса
- [x] Установщик модов с графическим интерфейсом
- [x] OpenGL в окружении OpenJDK
- [x] OpenAL (работает на большинстве устройств)
- [x] Поддержка Minecraft 1.12.2 и ниже
- [x] Поддержка Minecraft 1.13 и выше
- [x] Поддержка Minecraft 1.17 (22w13a) и выше
- [x] Масштабирование игровой поверхности
- [x] Новый входной канал переписан в нативный код
- [x] Полностью переработана система управления
- [x] Добавлен визуализатор MobileGlues
- [x] Добавлен визуализатор NG_GL4ES (Krypton Wrapper)
- [x] Система инстансов вместо профилей
- [x] Готовая поддержка 1.21.5
- [ ] LTW: решение проблем с Create
- [ ] LTW: включение расширений compute shader/image
- [ ] LTW: переход на формат с возможностью визуализации цвета для буферов кадров
- [ ] Инструмент управления модпаками/модами
- [x] Импорт mrpack/CurseForge zip
- [ ] Импорт инстансов совместимых с MMC
- [ ] Patch-on-dlopen для нативных библиотек модов
- [ ] Ещё много чего в разработке!

## Известные проблемы

Список известных проблем и их текущий статус можно найти в нашей [системе отслеживания проблем](https://github.com/TeamPojavLauncher/PojavLauncher/issues).

## Часто задаваемые вопросы

Больше информации можно найти в нашем [вики](https://pojavlauncherteam.github.io/).

## Участие в разработке

Мы приветствуем вклад! Мы приветствуем любого вида вклад, не только код. Например, вы можете помочь улучшить [вики](https://pojavlauncherteam.github.io/), поспособствовать [переводам](https://crowdin.com/project/pojavlauncher) или отправить отчёты об ошибках и запросы на новые функции.

Любое изменение кода должно быть отправлено как pull request. Описание должно объяснить, что делает код, и дать шаги для его выполнения.

## Поддержка

Для получения поддержки присоединитесь к нашему [серверу Discord](https://discord.com/invite/aenk3EUvER).

## Лицензия

PojavLauncher распространяется под лицензией [GNU LGPLv3](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE).

## Кредиты и компоненты третьих сторон и их лицензии

### Основные компоненты
- [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher): [Лицензия GNU LGPLv3](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/LICENSE)
- [Boardwalk](https://github.com/zhuowei/Boardwalk) (JVM Launcher): Лицензия неизвестна / [Apache License 2.0](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE) или [GNU GPLv2](https://github.com/zhuowei/Boardwalk/blob/master/LICENSE)
- [MojoLauncher](https://github.com/MojoLauncher/MojoLauncher): [Лицензия GNU LGPLv3](https://github.com/MojoLauncher/MojoLauncher/blob/v3_openjdk/LICENSE)

### Поддержка фреймворков и библиотек
- Android Support Libraries: [Apache License 2.0](https://android.googlesource.com/platform/prebuilts/maven_repo/android/+/master/NOTICE.txt)
- [OpenJDK](https://github.com/PojavLauncherTeam/openjdk-multiarch-jdk8u): [Лицензия GNU GPLv2](https://openjdk.java.net/legal/gplv2+ce.html)

### Графика и рендеринг
- [GL4ES](https://github.com/PojavLauncherTeam/gl4es): [Лицензия MIT](https://github.com/ptitSeb/gl4es/blob/master/LICENSE)
- [MobileGlues](https://github.com/MobileGL-Dev/MobileGlues): [Лицензия LGPL-2.1](https://github.com/MobileGL-Dev/MobileGlues/blob/dev-es/LICENSE)
- [Krypton Wrapper](https://github.com/BZLZHH/NG-GL4ES): [Лицензия MIT](https://github.com/BZLZHH/NG-GL4ES/blob/main/LICENSE)
- [Mesa 3D Graphics Library](https://gitlab.freedesktop.org/mesa/mesa): [Лицензия MIT](https://docs.mesa3d.org/license.html)

### Java и игровые библиотеки
- [LWJGL3](https://github.com/MojoLauncher/lwjgl3): [Лицензия BSD-3](https://github.com/LWJGL/lwjgl3/blob/master/LICENSE.md)
- [GLFW](https://github.com/MojoLauncher/glfw): [zlib license](https://github.com/MojoLauncher/glfw/blob/glfw34/LICENSE.md)
- [LWJGL2-GLFW](https://github.com/MojoLauncher/lwjgl2-glfw): 3-Clause BSD license

### Безопасность и система
- [pro-grade](https://github.com/pro-grade/pro-grade) (менеджер безопасности Java sandboxing): [Apache License 2.0](https://github.com/pro-grade/pro-grade/blob/master/LICENSE.txt)
- [bhook](https://github.com/bytedance/bhook) (перехват кода выхода): [Лицензия MIT](https://github.com/bytedance/bhook/blob/main/LICENSE)
- [Authlib-Injector](https://github.com/yushijinhun/authlib-injector) (авторизация через ely.by): [AGPL-3.0](https://github.com/yushijinhun/authlib-injector/blob/develop/LICENSE)

### Аудио
- [OpenAL-Soft](https://github.com/kcat/openal-soft): [Лицензия GNU GPLv2](app_pojavlauncher/src/main/assets/licenses/OPENAL-SOFT_GPL2)
- [oboe](https://github.com/google/oboe): [Apache License 2.0](https://github.com/google/oboe/blob/main/LICENSE)

### Другие услуги
- Спасибо [Mineskin](https://mineskin.eu/) и [MCHeads](https://mc-heads.net) за предоставление аватаров Minecraft

## Дорожная карта

Мы в настоящее время сосредоточены на:

* Исследовании новых технологий визуализации.

Планы на будущее включают:

* Повышение стабильности и производительности.
* Улучшение опыта установки модов.

Мы приветствуем отзывы сообщества и предложения по нашей дорожной карте. Не стесняйтесь открыть запрос на новую функцию в нашей [системе отслеживания проблем](https://github.com/PojavLauncherTeam/PojavLauncher/issues).
