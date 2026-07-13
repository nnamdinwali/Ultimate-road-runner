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

    private static final String PRIVACY_POLICY_URL =
            "https://nnamdinwali.github.io/ultimate-road-runner-privacy/";

    // Game is in app/src/main/assets/game/index.html
    // WebViewAssetLoader maps /assets/ -> assets folder, so URL is /assets/game/index.html
    private static final String GAME_URL =
            "https://appassets.androidplatform.net/assets/game/index.html";

    private WebView webView;

    /**
     * Set to true when showMREC() is called but fill isn't loaded yet.
     * onMrecLoaded() auto-shows once fill arrives.
     * Cleared by hideMREC() so Retry cancels a pending MREC correctly.
     */
    private volatile boolean mrecPending = false;

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
                // Inject thin pass-through stubs so any GDevelop event that calls
                // window.showInterstitialAd() etc. directly still reaches AndroidBridge.
                // Callbacks (onInterstitialAdClosed, etc.) are defined in index.html.
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
            }
        });

        // Single load — do NOT call loadUrl again inside onPageFinished (causes infinite loop)
        webView.loadUrl(GAME_URL);
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
            @Override public void onMrecLoaded(boolean isPrecache) {
                Log.d(TAG, "MREC loaded, mrecPending=" + mrecPending);
                if (mrecPending) {
                    mrecPending = false;
                    runOnUiThread(() -> Appodeal.show(MainActivity.this, Appodeal.MREC));
                }
            }
            @Override public void onMrecFailedToLoad() {
                Log.d(TAG, "MREC failed to load, retrying cache");
                Appodeal.cache(MainActivity.this, Appodeal.MREC);
            }
            @Override public void onMrecShown() {
                Log.d(TAG, "MREC shown — re-caching for next death");
                Appodeal.cache(MainActivity.this, Appodeal.MREC);
            }
            @Override public void onMrecShowFailed() { Log.d(TAG, "MREC show failed"); }
            @Override public void onMrecClicked() { Log.d(TAG, "MREC clicked"); }
            @Override public void onMrecExpired() { Log.d(TAG, "MREC expired"); }
        });

        Appodeal.cache(this, Appodeal.INTERSTITIAL);
        Appodeal.cache(this, Appodeal.MREC);
    }

    void showInterstitialAd() {
        runOnUiThread(() -> {
            if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                Appodeal.show(this, Appodeal.INTERSTITIAL);
            } else {
                Log.d(TAG, "Interstitial not loaded — firing failed");
                fireJs("if(window.onInterstitialAdFailed)onInterstitialAdFailed();");
            }
        });
    }

    void showRewardedAd() {
        runOnUiThread(() -> {
            Log.d(TAG, "Rewarded not implemented — firing failed");
            fireJs("if(window.onRewardedAdFailed)onRewardedAdFailed();");
        });
    }

    void showBanner() {
        runOnUiThread(() -> Appodeal.show(this, Appodeal.BANNER_BOTTOM));
    }

    void hideBanner() {
        runOnUiThread(() -> Appodeal.hide(this, Appodeal.BANNER_BOTTOM));
    }

    void showMREC() {
        runOnUiThread(() -> {
            if (Appodeal.isLoaded(Appodeal.MREC)) {
                mrecPending = false;
                Appodeal.show(this, Appodeal.MREC);
                Log.d(TAG, "showMREC: fill ready, showing");
            } else {
                mrecPending = true;
                Log.d(TAG, "showMREC: fill not ready, mrecPending=true");
            }
        });
    }

    void hideMREC() {
        runOnUiThread(() -> {
            mrecPending = false;
            Appodeal.hide(this, Appodeal.MREC);
            Log.d(TAG, "hideMREC: pending cleared, hide() called");
        });
    }

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
        // Deliberately NOT calling webView.onPause() — that froze the WebView JS clock
        // whenever an ad Activity covered this one (the original "freeze on death" bug).
    }

    @Override
    protected void onResume() {
        super.onResume();
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
