package com.ultimateroadrunner.game;

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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "URR";
    private static final String APP_KEY = "d7441b7444df839562102f3e95a44793d98cd126509b5ce2";

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

        // Register the bridge so the game JS can reach ad and policy methods
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

                // Wrapper functions: the game calls window.showInterstitialAd() directly
                // (GDevelop omits the "AndroidBridge." prefix), so we bridge the gap here.
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

                // Inject a small floating Privacy Policy link in the bottom-left corner.
                // Guarded by an ID check so it is never added twice (e.g. on back-navigation).
                // The onclick calls openPrivacyPolicy() with NO URL argument — the destination
                // is a trusted constant inside the Java layer, not reachable from JS.
                fireJs(
                    "(function() {" +
                    "  if (document.getElementById('_pp_btn')) return;" +
                    "  var btn = document.createElement('a');" +
                    "  btn.id = '_pp_btn';" +
                    "  btn.innerText = 'Privacy Policy';" +
                    "  btn.href = 'javascript:void(0)';" +
                    "  btn.onclick = function(e) {" +
                    "    e.preventDefault();" +
                    "    if (window.AndroidBridge) window.AndroidBridge.openPrivacyPolicy();" +
                    "  };" +
                    "  btn.style.cssText = '" +
                    "    position:fixed;" +
                    "    bottom:6px;" +
                    "    left:8px;" +
                    "    z-index:99999;" +
                    "    font-size:10px;" +
                    "    color:rgba(255,255,255,0.55);" +
                    "    text-decoration:none;" +
                    "    font-family:sans-serif;" +
                    "    padding:2px 4px;" +
                    "    background:rgba(0,0,0,0.25);" +
                    "    border-radius:3px;" +
                    "    pointer-events:auto;" +
                    "  ';" +
                    "  document.body.appendChild(btn);" +
                    "})();"
                );
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/game/index.html");
    }

    private void initAppodeal() {
        try {
            Appodeal.setTesting(false);
            int adTypes = Appodeal.BANNER | Appodeal.INTERSTITIAL;
            Appodeal.initialize(this, APP_KEY, adTypes, initialized -> {
                appodealReady = true;
                Log.d(TAG, "Appodeal initialized, showing banner");
                runOnUiThread(() -> {
                    try {
                        Appodeal.show(this, Appodeal.BANNER);
                    } catch (Throwable t) {
                        Log.e(TAG, "showBanner after init: " + t);
                    }
                });
            });

            Appodeal.setBannerCallbacks(new BannerCallbacks() {
                public void onBannerLoaded(int h, boolean isPrecache) { Log.d(TAG, "Banner loaded h=" + h); }
                public void onBannerFailedToLoad() { Log.w(TAG, "Banner failed to load"); }
                public void onBannerShown() { Log.d(TAG, "Banner shown"); }
                public void onBannerShowFailed() { Log.w(TAG, "Banner show failed"); }
                public void onBannerClicked() { Log.d(TAG, "Banner clicked"); }
                public void onBannerExpired() { Log.d(TAG, "Banner expired"); }
            });

            Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
                public void onInterstitialLoaded(boolean isPrecache) { Log.d(TAG, "Interstitial loaded"); }
                public void onInterstitialFailedToLoad() { Log.w(TAG, "Interstitial failed to load"); }
                public void onInterstitialShown() {
                    Log.d(TAG, "Interstitial shown");
                    fireJs("if(window.onInterstitialAdShown) window.onInterstitialAdShown();");
                }
                public void onInterstitialShowFailed() {
                    Log.w(TAG, "Interstitial show failed");
                    fireJs("if(window.onInterstitialAdFailed) window.onInterstitialAdFailed();");
                }
                public void onInterstitialClicked() { Log.d(TAG, "Interstitial clicked"); }
                public void onInterstitialClosed() {
                    Log.d(TAG, "Interstitial closed");
                    fireJs("if(window.onInterstitialAdClosed) window.onInterstitialAdClosed();");
                }
                public void onInterstitialExpired() { Log.d(TAG, "Interstitial expired"); }
            });

        } catch (Throwable t) {
            Log.e(TAG, "initAppodeal error: " + t);
        }
    }

    void showBanner() {
        if (!appodealReady) return;
        runOnUiThread(() -> {
            try { Appodeal.show(this, Appodeal.BANNER); }
            catch (Throwable t) { Log.e(TAG, "showBanner: " + t); }
        });
    }

    void hideBanner() {
        if (!appodealReady) return;
        runOnUiThread(() -> {
            try { Appodeal.hide(this, Appodeal.BANNER); }
            catch (Throwable t) { Log.e(TAG, "hideBanner: " + t); }
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

    /**
     * Opens the privacy policy page in the device's default browser.
     * Called by AndroidBridge.openPrivacyPolicy() — no URL comes from JS.
     */
    void openPrivacyPolicy() {
        runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Log.w(TAG, "Could not open privacy policy: " + e.getMessage());
            }
        });
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
