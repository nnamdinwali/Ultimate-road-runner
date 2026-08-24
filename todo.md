
- [x] Verify WebGame JavaScript invokes AndroidBridge Native and Feed ad methods and repair any missing triggers
- [x] Validate end-to-end Native and Feed communication between WebGame and Android shell

- [x] Diagnose the exact GitHub Actions compiler failure in the Native/Feed Android integration
- [x] Fix the Android build failure and verify a passing APK workflow before claiming readiness

- [x] Prevent the Native ad container from showing as an empty black bar when no ad content is rendered
- [x] Validate Native ad failure and successful-render visibility behavior in Android CI

- [ ] Add an app-owned ✕ close control to the Native and Feed ad overlay
- [ ] Show the ad overlay from the game menu and allow immediate return to the game
- [ ] Validate close behavior and Android CI build

- [x] Remove the Yandex Unity Ads mediation dependency and update stale Appodeal/Yandex documentation
- [x] Refactor interstitial, rewarded, and app-open ads to use one persistent loader with one current ad and one controlled preload after consumption
- [x] Repair native and feed ad loading/display lifecycle, including safe reloads after use and visibility state handling
- [x] Add bounded retry/backoff so failed ad requests do not loop or cause duplicate loads
- [x] Run Android compile/tests and inspect the final diff before syncing changes to GitHub

- [x] Remove the 60-second interstitial cooldown from the game WebView trigger
- [x] Verify and repair explicit native and feed ad bridge calls from the packaged web game
- [x] Build and push the cooldown and native/feed trigger fixes, then verify GitHub Actions

- [ ] Add a local four-hour inactivity reminder that schedules when the app leaves the foreground and cancels when the player returns
- [ ] Add the Android 13 notification permission/channel and a tap action that reopens Ultimate Road Runner
- [ ] Build and push the local reminder implementation, then verify the APK workflow

- [ ] Inspect native and feed ad paths only and identify concrete display or lifecycle defects
- [ ] Apply and validate only confirmed native/feed fixes; leave all other ad formats and gameplay unchanged

- [ ] Add bounded retry recovery to the Yandex feed load failure path only
- [ ] Validate and push the feed-only retry amendment
