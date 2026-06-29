package com.ultimateroadrunner.game;

import android.webkit.JavascriptInterface;

/**
 * JavaScript bridge exposed as window.AndroidBridge inside the WebView.
 * The game's index.html already calls these methods — no GDevelop changes needed.
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
}
