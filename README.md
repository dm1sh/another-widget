<img src="hero.png"/>

# Another Widget (`dm1sh.anotherwidget`)

[![Build Debug APK](https://github.com/dm1sh/another-widget/actions/workflows/build.yml/badge.svg)](https://github.com/dm1sh/another-widget/actions/workflows/build.yml)

**Another Widget** intelligently summarizes the information you need most on your Android home screen — upcoming calendar events, weather, alarms, media playback, notifications, and custom notes — in a clean "At a Glance" layout.

This repository is a maintained, **Google-free (FOSS)** fork of [Tommaso Berlose's Another Widget](https://github.com/tommasoberlose/another-widget) based on [`azuo/another-widget`](https://github.com/azuo/another-widget), published under the standalone application ID **`dm1sh.anotherwidget`** so it can be installed independently or alongside other builds.

---

## Key Changes in This Fork

* **Standalone Package ID (`dm1sh.anotherwidget`)**: Installs cleanly without conflicting with the archived upstream package (`com.tommasoberlose.anotherwidget`) or `azuo`'s package (`com.tommasoberlose.anotherwidget.azuo`).
* **De-Googled / Zero Proprietary SDKs**:
  * Removed **Google Play Services** (`play-services-location`, `play-services-fitness`, `play-services-auth`), **Firebase Crashlytics**, and **Google Play Billing**.
  * Removed the `apikey.properties` / `GOOGLE_API_KEY` build requirement.
  * Runs out of the box on de-Googled / AOSP /Huawei / GrapheneOS / CalyxOS devices without GMS.
* **Native Android Location**: Resolves device coordinates exclusively through Android's built-in `LocationManager`.
* **Yandex Weather Fallback**: Tapping the weather widget when no dedicated weather app is configured opens `https://yandex.ru/pogoda?lat=<lat>&lon=<lon>` for the widget's current coordinates.
* **Includes Upstream `azuo` Fixes & Features (`v2.3.3.8`)**:
  * AndroidX **Room** event database (replacing Realm) and **WorkManager** background workers (`WeatherWorker`, `UpdateCalendarWorker`).
  * Android 12+ exact alarm support (`SCHEDULE_EXACT_ALARM`) and Android 10+ background location handling.
  * Additional weather providers (**HERE**, **AccuWeather**, plus fixed **Weather.gov**, **Weatherbit**, and **Yr.no** support) and Weather as a secondary Glance provider.
  * Top-aligned widget modes (`TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`), multi-day event day-offset indicators, and calendar/line-wrapping fixes.

---

## Download & Building

### Prebuilt CI Artifacts
Every push to `patch` automatically builds and signs a debug APK via GitHub Actions. Download the latest `dm1sh.anotherwidget-debug` artifact from the [**Actions**](https://github.com/dm1sh/another-widget/actions/workflows/build.yml) tab.

### Building Locally
Requires **JDK 11** and the **Android SDK** (`compileSdkVersion 30`, `buildToolsVersion "30.0.3"`):

```bash
./gradlew assembleDebug
```

To sign `debug` and `release` builds with a custom JKS/PKCS12 keystore (as used in CI), set the following environment variables before running Gradle:

```bash
export SIGNING_STORE_FILE=/path/to/keystore.jks
export SIGNING_STORE_PASSWORD=your_store_password
export SIGNING_KEY_ALIAS=your_key_alias
export SIGNING_KEY_PASSWORD=your_key_password
./gradlew assembleDebug
```

---

## Translations

English base strings are located in [`app/src/main/res/values/strings.xml`](https://github.com/dm1sh/another-widget/blob/patch/app/src/main/res/values/strings.xml). To contribute or improve a localization, update or add `app/src/main/res/values-<lang>/strings.xml` and open a pull request on [`dm1sh/another-widget`](https://github.com/dm1sh/another-widget).

---

## Privacy Policy

This fork contains **no analytics, crash reporting, billing SDKs, or trackers**. See [`privacy-policy.md`](privacy-policy.md) for full details on permissions and optional user-configured weather API requests.

---

## License

* Copyright (c) 2017–2021 Tommaso Berlose
* Copyright (c) 2021–2026 azuo
* Copyright (c) 2023–2026 Dmitriy Shishkov (`dm1sh`)

Source code and binaries are distributed under the [MIT License](LICENSE).
