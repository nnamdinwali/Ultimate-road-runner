package com.roadrunner.game;

import android.webkit.JavascriptInterface;

/**
 * JavaScript bridge exposed as window.AndroidBridge inside the WebView.
 * The game's index.html calls these methods — no GDevelop changes needed.
 *
 * NOTE: openPrivacyPolicy() takes NO parameter from JS to prevent intent
 * injection attacks. The destination URL is a hardcoded constant in MainActivity.
 */
public class AndroidBridge {

    private final MainActivity activity;

    public AndroidBridge(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void showBanner() {
        activity.showBanner();
    }

    @JavascriptInterface
    public void hideBanner() {
        activity.hideBanner();
    }

    @JavascriptInterface
    public void showInterstitialAd() {
        activity.showInterstitialAd();
    }

    @JavascriptInterface
    public void showRewardedAd() {
        activity.showRewardedAd();
    }

    /**
     * Opens the Privacy Policy page in the device browser.
     * No URL parameter — the destination is a trusted constant in MainActivity,
     * not controllable by any JavaScript running in the WebView.
     */
    @JavascriptInterface
    public void openPrivacyPolicy() {
        activity.openPrivacyPolicy();
    }
}
