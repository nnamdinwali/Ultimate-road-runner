# Ultimate Road Runner — Android APK Builder

This repository wraps the GDevelop web game into an Android APK using the **direct Yandex Mobile Ads Android SDK**. No Appodeal, Unity Ads adapter, or other mediation layer is included.

The Android shell supports a sticky banner, interstitial, rewarded, app-open, native, and feed placement. Full-screen formats use a single persistent Yandex loader per format. One loaded ad is held for display, and the next request starts immediately after the displayed ad is consumed or fails to show. Load failures use bounded backoff rather than an uncontrolled request loop.

## Project Structure

```text
ultimate-road-runner/
├── app/
│   └── src/main/
│       ├── assets/game/         ← GDevelop HTML5 export
│       ├── java/com/roadrunner/game/
│       │   ├── MainActivity.java
│       │   └── AndroidBridge.java
│       └── res/
├── .github/workflows/build-apk.yml
└── README.md
```

## Add or Update the Game

Export the game from GDevelop as **HTML5 / Web** and copy the complete export into `app/src/main/assets/game/`. The `index.html` file must be directly inside that directory, not nested in another folder.

The packaged game already calls the Android bridge for its ad placements. The interstitial is triggered from the game-over/death flow after a short delay and is limited to once per 60 seconds. Rewarded ads are requested by the game’s watch-ad action. The banner is shown only during active gameplay. Native ads are shown on the menu, while the feed placement is shown in the shop.

For a manual interstitial call from GDevelop, use:

```javascript
if (window.AndroidBridge && typeof window.AndroidBridge.showInterstitialAd === 'function') {
    window.AndroidBridge.showInterstitialAd();
}
```

## Yandex Mobile Ads Configuration

The app uses the direct dependency:

```gradle
implementation 'com.yandex.android:mobileads:8.3.0'
```

The placement IDs are defined in `MainActivity.java`. Replace them with the production placement IDs from the Yandex Advertising Network account before release if the repository values are not the intended production placements.

Yandex SDK calls run on the Android main thread. Interstitial, rewarded, and app-open objects are cleared after use, while their persistent loaders prepare the next ad. A failed request is retried with bounded backoff to avoid duplicate requests and repeated unsuccessful calls. `FeedAd` is preloaded once and manages its sequential feed internally.

## Return Reminder

The Android shell schedules a one-time local notification for four hours after the activity leaves the foreground. When the player returns, the pending reminder is cancelled and a new one is scheduled only when the player leaves again. Tapping the notification opens Ultimate Road Runner directly. Android 13 and later require the user to grant notification permission; if that permission is denied, Android will not display the reminder.

This is a device-local reminder. It does not require FCM, HMS, a push token, or an internet connection at the time the reminder appears. Android may still defer reminders under device battery-saving policies.

## Signing Secrets

For GitHub Actions APK signing, add these secrets under **Settings → Secrets and variables → Actions**:

| Secret | Purpose |
| --- | --- |
| `KEYSTORE_BASE64` | Base64-encoded Android keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing key alias |
| `KEY_PASSWORD` | Signing key password |

## Build

Every push to `main` triggers the configured GitHub Actions build. A manual build can be started from **GitHub → Actions → Build APK → Run workflow**. Download the resulting APK from the workflow artifacts.

The project targets Android API 35, supports Android API 21 and later, and uses Android Gradle Plugin 8.9.1 with Gradle 8.11.1 to match the current Yandex Mobile Ads 8.3.0 integration guidance.

## Notes

The game remains configured for its existing orientation and WebView behavior. Ad presentation remains controlled by the game’s existing placement logic; the Android-side changes only standardize direct Yandex loading, caching, lifecycle cleanup, and retry behavior.
