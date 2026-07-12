package com.roadrunner.game;

import android.content.Intent;
import android.net.Uri;
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
import com.appodeal.ads.MrecCallbacks;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "URR";
    private static final String APP_KEY = "784f1feaa9b08dbfa4a8177fd8274436ec8f04f4f9eb9322";

    /**
     * The ONLY URL this app will ever open via the bridge.
     * Hardcoded here so JS cannot influence the destination.
     */
    private static final String PRIVACY_POLICY_URL =
            "https://nnamdinwali.github.io/ultimate-road-runner-privacy/";

    private WebView webView;
    private boolean appodealReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWebView();
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

                // Inject thin pass-through stubs so any legacy GDevelop event that
                // calls window.showInterstitialAd() / showBanner() / etc. directly
                // still reaches AndroidBridge. The actual ad lifecycle callbacks
                // (onInterstitialAdClosed, onInterstitialAdFailed, etc.) are defined
                // in index.html and must NOT be overridden here.
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
                    "window.showMREC = function() {" +
                    "  if (window.AndroidBridge) window.AndroidBridge.showMREC();" +
                    "};" +
                    "window.hideMREC = function() {" +
                    "  if (window.AndroidBridge) window.AndroidBridge.hideMREC();" +
                    "};" +
                    "window.openPrivacyPolicy = function() {" +
                    "  if (window.AndroidBridge) window.AndroidBridge.openPrivacyPolicy();" +
                    "};"
                );

                webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
    }

    private void initAppodeal() {
        Appodeal.setLogLevel(com.appodeal.ads.utils.Log.LogLevel.verbose);
        Appodeal.setTesting(false);
        Appodeal.initialize(this, APP_KEY,
                Appodeal.INTERSTITIAL | Appodeal.BANNER | Appodeal.MREC);

        Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
            @Override public void onInterstitialLoaded(boolean isPrecache) { Log.d(TAG, "Interstitial loaded"); }
            @Override public void onInterstitialFailedToLoad() {
                Log.d(TAG, "Interstitial failed to load");
                fireJs("if(window.onInterstitialAdFailed)onInterstitialAdFailed();");
            }
            @Override public void onInterstitialShown() { Log.d(TAG, "Interstitial shown"); }
            @Override public void onInterstitialShowFailed() {
                Log.d(TAG, "Interstitial show failed");
                fireJs("if(window.onInterstitialAdFailed)onInterstitialAdFailed();");
            }
            @Override public void onInterstitialClicked() { Log.d(TAG, "Interstitial clicked"); }
            @Override public void onInterstitialClosed() {
                Log.d(TAG, "Interstitial closed");
                fireJs("if(window.onInterstitialAdClosed)onInterstitialAdClosed();");
            }
            @Override public void onInterstitialExpired() { Log.d(TAG, "Interstitial expired"); }
        });

        Appodeal.setBannerCallbacks(new BannerCallbacks() {
            @Override public void onBannerLoaded(int height, boolean isPrecache) { Log.d(TAG, "Banner loaded"); }
            @Override public void onBannerFailedToLoad() { Log.d(TAG, "Banner failed to load"); }
            @Override public void onBannerShown() { Log.d(TAG, "Banner shown"); }
            @Override public void onBannerShowFailed() { Log.d(TAG, "Banner show failed"); }
            @Override public void onBannerClicked() { Log.d(TAG, "Banner clicked"); }
            @Override public void onBannerExpired() { Log.d(TAG, "Banner expired"); }
        });

        Appodeal.setMrecCallbacks(new MrecCallbacks() {
            @Override public void onMrecLoaded(boolean isPrecache) { Log.d(TAG, "MREC loaded"); }
            @Override public void onMrecFailedToLoad() { Log.d(TAG, "MREC failed to load"); }
            @Override public void onMrecShown() {
                Log.d(TAG, "MREC shown");
                // Re-cache immediately so next death has fill ready
                Appodeal.cache(MainActivity.this, Appodeal.MREC);
            }
            @Override public void onMrecShowFailed() { Log.d(TAG, "MREC show failed"); }
            @Override public void onMrecClicked() { Log.d(TAG, "MREC clicked"); }
            @Override public void onMrecExpired() { Log.d(TAG, "MREC expired"); }
        });

        // Pre-cache for first death
        Appodeal.cache(this, Appodeal.INTERSTITIAL);
        Appodeal.cache(this, Appodeal.MREC);

        appodealReady = true;
    }

    // Called by AndroidBridge.showInterstitialAd()
    void showInterstitialAd() {
        runOnUiThread(() -> {
            if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                Appodeal.show(this, Appodeal.INTERSTITIAL);
            } else {
                Log.d(TAG, "Interstitial not loaded, firing failed callback");
                fireJs("if(window.onInterstitialAdFailed)onInterstitialAdFailed();");
            }
        });
    }

    // Called by AndroidBridge.showRewardedAd() — not yet implemented, fire failed
    void showRewardedAd() {
        runOnUiThread(() -> {
            Log.d(TAG, "Rewarded ad requested but not implemented");
            fireJs("if(window.onRewardedAdFailed)onRewardedAdFailed();");
        });
    }

    void showBanner() {
        runOnUiThread(() -> Appodeal.show(this, Appodeal.BANNER_BOTTOM));
    }

    void hideBanner() {
        runOnUiThread(() -> Appodeal.hide(this, Appodeal.BANNER_BOTTOM));
    }

    /**
     * Show the MREC ad as a floating overlay (same mechanism as banner).
     * Appodeal.show(activity, Appodeal.MREC) renders as a floating 300x250 view
     * managed entirely by the Appodeal SDK — no AppodealView XML widget needed.
     * (AppodealView does not exist in Appodeal 3.3.1.)
     */
    void showMREC() {
        runOnUiThread(() -> {
            if (Appodeal.isLoaded(Appodeal.MREC)) {
                Appodeal.show(this, Appodeal.MREC);
                Log.d(TAG, "showMREC: show() called");
            } else {
                Log.d(TAG, "showMREC: not loaded yet, caching");
                Appodeal.cache(this, Appodeal.MREC);
            }
        });
    }

    void hideMREC() {
        runOnUiThread(() -> {
            Appodeal.hide(this, Appodeal.MREC);
            Log.d(TAG, "hideMREC: hide() called");
        });
    }

    /**
     * The ONLY URL this app will ever open via the bridge.
     * No URL comes from JS — destination is this hardcoded constant only.
     */
    void openPrivacyPolicy() {
        runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL));
                startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Could not open privacy policy: " + e.getMessage());
            }
        });
    }

    private void fireJs(String js) {
        if (webView == null) return;
        webView.post(() -> webView.evaluateJavascript(js, null));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Deliberately NOT calling webView.onPause() here. That call froze the
        // WebView's JS/animation clock any time ANY Activity briefly covered
        // this one (including Appodeal's own interstitial Activity), and the
        // clock only resumed on the next onResume — which is exactly what
        // produced the "game freezes on death" bug. The game must never be
        // pausable by an Activity transition or an ad SDK callback again.
    }

    @Override
    protected void onResume() {
        super.onResume();
        // No webView.onResume() call needed since we never pause it above.
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
