# Yandex Ads Integration — GDevelop Setup Guide

The native Android SDK handles all ad loading and display automatically.
You only need to trigger ads from GDevelop at the right moments.

---

## ✅ What works automatically (no GDevelop changes needed)
- **Banner ad** loads and appears at the bottom on startup
- All ads have proper close/X buttons (passes Huawei & Google review)

---

## 🎮 Interstitial Ad (full-screen — on death or level complete)

In GDevelop, add a **"Execute JavaScript"** event where the player dies
or finishes a level:

```javascript
if (window.AndroidBridge) {
    window.AndroidBridge.showInterstitialAd();
}
```

The next interstitial preloads automatically after each one closes.

---

## 🎁 Rewarded Ad (watch ad → get coins / extra life)

### Step 1 — Trigger the ad (on button press)

```javascript
if (window.AndroidBridge) {
    window.AndroidBridge.showRewardedAd();
}
```

### Step 2 — Receive the reward (add this ONCE, e.g. on scene start)

```javascript
window.onRewardedAdComplete = function() {
    // Give the player their reward here, e.g.:
    gdjs.evtTools.variable.setNumber(
        runtimeScene.getVariables().get("Coins"),
        gdjs.evtTools.variable.getVariableNumber(
            runtimeScene.getVariables().get("Coins")
        ) + 50
    );
};
```

Or using a simpler GDevelop global variable approach:
```javascript
window.onRewardedAdComplete = function() {
    runtimeScene.getVariables().get("RewardReady").setNumber(1);
};
```
Then in your GDevelop events, check `Variable(RewardReady) = 1`,
give the coins, and reset it back to 0.

---

## 🔑 Ad Unit IDs (replace before final release)

Open `app/src/main/java/com/roadrunner/game/MainActivity.java`
and replace the three placeholder values near the top:

```java
private static final String BANNER_AD_UNIT_ID       = "YOUR_R-M-XXXXXXXX-X";
private static final String INTERSTITIAL_AD_UNIT_ID = "YOUR_R-M-XXXXXXXX-X";
private static final String REWARDED_AD_UNIT_ID     = "YOUR_R-M-XXXXXXXX-X";
```

Get these from https://partner2.yandex.ru/ → Your app (ID 19994035)
→ Ad Blocks → Create Banner / Interstitial / Rewarded block.

> **Current build uses Yandex demo IDs** — real ads will show after
> you replace the IDs and rebuild.

---

## 🔍 Yandex Verification (app-ads.txt)

Yandex also requires verifying app ownership. To unblock verification:
1. Go to https://partner2.yandex.ru/ → Settings → your Road Runner app
2. Add your email **jacksonmich972@gmail.com** in the "Developer email"
   field on the Access tab
3. Wait ~3 hours for Yandex to re-check, then click "Complete verification"
