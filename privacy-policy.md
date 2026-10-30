# Privacy Policy

**Effective date:** October 30, 2026

**Another Widget (`dm1sh.anotherwidget`)** is an open-source Android home-screen widget maintained by **Dmitriy Shishkov (`dm1sh`)** as a fork of the original Another Widget project. The application is provided free of charge under the MIT License and is intended for use as is.

---

## 1. No Telemetry, Analytics, or Data Collection

Unlike the original upstream release, **all proprietary tracking, crash-reporting, billing, and Google Play Services SDKs (including Firebase Crashlytics, Google Play Services Location/Fitness/Auth, and Google Play Billing) have been completely removed** from `dm1sh.anotherwidget`.

* We **do not** operate any backend servers.
* We **do not** collect, transmit, store, or sell any personal data, device identifiers, IP addresses, crash logs, analytics, or usage statistics.
* We **do not** use cookies or advertising SDKs.
* Cleartext HTTP network traffic is disabled (`usesCleartextTraffic="false"`).

All widget preferences, cached calendar events, step counts, backups, and notes remain strictly on your device.

---

## 2. Device Permissions & Local Data Processing

Depending on which widget features you explicitly enable, the app may request the following Android permissions. All data accessed through these permissions is processed locally on your device:

* **Calendar (`READ_CALENDAR`)** — Optional. Used solely to read your upcoming device calendar events and display them in the widget. Event entries are cached locally in an on-device SQLite (Room) database and are never transmitted off your device.
* **Location (`ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`)** — Optional. Used only when weather display is enabled without a manually configured location. Coordinates are obtained via Android's built-in `LocationManager` solely to request weather forecasts from your selected weather provider (and, when tapping the weather widget without a custom weather app configured, to open Yandex Weather for those coordinates in your browser).
* **Health Connect (`android.permission.health.READ_STEPS`)** — Optional. Used locally on your device only if you enable the *Fitness* (Daily Steps) Glance provider. Your daily step total is read from Android Health Connect solely to render it in the widget and never leaves your device.
* **Notification Access (`BIND_NOTIFICATION_LISTENER_SERVICE`)** — Optional. Used locally on your device only if you enable the *Playing Song* (media session) or *Notifications* Glance providers. Notification titles and media metadata never leave your device, and cached notification state is cleared automatically when dismissed or when access is revoked.
* **Wallpaper / Media (`READ_MEDIA_IMAGES` on Android 13+, `READ_EXTERNAL_STORAGE` on Android 12 and lower)** — Optional. Used locally only to render your current home-screen wallpaper inside the in-app widget preview.
* **Alarms & Boot (`SCHEDULE_EXACT_ALARM`, `SET_ALARM`, `RECEIVE_BOOT_COMPLETED`)** — Used to refresh widget time, calendar events, step totals, and alarm indicators reliably on your device.
* **Settings Backup & Restore (Storage Access Framework)** — Exporting or importing settings writes or reads a user-chosen JSON file locally via Android's system document picker without requiring broad storage permissions.

---

## 3. Direct Requests to Optional Third-Party Services

The app only makes network connections when you configure features that require external data:

1. **Weather Providers (User-Selected)**: When weather is enabled, your device sends your configured or detected latitude/longitude directly over HTTPS to **only the single weather provider you choose**:
   * [OpenWeatherMap](https://openweather.co.uk/privacy-policy)
   * [Weatherbit](https://www.weatherbit.io/privacy)
   * [WeatherAPI.com](https://www.weatherapi.com/privacy.aspx)
   * [HERE Weather](https://legal.here.com/en-gb/privacy)
   * [AccuWeather](https://www.accuweather.com/en/privacy)
   * [Weather.gov (US National Weather Service)](https://www.weather.gov/privacy)
   * [Yr.no / MET Norway](https://www.yr.no/en/other-conditions/3-97251/privacy-policy)
   * [meteoblue](https://content.meteoblue.com/en/about-us/legal)
2. **Custom Location Search**: Searching for a manual weather city uses Android's platform `Geocoder` service. Secondary clock timezones are resolved **100% offline** using the on-device IANA timezone database.
3. **External Links / Weather Web Fallback**: If you tap the weather widget without a dedicated weather application installed or configured, your browser opens [`https://yandex.ru/pogoda`](https://yandex.ru/legal/confidential/) with `lat` and `lon` query parameters. Tapping event addresses opens your installed maps app (or OpenStreetMap in a browser if no maps app is installed).

Please refer to the privacy policies of those respective services when enabling or using them.

---

## 4. Children's Privacy

This application does not collect personal information from anyone, including children under the age of 13.

---

## 5. Changes to This Privacy Policy

Any future updates to this Privacy Policy will be committed directly to [`privacy-policy.md`](https://github.com/dm1sh/another-widget/blob/patch/privacy-policy.md) in the public source repository with an updated effective date.

---

## 6. Contact

If you have questions or concerns about this Privacy Policy or the application, please open an issue at [`https://github.com/dm1sh/another-widget/issues`](https://github.com/dm1sh/another-widget/issues) or contact **Dmitriy Shishkov** at `me@dmitriy.icu`.
