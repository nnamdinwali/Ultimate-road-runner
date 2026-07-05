package com.ultimateroadrunner.game;

import android.content.Intent;
import android.net.Uri;
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

    /** Opens any URL in the device's default browser. Used by the in-game Privacy Policy button. */
    @JavascriptInterface
    public void openURL(String url) {
        activity.runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } catch (Exception e) {
                // If no browser is available, fail silently
            }
        });
    }
}
