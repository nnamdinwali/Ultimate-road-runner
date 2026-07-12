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
                // in index.html and must NOT be overridden here — they correctly wire
                // to _adEmitter and _unmuteAfterAd() without ever pausing the engine.
                // _startAdTimers() is also called by index.html's body script and must
                // NOT be called again here (double-call = double death listeners = two
                // ads per death).
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
                    "};"
                );

                // Privacy Policy floating button.
                fireJs(
                    "(function() {" +
                    "  var btn = document.getElementById('_pp_btn');" +
                    "  if (btn) btn.remove();" +
                    "  btn = document.createElement('div');" +
                    "  btn.id = '_pp_btn';" +
                    "  btn.innerText = 'Privacy Policy';" +
                    "  btn.style.cssText = 'position:fixed; bottom:10px; left:10px; z-index:2147483647; font-size:12px; color:white; background:rgba(0,0,0,0.5); padding:5px 10px; border-radius:5px; cursor:pointer; pointer-events:auto;';" +
                    "  var handler = function(e) {" +
                    "    e.preventDefault(); e.stopPropagation();" +
                    "    if (window.AndroidBridge) window.AndroidBridge.openPrivacyPolicy();" +
                    "  };" +
                    "  btn.addEventListener('click', handler, true);" +
                    "  btn.addEventListener('touchstart', handler, true);" +
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
                    // Preload the interstitial immediately so it's already sitting
                    // in memory before the player ever dies - same pattern the old
                    // build used with AdMob (load once at start, reload after every
                    // show/close/fail so there's never a wait at show-time).
                    try {
                        Appodeal.cache(this, Appodeal.INTERSTITIAL);
                    } catch (Throwable t) {
                        Log.e(TAG, "precache interstitial: " + t);
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
                public void onInterstitialFailedToLoad() {
                    Log.w(TAG, "Interstitial failed to load");
                    try { Appodeal.cache(MainActivity.this, Appodeal.INTERSTITIAL); } catch (Throwable t) {}
                }
                public void onInterstitialShown() {
                    Log.d(TAG, "Interstitial shown");
                    // index.html does not define onInterstitialAdShown — nothing to call.
                }
                public void onInterstitialShowFailed() {
                    Log.w(TAG, "Interstitial show failed");
                    // index.html's onInterstitialAdFailed calls _unmuteAfterAd() + emits event.
                    fireJs("if(typeof window.onInterstitialAdFailed === 'function') window.onInterstitialAdFailed();");
                    try { Appodeal.cache(MainActivity.this, Appodeal.INTERSTITIAL); } catch (Throwable t) {}
                }
                public void onInterstitialClicked() { Log.d(TAG, "Interstitial clicked"); }
                public void onInterstitialClosed() {
                    Log.d(TAG, "Interstitial closed");
                    // index.html's onInterstitialAdClosed calls _unmuteAfterAd() + emits event.
                    fireJs("if(typeof window.onInterstitialAdClosed === 'function') window.onInterstitialAdClosed();");
                    try { Appodeal.cache(MainActivity.this, Appodeal.INTERSTITIAL); } catch (Throwable t) {}
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
            fireJs("if(typeof window.onInterstitialAdFailed === 'function') window.onInterstitialAdFailed();");
            return;
        }
        runOnUiThread(() -> {
            try {
                if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                    Appodeal.show(this, Appodeal.INTERSTITIAL);
                } else {
                    Log.d(TAG, "Interstitial not loaded yet");
                    fireJs("if(typeof window.onInterstitialAdFailed === 'function') window.onInterstitialAdFailed();");
                }
            } catch (Throwable t) {
                Log.e(TAG, "showInterstitial: " + t);
                fireJs("if(typeof window.onInterstitialAdFailed === 'function') window.onInterstitialAdFailed();");
            }
        });
    }

    void showRewardedAd() {
        fireJs("if(typeof window.onRewardedAdFailed === 'function') window.onRewardedAdFailed();");
    }

    /**
     * Opens the privacy policy in the device browser.
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
