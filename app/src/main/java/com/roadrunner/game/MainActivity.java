package com.roadrunner.game;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdLoader;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;
import com.yandex.mobile.ads.nativeads.NativeAdView;
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder;
import com.yandex.mobile.ads.feed.FeedAd;
import com.yandex.mobile.ads.feed.FeedAdAdapter;
import com.yandex.mobile.ads.feed.FeedAdAppearance;
import com.yandex.mobile.ads.feed.FeedAdEventListener;
import com.yandex.mobile.ads.feed.FeedAdLoadListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "URR";

    private static final String PRIVACY_POLICY_URL =
            "https://nnamdinwali.github.io/ultimate-road-runner-privacy/";

    private static final String BANNER_AD_UNIT_ID       = "R-M-19594035-2";
    private static final String INTERSTITIAL_AD_UNIT_ID = "R-M-19594035-1";
    private static final String REWARDED_AD_UNIT_ID     = "R-M-19594035-4";
    private static final String APP_OPEN_AD_UNIT_ID     = "R-M-19594035-3";
    private static final String NATIVE_AD_UNIT_ID       = "R-M-19594035-8";
    private static final String FEED_AD_UNIT_ID         = "R-M-19594035-9";

    WebView webView;
    private BannerAdView bannerAdView;
    private FrameLayout nativeAdContainer;
    private FrameLayout feedContainer;
    private Button adCloseButton;
    private RecyclerView feedRecyclerView;
    private NativeAdLoader nativeAdLoader;
    private NativeAdLoadListener nativeAdLoadListener;
    private NativeAd nativeAd;
    private FeedAd feedAd;
    private FeedAdAdapter feedAdAdapter;
    private boolean nativeShouldBeVisible = false;
    private boolean nativeAdRendered = false;
    private boolean feedShouldBeVisible = false;
    private boolean feedAdLoaded = false;

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
        // Route hardware volume keys to the media/game audio stream so they
        // actually control game volume (satisfies Huawei AppGallery requirement).
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_main);

        adCloseButton = findViewById(R.id.adCloseButton);
        if (adCloseButton != null) {
            adCloseButton.setOnClickListener(v -> {
                hideNativeAd();
                hideFeedAd();
            });
        }

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
            initNativeAd();
            initFeedAd();
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
        if (nativeAd       != null) nativeAd.setNativeAdEventListener(null);
        if (feedAd         != null) feedAd.setLoadListener(null);
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
        // Allow Web Audio API to start without waiting for a user gesture.
        // Without this, the AudioContext stays "suspended" in some WebView versions
        // and Howler's masterGain never applies — causing the volume slider to have
        // no effect on the actual audio output.
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        // Required to unlock full Web Audio API support inside WebView
        // (including AudioContext creation and GainNode processing).
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        // JS patch injected after the game page loads:
        // On every user touch, resume the Howler AudioContext if it's suspended.
        // When the context is suspended, setValueAtTime() on masterGain is queued
        // but never processed — making the volume slider appear to do nothing.
        final String audioContextResumePatch =
            "(function() {" +
            "  function resumeCtx() {" +
            "    if (typeof Howler !== 'undefined' && Howler.ctx && Howler.ctx.state !== 'running') {" +
            "      Howler.ctx.resume();" +
            "    }" +
            "  }" +
            "  document.addEventListener('touchstart', resumeCtx, { passive: true });" +
            "  document.addEventListener('touchend',   resumeCtx, { passive: true });" +
            "  document.addEventListener('click',      resumeCtx);" +
            "})();";

        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, android.webkit.WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(audioContextResumePatch, null);
                Log.d(TAG, "AudioContext resume patch injected");
            }
        });

        webView.loadUrl("https://appassets.androidplatform.net/assets/game/index.html");
    }

    // ── Native Ad ───────────────────────────────────────────────────────────

    private void initNativeAd() {
        nativeAdContainer = findViewById(R.id.nativeAdContainer);
        if (nativeAdContainer == null) return;

        nativeAdLoader = new NativeAdLoader(this);
        nativeAdLoadListener = new NativeAdLoadListener() {
            @Override
            public void onAdLoaded(@NonNull NativeAd ad) {
                nativeAd = ad;
                nativeAdRendered = false;
                Log.d(TAG, "Native ad loaded");
                renderNativeAd(ad);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                nativeAd = null;
                nativeAdRendered = false;
                Log.w(TAG, "Native ad failed: " + error.getDescription());
                runOnUiThread(() -> {
                    if (nativeAdContainer != null) nativeAdContainer.setVisibility(View.GONE);
                    setAdCloseVisible(false);
                });
            }
        };
        loadNativeAd();
    }

    private void loadNativeAd() {
        if (nativeAdLoader == null || nativeAdLoadListener == null) return;
        nativeAdLoader.loadAd(
                new AdRequest.Builder(NATIVE_AD_UNIT_ID).build(),
                nativeAdLoadListener);
    }

    private void renderNativeAd(@NonNull NativeAd ad) {
        runOnUiThread(() -> {
            if (nativeAdContainer == null) return;

            NativeAdView adView = new NativeAdView(this);
            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(8), dp(6), dp(8), dp(6));

            TextView title = makeNativeText(16, true);
            TextView body = makeNativeText(13, false);
            TextView domain = makeNativeText(11, false);
            TextView sponsored = makeNativeText(10, false);
            TextView warning = makeNativeText(10, false);
            TextView price = makeNativeText(12, true);
            Button callToAction = new Button(this);
            callToAction.setTextSize(12);
            ImageView icon = new ImageView(this);
            icon.setAdjustViewBounds(true);
            com.yandex.mobile.ads.nativeads.MediaView mediaView =
                    new com.yandex.mobile.ads.nativeads.MediaView(this);
            mediaView.setMinimumHeight(dp(160));

            content.addView(title, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(domain, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
            content.addView(mediaView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(160)));
            content.addView(body, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(price, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(callToAction, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(sponsored, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(warning, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            adView.addView(content, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            NativeAdViewBinder binder = new NativeAdViewBinder.Builder(adView)
                    .setTitleView(title)
                    .setBodyView(body)
                    .setDomainView(domain)
                    .setMediaView(mediaView)
                    .setPriceView(price)
                    .setCallToActionView(callToAction)
                    .setIconView(icon)
                    .setSponsoredView(sponsored)
                    .setWarningView(warning)
                    .build();
            ad.bindNativeAd(binder);
            ad.setNativeAdEventListener(new NativeAdEventLogger());
            nativeAdContainer.removeAllViews();
            nativeAdContainer.addView(adView);
            nativeAdContainer.setVisibility(View.GONE);

            // Yandex can report a loaded object while the rendered assets are
            // still empty. Do not expose the dark placeholder until at least
            // one meaningful native asset has been bound.
            adView.post(() -> {
                boolean hasText = title.getText().length() > 0
                        || body.getText().length() > 0
                        || domain.getText().length() > 0
                        || callToAction.getText().length() > 0;
                nativeAdRendered = hasText;
                if (nativeShouldBeVisible && hasText) {
                    nativeAdContainer.setVisibility(View.VISIBLE);
                    setAdCloseVisible(true);
                } else {
                    nativeAdContainer.setVisibility(View.GONE);
                    setAdCloseVisible(false);
                }
                if (!hasText) Log.w(TAG, "Native ad loaded without renderable assets");
            });
        });
    }

    private TextView makeNativeText(int sizeSp, boolean bold) {
        TextView view = new TextView(this);
        view.setTextSize(sizeSp);
        view.setTextColor(0xFFFFFFFF);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class NativeAdEventLogger implements com.yandex.mobile.ads.nativeads.NativeAdEventListener {
        @Override public void onAdClicked() { Log.d(TAG, "Native ad clicked"); }
        @Override public void onImpression(ImpressionData data) { Log.i(TAG, "Native impression: " + data); }
    }

    void showNativeAd() {
        runOnUiThread(() -> {
            nativeShouldBeVisible = true;
            if (feedRecyclerView != null) feedRecyclerView.setVisibility(View.GONE);
            if (nativeAdContainer != null) {
                nativeAdContainer.setVisibility(nativeAdRendered ? View.VISIBLE : View.GONE);
                setAdCloseVisible(nativeAdRendered);
                if (nativeAd == null || !nativeAdRendered) loadNativeAd();
            }
        });
    }

    void hideNativeAd() {
        runOnUiThread(() -> {
            nativeShouldBeVisible = false;
            if (nativeAdContainer != null) nativeAdContainer.setVisibility(View.GONE);
            if (!feedShouldBeVisible) setAdCloseVisible(false);
        });
    }

    // ── Feed Ad ──────────────────────────────────────────────────────────────

    private void initFeedAd() {
        feedRecyclerView = findViewById(R.id.feedRecyclerView);
        feedContainer = findViewById(R.id.feedContainer);
        if (feedRecyclerView == null || feedContainer == null) return;

        int screenWidthDp = Math.round(getResources().getDisplayMetrics().widthPixels /
                getResources().getDisplayMetrics().density);
        int cardWidthDp = Math.max(280, screenWidthDp - 48);
        FeedAdAppearance appearance = new FeedAdAppearance.Builder(cardWidthDp)
                .setCardCornerRadius(16.0)
                .build();
        AdRequest request = new AdRequest.Builder(FEED_AD_UNIT_ID).build();
        feedAd = new FeedAd.Builder(this, request, appearance).build();
        feedAd.setLoadListener(new FeedAdLoadListener() {
            @Override public void onAdLoaded() {
                feedAdLoaded = true;
                Log.d(TAG, "Feed ad loaded");
                runOnUiThread(() -> {
                    if (feedContainer != null) {
                        feedContainer.setVisibility(feedShouldBeVisible ? View.VISIBLE : View.GONE);
                    }
                    setAdCloseVisible(feedShouldBeVisible);
                    if (feedRecyclerView != null) {
                        feedRecyclerView.setVisibility(feedShouldBeVisible ? View.VISIBLE : View.GONE);
                    }
                });
            }
            @Override public void onAdFailedToLoad(@NonNull AdRequestError error) {
                feedAdLoaded = false;
                Log.w(TAG, "Feed ad failed: " + error.getDescription());
                runOnUiThread(() -> {
                    if (feedContainer != null) feedContainer.setVisibility(View.GONE);
                    if (feedRecyclerView != null) feedRecyclerView.setVisibility(View.GONE);
                    if (!nativeShouldBeVisible) setAdCloseVisible(false);
                });
            }
        });
        feedAdAdapter = new FeedAdAdapter(feedAd);
        feedAdAdapter.setEventListener(new FeedAdEventListener() {
            @Override public void onAdClicked() { Log.d(TAG, "Feed ad clicked"); }
            @Override public void onImpression(ImpressionData data) { Log.i(TAG, "Feed impression: " + data); }
        });
        feedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        feedRecyclerView.setAdapter(feedAdAdapter);
        feedContainer.setVisibility(View.GONE);
        feedRecyclerView.setVisibility(View.GONE);
        feedAd.preloadAd();
    }

    void showFeedAd() {
        runOnUiThread(() -> {
            feedShouldBeVisible = true;
            if (nativeAdContainer != null) nativeAdContainer.setVisibility(View.GONE);
            if (feedContainer != null) feedContainer.setVisibility(feedAdLoaded ? View.VISIBLE : View.GONE);
            if (feedRecyclerView != null) feedRecyclerView.setVisibility(feedAdLoaded ? View.VISIBLE : View.GONE);
            setAdCloseVisible(feedAdLoaded);
        });
    }

    private void setAdCloseVisible(boolean visible) {
        if (adCloseButton != null) {
            adCloseButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    void hideFeedAd() {
        runOnUiThread(() -> {
            feedShouldBeVisible = false;
            if (feedContainer != null) feedContainer.setVisibility(View.GONE);
            if (!nativeShouldBeVisible) setAdCloseVisible(false);
            if (feedRecyclerView != null) feedRecyclerView.setVisibility(View.GONE);
        });
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
            @Override public void onAdClicked() {
                Log.d(TAG, "Banner clicked");
            }
            @Override public void onImpression(ImpressionData impressionData) {
                Log.i(TAG, "Banner impression: " + impressionData);
            }
        });

        // Preload immediately so a missed WebView bridge callback cannot prevent
        // the Yandex request and its configured mediation waterfall from starting.
        bannerLoadStarted = true;
        bannerAdView.loadAd(new AdRequest.Builder(BANNER_AD_UNIT_ID).build());
        Log.d(TAG, "Banner preload requested");
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
            }
            bannerAdView.setVisibility(View.VISIBLE);
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
                    @Override public void onAdClicked()             { Log.d(TAG, "Interstitial clicked"); }
                    @Override public void onAdImpression(ImpressionData d) { Log.i(TAG, "Interstitial impression: " + d); }
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
                    @Override public void onAdClicked()   { Log.d(TAG, "Rewarded clicked"); }
                    @Override public void onAdImpression(ImpressionData d) { Log.i(TAG, "Rewarded impression: " + d); }
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
            @Override public void onAdClicked()                       { Log.d(TAG, "App Open clicked"); }
            @Override public void onAdImpression(ImpressionData d)    { Log.i(TAG, "App Open impression: " + d); }
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
