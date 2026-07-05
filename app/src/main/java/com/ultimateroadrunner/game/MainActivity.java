package com.ultimateroadrunner.game;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerCallbacks;
import com.appodeal.ads.InterstitialCallbacks;
import com.appodeal.ads.RewardedVideoCallbacks;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String APP_KEY = "d7441b7444df839562102f3e95a44793d98cd126509b5ce2";
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
                Log.e(TAG, "Appodeal thread error: " + t.getMessage());
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
        
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                fireJs("if (window._startAdTimers) window._startAdTimers();");
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/game/index.html");
    }

    private void initAppodeal() {
        try {
            Appodeal.setTesting(false);
            
            // MODULAR SETUP: No need to disable networks via code if they aren't in the project
            Appodeal.initialize(this, APP_KEY,
                    Appodeal.BANNER | Appodeal.INTERSTITIAL | Appodeal.REWARDED_VIDEO);
            
            appodealReady = true;
            setupCallbacks();
            Log.d(TAG, "Appodeal initialized with Pure Modular Setup");
        } catch (Throwable t) {
            Log.e(TAG, "Appodeal init failed: " + t);
        }
    }

    private void setupCallbacks() {
        try {
            Appodeal.setBannerCallbacks(new BannerCallbacks() {
                @Override public void onBannerLoaded(int height, boolean isPrecache) {
                    runOnUiThread(() -> fireJs("window.onBannerAdLoaded && window.onBannerAdLoaded()"));
                }
                @Override public void onBannerFailedToLoad() {
                    runOnUiThread(() -> fireJs("window.onBannerAdFailed && window.onBannerAdFailed()"));
                }
                @Override public void onBannerShown() {}
                @Override public void onBannerShowFailed() {
                    runOnUiThread(() -> fireJs("window.onBannerAdFailed && window.onBannerAdFailed()"));
                }
                @Override public void onBannerClicked() {}
                @Override public void onBannerExpired() {}
            });
        } catch (Throwable t) { Log.e(TAG, "Banner callbacks: " + t); }

        try {
            Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
                @Override public void onInterstitialLoaded(boolean isPrecache) {
                    runOnUiThread(() -> fireJs("window.onInterstitialAdLoaded && window.onInterstitialAdLoaded()"));
                }
                @Override public void onInterstitialFailedToLoad() {
                    runOnUiThread(() -> fireJs("window.onInterstitialAdFailed && window.onInterstitialAdFailed()"));
                }
                @Override public void onInterstitialShown() {}
                @Override public void onInterstitialShowFailed() {
                    runOnUiThread(() -> fireJs("window.onInterstitialAdFailed && window.onInterstitialAdFailed()"));
                }
                @Override public void onInterstitialClicked() {}
                @Override public void onInterstitialClosed() {
                    runOnUiThread(() -> fireJs("window.onInterstitialAdClosed && window.onInterstitialAdClosed()"));
                }
                @Override public void onInterstitialExpired() {}
            });
        } catch (Throwable t) { Log.e(TAG, "Interstitial callbacks: " + t); }

        try {
            Appodeal.setRewardedVideoCallbacks(new RewardedVideoCallbacks() {
                @Override public void onRewardedVideoLoaded(boolean isPrecache) {
                    runOnUiThread(() -> fireJs("window.onRewardedAdLoaded && window.onRewardedAdLoaded()"));
                }
                @Override public void onRewardedVideoFailedToLoad() {
                    runOnUiThread(() -> fireJs("window.onRewardedAdFailed && window.onRewardedAdFailed()"));
                }
                @Override public void onRewardedVideoShown() {}
                @Override public void onRewardedVideoShowFailed() {
                    runOnUiThread(() -> fireJs("window.onRewardedAdFailed && window.onRewardedAdFailed()"));
                }
                @Override public void onRewardedVideoClicked() {}
                @Override public void onRewardedVideoFinished(double amount, String currency) {
                    runOnUiThread(() -> fireJs("window.onRewardedAdRewarded && window.onRewardedAdRewarded()"));
                }
                @Override public void onRewardedVideoClosed(boolean finished) {
                    runOnUiThread(() -> fireJs("window.onRewardedAdClosed && window.onRewardedAdClosed()"));
                }
                @Override public void onRewardedVideoExpired() {}
            });
        } catch (Throwable t) { Log.e(TAG, "Rewarded callbacks: " + t); }
    }

    void fireJs(String js) {
        if (webView != null) {
            webView.evaluateJavascript(js, null);
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
            try {
                Appodeal.hide(this, Appodeal.BANNER);
                fireJs("window.onBannerAdHidden && window.onBannerAdHidden()");
            } catch (Throwable t) { Log.e(TAG, "hideBanner: " + t); }
        });
    }

    void showInterstitialAd() {
        if (!appodealReady) {
            fireJs("window.onInterstitialAdFailed && window.onInterstitialAdFailed()");
            return;
        }
        runOnUiThread(() -> {
            try {
                if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                    Appodeal.show(this, Appodeal.INTERSTITIAL);
                } else {
                    fireJs("window.onInterstitialAdFailed && window.onInterstitialAdFailed()");
                }
            } catch (Throwable t) {
                Log.e(TAG, "showInterstitial: " + t);
                fireJs("window.onInterstitialAdFailed && window.onInterstitialAdFailed()");
            }
        });
    }

    void showRewardedAd() {
        if (!appodealReady) {
            fireJs("window.onRewardedAdFailed && window.onRewardedAdFailed()");
            return;
        }
        runOnUiThread(() -> {
            try {
                if (Appodeal.isLoaded(Appodeal.REWARDED_VIDEO)) {
                    Appodeal.show(this, Appodeal.REWARDED_VIDEO);
                } else {
                    fireJs("window.onRewardedAdFailed && window.onRewardedAdFailed()");
                }
            } catch (Throwable t) {
                Log.e(TAG, "showRewarded: " + t);
                fireJs("window.onRewardedAdFailed && window.onRewardedAdFailed()");
            }
        });
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
