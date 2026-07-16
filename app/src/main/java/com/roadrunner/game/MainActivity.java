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
import android.widget.TextView;

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
        initPrivacyPolicyButton(); // native view — GDevelop cannot steal its clicks
        initYandex();
    }

    // onPause / onResume deliberately do NOT touch webView.onPause/onResume.
    // Calling webView.onPause() whenever an Activity covered this one
    // (including Yandex's interstitial Activity) was the root cause of the
    // freeze-on-death bug. The game engine must never be paused.

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bannerAdView != null) bannerAdView.destroy();
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

    // ── Privacy Policy (native overlay — GDevelop-proof) ─────────────────────

    /**
     * The Privacy Policy button is a plain Android TextView declared in the
     * layout XML and stacked above the WebView in the native view hierarchy.
     * Because it is a sibling of the WebView (not a child), Android dispatches
     * touch events to it BEFORE the WebView ever sees them — GDevelop's canvas
     * cannot intercept or suppress them regardless of what JS event listeners
     * it registers.
     */
    private void initPrivacyPolicyButton() {
        TextView btn = findViewById(R.id.privacyPolicyBtn);
        if (btn == null) return;
        btn.setOnClickListener(v -> openPrivacyPolicy());
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
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/game/index.html");
    }

    // ── Yandex Ads init ──────────────────────────────────────────────────────

    private void initYandex() {
        MobileAds.initialize(this, () -> {
            Log.d(TAG, "Yandex Ads initialized");
            mainHandler.post(() -> {
                loadInterstitial();
                loadBanner();
            });
        });
    }

    // ── Interstitial ─────────────────────────────────────────────────────────

    private void loadInterstitial() {
        InterstitialAdLoader loader = new InterstitialAdLoader(this);
        loader.setAdLoadListener(new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(InterstitialAd ad) {
                Log.d(TAG, "Interstitial preloaded and ready");
                interstitialAd = ad;
                interstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                    @Override public void onAdShown() {
                        fireJs("if(window.onInterstitialAdShown) window.onInterstitialAdShown();");
                    }
                    @Override public void onAdFailedToShow(AdError error) {
                        Log.w(TAG, "Interstitial failed to show: " + error.getDescription());
                        interstitialAd = null;
                        fireJs("if(window.onInterstitialAdFailed) window.onInterstitialAdFailed();");
                        loadInterstitial();
                    }
                    @Override public void onAdDismissed() {
                        Log.d(TAG, "Interstitial dismissed");
                        interstitialAd = null;
                        fireJs("if(window.onInterstitialAdClosed) window.onInterstitialAdClosed();");
                        loadInterstitial();
                    }
                    @Override public void onAdClicked() {}
                    @Override public void onAdImpression(@Nullable ImpressionData data) {}
                });
            }

            @Override
            public void onAdFailedToLoad(AdRequestError error) {
                Log.w(TAG, "Interstitial failed to load: " + error.getDescription());
                interstitialAd = null;
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
                Log.d(TAG, "Interstitial not ready, failing gracefully");
                fireJs("if(window.onInterstitialAdFailed) window.onInterstitialAdFailed();");
                loadInterstitial();
            }
        });
    }

    void showRewardedAd() {
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
