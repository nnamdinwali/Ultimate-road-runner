package com.ultimateroadrunner.game;

import android.webkit.JavascriptInterface;
import com.appodeal.ads.Appodeal;

public class AppodealBridge {

    private final MainActivity activity;

    public AppodealBridge(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void showInterstitial() {
        activity.runOnUiThread(() -> activity.showInterstitialAd());
    }

    @JavascriptInterface
    public boolean isInterstitialReady() {
        return Appodeal.isLoaded(Appodeal.INTERSTITIAL);
    }
}
