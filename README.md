# Ultimate Road Runner — Android APK Builder

This repo wraps your GDevelop web game into an Android APK with **Appodeal** banner + interstitial ads.

---

## 📁 Project Structure

```
ultimate-road-runner-android/
├── app/
│   └── src/main/
│       ├── assets/game/         ← PUT YOUR GDEVELOP GAME FILES HERE
│       ├── java/com/ultimateroadrunner/game/
│       │   ├── MainActivity.java
│       │   └── AppodealBridge.java
│       └── res/
├── .github/workflows/build-apk.yml   ← GitHub Actions auto-build
└── README.md
```

---

## 🎮 Step 1 — Add Your Game Files

1. Export your game from GDevelop as **HTML5 / Web**
2. Copy ALL the exported files into:
   ```
   app/src/main/assets/game/
   ```
   Make sure `index.html` is directly inside `game/` (not in a subfolder).

---

## 📢 Step 2 — Add Ad Calls in Your Game (GDevelop)

Add this JavaScript event in GDevelop to show an interstitial ad (e.g. between levels):

```javascript
// Show interstitial ad between levels
if (window.showInterstitialAd) {
    window.showInterstitialAd();
}
```

The banner ad shows **automatically** at the bottom of the screen — no code needed.

---

## 🔑 Step 3 — Set Up GitHub Secrets (for APK signing)

Go to your GitHub repo → **Settings → Secrets and variables → Actions** and add:

| Secret Name        | Value |
|--------------------|-------|
| `KEYSTORE_BASE64`  | Your keystore file encoded as base64 (see below) |
| `KEYSTORE_PASSWORD`| Your keystore password |
| `KEY_ALIAS`        | Your key alias |
| `KEY_PASSWORD`     | Your key password |

### How to encode your keystore to base64:
```bash
# On Mac/Linux:
base64 -i your-keystore.jks | pbcopy

# On Windows (PowerShell):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("your-keystore.jks")) | clip
```
Paste the result as the `KEYSTORE_BASE64` secret.

---

## 🏗️ Step 4 — Build the APK

**Automatic:** Every push to `main` triggers a build.

**Manual:** Go to GitHub → **Actions → Build APK → Run workflow**

Download your APK from the **Artifacts** section after the build completes.

---

## 📱 Appodeal Details

- **App Key:** `d7441b7444df839562102f3e95a44793d98cd126509b5ce2`
- **Bundle ID:** `com.ultimateroadrunner.game`
- **Ads:** Banner (bottom) + Interstitial

---

## ⚠️ Notes

- Game is forced to **landscape** orientation
- Minimum Android version: **5.0 (API 21)**
- Your old keystore/signature from Google Drive can be used directly for `KEYSTORE_BASE64`
