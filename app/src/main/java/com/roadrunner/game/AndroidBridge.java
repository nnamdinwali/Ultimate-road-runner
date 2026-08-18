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

    // ── Push token registration ───────────────────────────────────────────────

    /**
     * Register a real provider token once an FCM/HMS SDK is added to the shell.
     * Example: window.AndroidBridge.registerPushToken("fcm", token)
     */
    @JavascriptInterface
    public void registerPushToken(String provider, String token) {
        activity.registerPushToken(provider, token);
    }

    // ── Privacy Policy ───────────────────────────────────────────────────────

    @JavascriptInterface
    public void openPrivacyPolicy() {
        activity.openPrivacyPolicy();
    }

    // ── Banner Ad ────────────────────────────────────────────────────────────

    /** Show the bottom banner — JS calls this when active gameplay is detected. */
    @JavascriptInterface
    public void showBanner() { activity.showBanner(); }

    /** Hide the bottom banner — JS calls this on Menu / Game Over / Shop / Settings. */
    @JavascriptInterface
    public void hideBanner() { activity.hideBanner(); }

    // ── Native Ad ────────────────────────────────────────────────────────────

    /** Show a Yandex native placement after it has been loaded. */
    @JavascriptInterface
    public void showNativeAd() { activity.showNativeAd(); }

    /** Hide the Yandex native placement without destroying its loaded ad. */
    @JavascriptInterface
    public void hideNativeAd() { activity.hideNativeAd(); }

    // ── Feed Ad ──────────────────────────────────────────────────────────────

    /** Show the Yandex feed placement. */
    @JavascriptInterface
    public void showFeedAd() { activity.showFeedAd(); }

    /** Hide the Yandex feed placement. */
    @JavascriptInterface
    public void hideFeedAd() { activity.hideFeedAd(); }

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
            activity.runOnUiThread(() -> {
                if (activity.webView != null) {
                    activity.webView.evaluateJavascript(
                        "if(typeof window.onRewardedAdComplete==='function'){window.onRewardedAdComplete();}",
                        null);
                }
            })
        );
    }
}
