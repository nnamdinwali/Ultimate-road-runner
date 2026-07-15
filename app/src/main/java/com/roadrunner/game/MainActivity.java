package com.roadrunner.game;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "URR";

    // TODO: Replace with your real Yandex Ad Unit IDs from partner.yandex.com
    // Use "demo-interstitial-yandex" and "demo-banner-yandex" for initial testing
    private static final String INTERSTITIAL_ID = "demo-interstitial-yandex";
    private static final String BANNER_ID       = "demo-banner-yandex";

    private static final String PRIVACY_POLICY_URL =
            "https://nnamdinwali.github.io/ultimate-road-runner-privacy/";

    private WebView webView;
    private BannerAdView bannerAdView;
    private InterstitialAd interstitialAd;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWebView();
        initYandex();
    }

    // onPause and onResume deliberately do NOT touch webView.onPause/onResume.
    // Calling webView.onPause() whenever an Activity covers this one (including
    // Yandex's own interstitial Activity) was the root cause of the
    // freeze-on-death bug — it halted the WebView's JS clock and left the game
    // waiting for a callback that might never arrive.
    // The game engine must never be paused by any ad or lifecycle event.

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bannerAdView != null) {
            bannerAdView.destroy();
        }
        if (webView != null) {
            webView.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ── WebView setup ────────────────────────────────────────────────────────

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
                // Privacy Policy floating button — injected after page load so it
                // always sits on top of the game canvas.
                fireJs(
                    "(function(){" +
                    "  var btn=document.getElementById('_pp_btn');if(btn)btn.remove();" +
                    "  btn=document.createElement('div');" +
                    "  btn.id='_pp_btn';btn.innerText='Privacy Policy';" +
                    "  btn.style.cssText='position:fixed;bottom:10px;left:10px;" +
                        "z-index:2147483647;font-size:12px;color:white;" +
                        "background:rgba(0,0,0,0.5);padding:5px 10px;" +
                        "border-radius:5px;cursor:pointer;pointer-events:auto;';" +
                    "  var h=function(e){e.preventDefault();e.stopPropagation();" +
                    "    if(window.AndroidBridge)window.AndroidBridge.openPrivacyPolicy();};" +
                    "  btn.addEventListener('click',h,true);" +
                    "  btn.addEventListener('touchstart',h,true);" +
                    "  document.body.appendChild(btn);" +
                    "})();"
                );
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/game/index.html");
    }

    // ── Yandex Ads init ──────────────────────────────────────────────────────

    private void initYandex() {
        MobileAds.initialize(this, () -> {
            Log.d(TAG, "Yandex Ads initialized");
            mainHandler.post(() -> {
                loadInterstitial(); // preload immediately so it's ready before first death
                loadBanner();
            });
        });
    }

    // ── Interstitial ─────────────────────────────────────────────────────────

    /**
     * Pre-loads the next interstitial ad in the background.
     * Called on init, and immediately after every close/fail so there is always
     * an ad sitting ready in memory before the player next dies — the same
     * pattern the old AdMob build used, which is why that one never had a
     * show-time delay or freeze.
     */
    private void loadInterstitial() {
        InterstitialAdLoader loader = new InterstitialAdLoader(this);
        loader.setAdLoadListener(new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                Log.d(TAG, "Interstitial preloaded and ready");
                interstitialAd = ad;
                interstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                    @Override public void onAdShown() {
                        Log.d(TAG, "Interstitial shown");
                        fireJs("if(window.onInterstitialAdShown) window.onInterstitialAdShown();");
                    }
                    @Override public void onAdFailedToShow(AdError error) {
                        Log.w(TAG, "Interstitial failed to show: " + error.getDescription());
                        interstitialAd = null;
                        fireJs("if(window.onInterstitialAdFailed) window.onInterstitialAdFailed();");
                        loadInterstitial(); // preload next immediately
                    }
                    @Override public void onAdDismissed() {
                        Log.d(TAG, "Interstitial dismissed");
                        interstitialAd = null;
                        fireJs("if(window.onInterstitialAdClosed) window.onInterstitialAdClosed();");
                        loadInterstitial(); // preload next immediately
                    }
                    @Override public void onAdClicked() {}
                    @Override public void onAdImpression(@Nullable ImpressionData data) {}
                });
            }

            @Override
            public void onAdFailedToLoad(AdRequestError error) {
                Log.w(TAG, "Interstitial failed to load: " + error.getDescription());
                interstitialAd = null;
                // Retry after 30 s so we're ready well before the next death
                mainHandler.postDelayed(MainActivity.this::loadInterstitial, 30_000);
            }
        });
        loader.loadAd(new AdRequestConfiguration.Builder(INTERSTITIAL_ID).build());
    }

    void showInterstitialAd() {
        mainHandler.post(() -> {
            if (interstitialAd != null) {
                interstitialAd.show(this);
            } else {
                // Ad not ready yet — tell JS immediately so the game keeps moving.
                Log.d(TAG, "Interstitial not ready, failing gracefully");
                fireJs("if(window.onInterstitialAdFailed) window.onInterstitialAdFailed();");
                loadInterstitial(); // kick off a fresh load for next time
            }
        });
    }

    void showRewardedAd() {
        // Not implemented — fail gracefully so the game never gets stuck.
        fireJs("if(window.onRewardedAdFailed) window.onRewardedAdFailed();");
    }

    // ── Banner ───────────────────────────────────────────────────────────────

    private void loadBanner() {
        FrameLayout container = findViewById(R.id.bannerContainer);
        if (container == null) return;

        bannerAdView = new BannerAdView(this);
        bannerAdView.setAdUnitId(BANNER_ID);
        bannerAdView.setAdSize(BannerAdSize.stickySize(this, 320));

        container.removeAllViews();
        container.addView(bannerAdView);
        bannerAdView.loadAd(new AdRequest.Builder().build());
    }

    void showBanner() {
        mainHandler.post(() -> {
            FrameLayout c = findViewById(R.id.bannerContainer);
            if (c != null) c.setVisibility(View.VISIBLE);
        });
    }

    void hideBanner() {
        mainHandler.post(() -> {
            FrameLayout c = findViewById(R.id.bannerContainer);
            if (c != null) c.setVisibility(View.GONE);
        });
    }

    // ── Privacy Policy ───────────────────────────────────────────────────────

    void openPrivacyPolicy() {
        mainHandler.post(() -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)));
            } catch (Exception e) {
                Log.w(TAG, "Could not open privacy policy: " + e.getMessage());
            }
        });
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private void fireJs(String js) {
        if (webView == null) return;
        webView.post(() -> webView.evaluateJavascript(js, null));
    }
}
