# How to Add Appodeal Ads in GDevelop

## Banner Ad
The banner shows **automatically** at the bottom when the game loads.
No GDevelop changes needed.

## Interstitial Ad (Between Levels)

In GDevelop, add a **JavaScript event** where you want the interstitial to appear
(e.g. when the player completes a level or dies):

### GDevelop Event Setup:
1. Add event: **Execute a JavaScript code**
2. Paste this code:

```javascript
if (window.showInterstitialAd) {
    window.showInterstitialAd();
}
```

That's it! The bridge between your game and the Android Appodeal SDK is already wired up.

## Testing
- When you install a **debug** build, ads will show in test mode automatically.
- Switch `Appodeal.setTesting(false)` in `MainActivity.java` only when releasing to store.
