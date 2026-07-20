package com.roadrunner.game;

import android.webkit.JavascriptInterface;

/**
 * JavaScript bridge exposed as window.AndroidBridge inside the GDevelop WebView.
 *
 * In GDevelop, call these from a "Execute JavaScript" event:
 *
 *   // Show interstitial (on death / level complete):
 *   if (window.AndroidBridge) window.AndroidBridge.showInterstitialAd();
 *
 *   // Show rewarded ad (watch-ad-for-coins button):
 *   if (window.AndroidBridge) window.AndroidBridge.showRewardedAd();
 *   // → game receives window.onRewardedAdComplete() when reward is earned
 *
 *   // Open Privacy Policy:
 *   if (window.AndroidBridge) window.AndroidBridge.openPrivacyPolicy();
 */
public class AndroidBridge {

    private final MainActivity activity;

    public AndroidBridge(MainActivity activity) {
        this.activity = activity;
    }

    // ── Privacy Policy ───────────────────────────────────────────────────────

    @JavascriptInterface
    public void openPrivacyPolicy() {
        activity.openPrivacyPolicy();
    }

    // ── Interstitial Ad ──────────────────────────────────────────────────────

    /**
     * Show a full-screen interstitial ad.
     * Call this when the player dies or completes a level.
     * The ad has a proper close button — no Huawei review issues.
     */
    @JavascriptInterface
    public void showInterstitialAd() {
        activity.showInterstitialAd();
    }

    // ── Rewarded Ad ──────────────────────────────────────────────────────────

    /**
     * Show a rewarded video ad.
     * When the player watches it fully, the game's
     * window.onRewardedAdComplete() function is called so you can
     * grant coins, extra lives, etc.
     */
    @JavascriptInterface
    public void showRewardedAd() {
        activity.showRewardedAd(() ->
            activity.runOnUiThread(() ->
                activity.webView != null
                    ? activity.webView.evaluateJavascript(
                        "if(typeof window.onRewardedAdComplete === 'function') { window.onRewardedAdComplete(); }",
                        null)
                    : null
            )
        );
    }
}
