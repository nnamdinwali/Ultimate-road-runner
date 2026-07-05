package com.ultimateroadrunner.game;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerCallbacks;
import com.appodeal.ads.InterstitialCallbacks;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "URR";
    private static final String APP_KEY = "d7441b7444df839562102f3e95a44793d98cd126509b5ce2";
    private WebView webView;
    private boolean appodealReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWebView();
        // Delay Appodeal init slightly so WebView loads first
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                runOnUiThread(this::initAppodeal);
            } catch (Throwable t) {
                Log.e(TAG, "Init thread error: " + t.getMessage());
            }
        }).start();
    }

    private void initWebView() {
        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);

        // FIX 1: Register the bridge with the WebView.
        // Without this line the game JS cannot reach any ad methods.
        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, android.webkit.WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // FIX 2: The game calls window.showInterstitialAd() directly (not window.AndroidBridge.showInterstitialAd()).
                // Inject thin wrappers so both calling styles work.
                fireJs(
                    "window.showInterstitialAd = function() {" +
                    "  if (window.AndroidBridge) window.AndroidBridge.showInterstitialAd();" +
                    "};" +
                    "window.showBanner = function() {" +
                    "  if (window.AndroidBridge) window.AndroidBridge.showBanner();" +
                    "};" +
                    "window.hideBanner = function() {" +
                    "  if (window.AndroidBridge) window.AndroidBridge.hideBanner();" +
                    "};" +
                    "window.showRewardedAd = function() {" +
                    "  if (window.AndroidBridge) window.AndroidBridge.showRewardedAd();" +
                    "};" +
                    "if (typeof window._startAdTimers === 'function') window._startAdTimers();"
                );
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/game/index.html");
    }

    private void initAppodeal() {
        try {
            Appodeal.setTesting(false);
            // Only init the ad types the game actually uses
            Appodeal.initialize(this, APP_KEY, Appodeal.BANNER | Appodeal.INTERSTITIAL);
            appodealReady = true;
            setupCallbacks();
            // Show banner immediately at bottom of screen — game expects this automatically
            Appodeal.show(this, Appodeal.BANNER);
            Log.d(TAG, "Appodeal ready");
        } catch (Throwable t) {
            Log.e(TAG, "Appodeal init failed: " + t);
        }
    }

    private void setupCallbacks() {
        Appodeal.setBannerCallbacks(new BannerCallbacks() {
            @Override public void onBannerLoaded(int height, boolean isPrecache) {
                Log.d(TAG, "Banner loaded h=" + height);
                runOnUiThread(() -> fireJs("if(window.onBannerAdLoaded) window.onBannerAdLoaded();"));
            }
            @Override public void onBannerFailedToLoad() {
                Log.d(TAG, "Banner failed to load");
                runOnUiThread(() -> fireJs("if(window.onBannerAdFailed) window.onBannerAdFailed();"));
            }
            @Override public void onBannerShown() { Log.d(TAG, "Banner shown"); }
            @Override public void onBannerShowFailed() {
                runOnUiThread(() -> fireJs("if(window.onBannerAdFailed) window.onBannerAdFailed();"));
            }
            @Override public void onBannerClicked() {}
            @Override public void onBannerExpired() {}
        });

        Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
            @Override public void onInterstitialLoaded(boolean isPrecache) {
                Log.d(TAG, "Interstitial loaded isPrecache=" + isPrecache);
            }
            @Override public void onInterstitialFailedToLoad() {
                Log.d(TAG, "Interstitial failed to load");
            }
            @Override public void onInterstitialShown() { Log.d(TAG, "Interstitial shown"); }
            @Override public void onInterstitialShowFailed() {
                runOnUiThread(() -> fireJs("if(window.onInterstitialAdFailed) window.onInterstitialAdFailed();"));
            }
            @Override public void onInterstitialClicked() {}
            @Override public void onInterstitialClosed() {
                Log.d(TAG, "Interstitial closed");
                runOnUiThread(() -> fireJs("if(window.onInterstitialAdClosed) window.onInterstitialAdClosed();"));
            }
            @Override public void onInterstitialExpired() {}
        });
    }

    // --- Methods called by AndroidBridge (via @JavascriptInterface) ---

    void showBanner() {
        if (!appodealReady) return;
        runOnUiThread(() -> {
            try {
                Appodeal.show(this, Appodeal.BANNER);
            } catch (Throwable t) {
                Log.e(TAG, "showBanner: " + t);
            }
        });
    }

    void hideBanner() {
        if (!appodealReady) return;
        runOnUiThread(() -> {
            try {
                Appodeal.hide(this, Appodeal.BANNER);
            } catch (Throwable t) {
                Log.e(TAG, "hideBanner: " + t);
            }
        });
    }

    void showInterstitialAd() {
        if (!appodealReady) {
            fireJs("if(window.onInterstitialAdFailed) window.onInterstitialAdFailed();");
            return;
        }
        runOnUiThread(() -> {
            try {
                if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                    Appodeal.show(this, Appodeal.INTERSTITIAL);
                } else {
                    Log.d(TAG, "Interstitial not loaded yet");
                    fireJs("if(window.onInterstitialAdFailed) window.onInterstitialAdFailed();");
                }
            } catch (Throwable t) {
                Log.e(TAG, "showInterstitial: " + t);
                fireJs("if(window.onInterstitialAdFailed) window.onInterstitialAdFailed();");
            }
        });
    }

    void showRewardedAd() {
        // Game doesn't use rewarded — silently fail
        fireJs("if(window.onRewardedAdFailed) window.onRewardedAdFailed();");
    }

    // Safely runs JavaScript on the UI thread
    private void fireJs(String js) {
        if (webView == null) return;
        webView.post(() -> webView.evaluateJavascript(js, null));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) webView.destroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
