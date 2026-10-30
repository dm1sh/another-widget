<img src="hero.png"/>

# Another Widget (`dm1sh.anotherwidget`)

[![Build Debug APK](https://github.com/dm1sh/another-widget/actions/workflows/build.yml/badge.svg)](https://github.com/dm1sh/another-widget/actions/workflows/build.yml)

**Another Widget** intelligently summarizes the information you need most on your Android home screen — upcoming calendar events, weather, alarms, daily steps, battery status, media playback, notifications, and custom notes — in a clean "At a Glance" layout.

This repository is a maintained, **Google-free (FOSS)** fork of [Tommaso Berlose's Another Widget](https://github.com/tommasoberlose/another-widget) based on [`azuo/another-widget`](https://github.com/azuo/another-widget), published under the standalone application ID **`dm1sh.anotherwidget`** so it can be installed independently or alongside other builds.

---

## Key Features & Improvements in This Fork

* **Standalone Package ID (`dm1sh.anotherwidget`)**: Installs cleanly without conflicting with the archived upstream package (`com.tommasoberlose.anotherwidget`) or `azuo`'s package (`com.tommasoberlose.anotherwidget.azuo`).
* **100% De-Googled / Zero Proprietary SDKs**:
  * Removed **Google Play Services** (`play-services-location`, `play-services-fitness`, `play-services-auth`), **Firebase Crashlytics**, **Google Play Billing**, and defunct `jcenter()` binary artifacts.
  * Runs out of the box on AOSP, GrapheneOS, CalyxOS, LineageOS, Huawei, and standard Android devices alike.
  * Enforces HTTPS-only network traffic (`usesCleartextTraffic="false"`).
* **Modernized Android 13–16 Stack (`compileSdk 36`, `targetSdk 35`)**:
  * Built with Gradle `8.11.1`, AGP `8.9.1`, Kotlin `2.2.10`, KSP, Room `2.7.1`, and WorkManager `2.10.0`.
  * Android 11+ `<queries>` package visibility for reliable default calendar, event, alarm, and custom app launches.
  * Android 13+ Material You `<monochrome>` themed launcher icon, `READ_MEDIA_IMAGES` wallpaper preview permission, and native per-app language support.
  * Canvas + `BlurMaskFilter` shadow rendering (replacing deprecated `RenderScript`).
* **Smart Glance & Widget Enhancements**:
  * **Health Connect Daily Steps**: GMS-free daily step count integration via AndroidX Health Connect (`READ_STEPS`).
  * **Configurable Next-Alarm Lookahead Window**: Show upcoming clock alarms always or only within `30m`, `1h`, `3h`, `6h`, `12h`, or `24h`, while filtering out non-clock automation/reminder triggers (`setAlarmClock`) and keeping alarms visible right up until they ring.
  * **Smarter Notifications**: Displays recognizable full-color notifying app icons, filters out persistent foreground-service/system notifications, strips redundant `"WhatsApp: "` prefixes, and purges stale cached notifications automatically.
  * **Live Battery Percentage & Stable Greetings**: Displays `"Charging - XX%"` / low battery percentage with debounced updates without reshuffling random time-of-day greetings on unrelated widget redraws.
  * **Offline Secondary Timezone Picker**: Instant, searchable on-device IANA `TimeZone` selector replacing the defunct cleartext GeoNames API.
  * **JSON Settings Backup & Restore**: Permissionless Storage Access Framework (SAF) export and import of widget settings with an optional toggle for weather API keys.
  * **Per-App Language Picker & Expanded Translations**: Built-in language selector with community translations including English, Danish, German, Spanish, French, Indonesian, Italian, Polish, Portuguese, Russian, Slovak, Ukrainian, and Simplified Chinese, plus RTL centered preview fixes.
* **Modernized GMS-Free Location & Weather**:
  * Uses `LocationManagerCompat.getCurrentLocation` with multi-provider freshness/accuracy candidate scoring, stationary weather cache reuse, and reverse-geocoded city/district labels in Weather settings.
  * Supports all 7 weather providers (**Yr.no**, **Weather.gov**, **OpenWeatherMap**, **Weatherbit**, **WeatherAPI**, **HERE**, **AccuWeather**) plus **Yandex Weather** web fallback (`https://yandex.ru/pogoda?lat=<lat>&lon=<lon>`) when tapping weather without a dedicated weather app.

---

## Download & Building

### Prebuilt CI Artifacts
Every push to `patch` automatically runs unit tests and builds a signed debug APK via GitHub Actions. Download the latest `dm1sh.anotherwidget-debug` artifact from the [**Actions**](https://github.com/dm1sh/another-widget/actions/workflows/build.yml) tab.

### Building with Gradle
Requires **JDK 17+** (JDK 21 recommended) and the **Android SDK** (`compileSdk 36`, `buildToolsVersion "35.0.0"`):

```bash
./gradlew testDebugUnitTest assembleDebug
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
