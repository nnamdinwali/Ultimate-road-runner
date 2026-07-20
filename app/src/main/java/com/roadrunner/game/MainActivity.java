package com.roadrunner.game;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestConfiguration;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.MobileAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "URR";

    private static final String PRIVACY_POLICY_URL =
            "https://nnamdinwali.github.io/ultimate-road-runner-privacy/";

    // ─────────────────────────────────────────────────────────────────────────
    // TODO: Replace these with your real Yandex Ad Unit IDs from
    //       https://partner2.yandex.ru/  (App ID: 19994035)
    //       Format: R-M-XXXXXXXX-X
    // ─────────────────────────────────────────────────────────────────────────
    private static final String BANNER_AD_UNIT_ID       = "R-M-19594035-2";
    private static final String INTERSTITIAL_AD_UNIT_ID = "R-M-19594035-1";
    private static final String REWARDED_AD_UNIT_ID     = "R-M-19594035-4";
    private static final String APP_OPEN_AD_UNIT_ID     = "R-M-19594035-3";

    WebView webView;  // package-private so AndroidBridge can call evaluateJavascript
    private BannerAdView bannerAdView;

    private InterstitialAdLoader interstitialLoader;
    private InterstitialAd       interstitialAd;

    private RewardedAdLoader rewardedLoader;
    private RewardedAd       rewardedAd;

    // Reward callback to run after a rewarded ad completes
    private Runnable pendingRewardCallback;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, () -> {
            Log.d(TAG, "Yandex MobileAds SDK initialized");
            loadInterstitialAd();
            loadRewardedAd();
        });

        initWebView();
        initBannerAd();
        initPrivacyPolicyButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bannerAdView   != null) bannerAdView.destroy();
        if (interstitialAd != null) interstitialAd.setAdEventListener(null);
        if (rewardedAd     != null) rewardedAd.setAdEventListener(null);
        if (webView        != null) webView.destroy();
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
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/game/index.html");
    }

    // ── Banner Ad ────────────────────────────────────────────────────────────

    private void initBannerAd() {
        bannerAdView = findViewById(R.id.bannerAdView);
        bannerAdView.setAdUnitId(BANNER_AD_UNIT_ID);

        int widthPx = getResources().getDisplayMetrics().widthPixels;
        bannerAdView.setAdSize(BannerAdSize.stickySize(this, widthPx));

        bannerAdView.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                bannerAdView.setVisibility(View.VISIBLE);
                Log.d(TAG, "Banner loaded");
            }
            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                Log.w(TAG, "Banner failed: " + error.getDescription());
                bannerAdView.setVisibility(View.GONE);
            }
            @Override public void onAdClicked()                                  {}
            @Override public void onLeftApplication()                            {}
            @Override public void onReturnedToApplication()                      {}
            @Override public void onImpression(ImpressionData impressionData)    {}
        });

        bannerAdView.loadAd(new AdRequest.Builder().build());
    }

    // ── Interstitial Ad ──────────────────────────────────────────────────────

    void loadInterstitialAd() {
        interstitialLoader = new InterstitialAdLoader(this);
        interstitialLoader.setAdLoadListener(new InterstitialAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                Log.d(TAG, "Interstitial loaded");
            }
            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                interstitialAd = null;
                Log.w(TAG, "Interstitial failed: " + error.getDescription());
            }
        });
        interstitialLoader.loadAd(
                new AdRequestConfiguration.Builder(INTERSTITIAL_AD_UNIT_ID).build());
    }

    /** Called from AndroidBridge (JS: window.AndroidBridge.showInterstitialAd()) */
    void showInterstitialAd() {
        runOnUiThread(() -> {
            if (interstitialAd != null) {
                interstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                    @Override public void onAdShown()               { Log.d(TAG, "Interstitial shown"); }
                    @Override public void onAdFailedToShow(@NonNull AdRequestError e) { loadInterstitialAd(); }
                    @Override public void onAdDismissed()           { interstitialAd = null; loadInterstitialAd(); }
                    @Override public void onAdClicked()             {}
                    @Override public void onAdImpression(ImpressionData d) {}
                });
                interstitialAd.show(MainActivity.this);
            } else {
                Log.d(TAG, "Interstitial not ready yet, preloading");
                loadInterstitialAd();
            }
        });
    }

    // ── Rewarded Ad ──────────────────────────────────────────────────────────

    void loadRewardedAd() {
        rewardedLoader = new RewardedAdLoader(this);
        rewardedLoader.setAdLoadListener(new RewardedAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                Log.d(TAG, "Rewarded ad loaded");
            }
            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                rewardedAd = null;
                Log.w(TAG, "Rewarded failed: " + error.getDescription());
            }
        });
        rewardedLoader.loadAd(
                new AdRequestConfiguration.Builder(REWARDED_AD_UNIT_ID).build());
    }

    /**
     * Called from AndroidBridge.
     * After the user watches the ad to completion the game receives
     * window.onRewardedAdComplete() so it can give coins / lives / etc.
     */
    void showRewardedAd(Runnable onRewarded) {
        pendingRewardCallback = onRewarded;
        runOnUiThread(() -> {
            if (rewardedAd != null) {
                rewardedAd.setAdEventListener(new RewardedAdEventListener() {
                    @Override
                    public void onRewarded(@NonNull Reward reward) {
                        Log.d(TAG, "User earned reward: " + reward.getAmount() + " " + reward.getType());
                        if (pendingRewardCallback != null) {
                            pendingRewardCallback.run();
                            pendingRewardCallback = null;
                        }
                    }
                    @Override public void onAdShown()    { Log.d(TAG, "Rewarded shown"); }
                    @Override public void onAdDismissed() { rewardedAd = null; loadRewardedAd(); }
                    @Override public void onAdFailedToShow(@NonNull AdRequestError e) { loadRewardedAd(); }
                    @Override public void onAdClicked()  {}
                    @Override public void onAdImpression(ImpressionData d) {}
                });
                rewardedAd.show(MainActivity.this);
            } else {
                Log.d(TAG, "Rewarded ad not ready, preloading");
                loadRewardedAd();
            }
        });
    }

    // ── Privacy Policy ───────────────────────────────────────────────────────

    private void initPrivacyPolicyButton() {
        TextView btn = findViewById(R.id.privacyPolicyBtn);
        if (btn == null) return;
        btn.setOnClickListener(v -> openPrivacyPolicy());
    }

    void openPrivacyPolicy() {
        runOnUiThread(() -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)));
            } catch (Exception e) {
                Log.w(TAG, "Could not open privacy policy: " + e.getMessage());
            }
        });
    }
}
