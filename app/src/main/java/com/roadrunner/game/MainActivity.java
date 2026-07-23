package com.roadrunner.game;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
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
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.common.YandexAds;
import com.yandex.mobile.ads.interstitial.InterstitialAd;
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener;
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader;
import com.yandex.mobile.ads.appopenad.AppOpenAd;
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener;
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener;
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "URR";

    private static final String PRIVACY_POLICY_URL =
            "https://nnamdinwali.github.io/ultimate-road-runner-privacy/";

    private static final String BANNER_AD_UNIT_ID       = "R-M-19594035-2";
    private static final String INTERSTITIAL_AD_UNIT_ID = "R-M-19594035-1";
    private static final String REWARDED_AD_UNIT_ID     = "R-M-19594035-4";
    private static final String APP_OPEN_AD_UNIT_ID     = "R-M-19594035-3";

    WebView webView;
    private BannerAdView bannerAdView;

    private boolean bannerLoadStarted     = false;
    private boolean bannerShouldBeVisible = false;

    private InterstitialAdLoader interstitialLoader;
    private InterstitialAd       interstitialAd;

    private RewardedAdLoader rewardedLoader;
    private RewardedAd       rewardedAd;

    private Runnable pendingRewardCallback;

    private AppOpenAdLoader appOpenAdLoader;
    private AppOpenAd       appOpenAd;
    private boolean         appOpenAdShowing   = false;
    private boolean         isFirstStart       = true;
    private boolean         appWasInBackground = false;

    // ── Network monitoring ────────────────────────────────────────────────────
    private ConnectivityManager         connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private AlertDialog                 noNetworkDialog;   // currently showing dialog, if any
    private boolean                     appStarted = false; // true once startApp() has run

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        registerNetworkCallback();

        if (!isNetworkAvailable()) {
            showNoNetworkDialog();
        } else {
            startApp();
        }
    }

    /** Called once we confirm there is a network connection. */
    private void startApp() {
        appStarted = true;

        YandexAds.initialize(this, () -> {
            Log.d(TAG, "Yandex MobileAds SDK initialized");
            loadInterstitialAd();
            loadRewardedAd();
            loadAppOpenAd();
        });

        initWebView();
        initBannerAd();
        initPrivacyPolicyButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isFirstStart) {
            isFirstStart = false;
        } else if (appWasInBackground) {
            appWasInBackground = false;
            showAppOpenAd();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        appWasInBackground = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterNetworkCallback();
        if (noNetworkDialog != null && noNetworkDialog.isShowing()) noNetworkDialog.dismiss();
        if (bannerAdView   != null) bannerAdView.destroy();
        if (interstitialAd != null) interstitialAd.setAdEventListener(null);
        if (rewardedAd     != null) rewardedAd.setAdEventListener(null);
        if (appOpenAd      != null) appOpenAd.setAdEventListener(null);
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

    // ── Network monitoring ────────────────────────────────────────────────────

    private void registerNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Network came back — dismiss the dialog if it's showing
                runOnUiThread(() -> {
                    if (noNetworkDialog != null && noNetworkDialog.isShowing()) {
                        noNetworkDialog.dismiss();
                        noNetworkDialog = null;
                        // If the app hasn't started yet (offline at launch), start it now
                        if (!appStarted) {
                            startApp();
                        }
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                // Network dropped mid-session — block the game immediately
                runOnUiThread(() -> {
                    if (noNetworkDialog == null || !noNetworkDialog.isShowing()) {
                        showNoNetworkDialog();
                    }
                });
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (networkCallback != null && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering network callback: " + e.getMessage());
            }
            networkCallback = null;
        }
    }

    private boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        NetworkCapabilities caps =
                connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return caps != null && (
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)     ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    private void showNoNetworkDialog() {
        // Dismiss any existing dialog before showing a new one
        if (noNetworkDialog != null && noNetworkDialog.isShowing()) return;

        noNetworkDialog = new AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("Road Runner requires an internet connection to play. Please connect to the internet and try again.")
                .setCancelable(false)
                .setPositiveButton("Retry", (dialog, which) -> {
                    noNetworkDialog = null;
                    if (isNetworkAvailable()) {
                        if (!appStarted) startApp();
                    } else {
                        showNoNetworkDialog();
                    }
                })
                .setNegativeButton("Exit", (dialog, which) -> finishAffinity())
                .show();
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

        int widthPx = getResources().getDisplayMetrics().widthPixels;
        bannerAdView.setAdSize(BannerAdSize.sticky(this, widthPx));

        bannerAdView.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner loaded");
                if (bannerShouldBeVisible) {
                    runOnUiThread(() -> {
                        if (bannerAdView != null) bannerAdView.setVisibility(View.VISIBLE);
                    });
                } else {
                    runOnUiThread(() -> {
                        if (bannerAdView != null) bannerAdView.setVisibility(View.GONE);
                    });
                }
            }
            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                Log.w(TAG, "Banner failed: " + error.getDescription());
                bannerLoadStarted = false;
                runOnUiThread(() -> {
                    if (bannerAdView != null) bannerAdView.setVisibility(View.GONE);
                });
            }
            @Override public void onAdClicked()                               {}
            @Override public void onImpression(ImpressionData impressionData) {}
        });
    }

    // ── Banner show / hide ────────────────────────────────────────────────────

    void showBanner() {
        runOnUiThread(() -> {
            if (bannerAdView == null) return;
            bannerShouldBeVisible = true;
            if (!bannerLoadStarted) {
                bannerLoadStarted = true;
                bannerAdView.loadAd(new AdRequest.Builder(BANNER_AD_UNIT_ID).build());
                Log.d(TAG, "Banner: first load requested (gameplay started)");
            } else {
                bannerAdView.setVisibility(View.VISIBLE);
            }
        });
    }

    void hideBanner() {
        runOnUiThread(() -> {
            bannerShouldBeVisible = false;
            if (bannerAdView != null) bannerAdView.setVisibility(View.GONE);
        });
    }

    // ── Interstitial Ad ──────────────────────────────────────────────────────

    void loadInterstitialAd() {
        interstitialLoader = new InterstitialAdLoader(this);
        interstitialLoader.loadAd(
                new AdRequest.Builder(INTERSTITIAL_AD_UNIT_ID).build(),
                new InterstitialAdLoadListener() {
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
    }

    void showInterstitialAd() {
        runOnUiThread(() -> {
            if (interstitialAd != null) {
                interstitialAd.setAdEventListener(new InterstitialAdEventListener() {
                    @Override public void onAdShown()               { Log.d(TAG, "Interstitial shown"); }
                    @Override public void onAdFailedToShow(@NonNull AdError e) { loadInterstitialAd(); }
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
        rewardedLoader.loadAd(
                new AdRequest.Builder(REWARDED_AD_UNIT_ID).build(),
                new RewardedAdLoadListener() {
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
    }

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
                    @Override public void onAdShown()     { Log.d(TAG, "Rewarded shown"); }
                    @Override public void onAdDismissed() { rewardedAd = null; loadRewardedAd(); }
                    @Override public void onAdFailedToShow(@NonNull AdError e) { loadRewardedAd(); }
                    @Override public void onAdClicked()   {}
                    @Override public void onAdImpression(ImpressionData d) {}
                });
                rewardedAd.show(MainActivity.this);
            } else {
                Log.d(TAG, "Rewarded ad not ready, preloading");
                loadRewardedAd();
            }
        });
    }

    // ── App Open Ad ──────────────────────────────────────────────────────────

    void loadAppOpenAd() {
        appOpenAdLoader = new AppOpenAdLoader(this);
        appOpenAdLoader.loadAd(
                new AdRequest.Builder(APP_OPEN_AD_UNIT_ID).build(),
                new AppOpenAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        appOpenAd = ad;
                        Log.d(TAG, "App Open ad loaded");
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError error) {
                        appOpenAd = null;
                        Log.w(TAG, "App Open ad failed: " + error.getDescription());
                    }
                });
    }

    void showAppOpenAd() {
        if (appOpenAd == null || appOpenAdShowing) return;
        appOpenAdShowing = true;
        appOpenAd.setAdEventListener(new AppOpenAdEventListener() {
            @Override public void onAdShown()       { Log.d(TAG, "App Open shown"); }
            @Override public void onAdFailedToShow(@NonNull AdError e) {
                appOpenAdShowing = false;
                appOpenAd = null;
                loadAppOpenAd();
            }
            @Override public void onAdDismissed() {
                appOpenAdShowing = false;
                appOpenAd = null;
                loadAppOpenAd();
            }
            @Override public void onAdClicked()                       {}
            @Override public void onAdImpression(ImpressionData d)    {}
        });
        appOpenAd.show(MainActivity.this);
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
