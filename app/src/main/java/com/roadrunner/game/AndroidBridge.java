package com.roadrunner.game;

import android.webkit.JavascriptInterface;

/**
 * JavaScript bridge exposed as window.AndroidBridge inside the WebView.
 * Only Privacy Policy is wired here — all ad placements have been removed.
 */
public class AndroidBridge {

    private final MainActivity activity;

    public AndroidBridge(MainActivity activity) {
        this.activity = activity;
    }

    /**
     * Opens the Privacy Policy page in the device browser.
     * No URL parameter — the destination is a hardcoded constant in MainActivity.
     */
    @JavascriptInterface
    public void openPrivacyPolicy() {
        activity.openPrivacyPolicy();
    }
}
