package com.roadrunner.gamer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "URR";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 4101;
    private static final int RETURN_REMINDER_REQUEST_CODE = 4104;
    private static final long RETURN_REMINDER_DELAY_MS = 2L * 60L * 1000L; // TEST: 2 minutes // TEST: 2 minutes (change back to 3 hours later)
    private static final String ROCKCITY_API_BASE = "https://gamezoneapi-cp623ub2.manus.space/api";
    private static final String PUSH_PREFS = "rockcity_push";

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
    private boolean nativeLoading = false;
    private boolean nativeRetryScheduled = false;
    private int     nativeRetryCount = 0;
    private boolean feedShouldBeVisible = false;
    private boolean feedAdLoaded = false;
    private boolean feedPreloadRequested = false;
    private boolean feedRetryScheduled = false;
    private int     feedRetryCount = 0;

    private boolean bannerLoadStarted     = false;
    private boolean bannerLoading         = false;
    private boolean bannerRetryScheduled  = false;
    private int     bannerRetryCount     = 0;
    private boolean bannerShouldBeVisible = false;

    private static final long AD_RETRY_BASE_MS = 15_000L;
    private static final long AD_RETRY_MAX_MS = 120_000L;
    private final Handler adHandler = new Handler(Looper.getMainLooper());

    private InterstitialAdLoader interstitialLoader;
    private InterstitialAd       interstitialAd;
    private boolean               interstitialLoading;
    private boolean               interstitialRetryScheduled;
    private int                   interstitialRetryCount;

    private RewardedAdLoader rewardedLoader;
    private RewardedAd       rewardedAd;
    private boolean           rewardedLoading;
    private boolean           rewardedRetryScheduled;
    private int               rewardedRetryCount;

    private Runnable pendingRewardCallback;

    private AppOpenAdLoader appOpenAdLoader;
    private AppOpenAd       appOpenAd;
    private boolean          appOpenLoading;
    private boolean          appOpenRetryScheduled;
    private int              appOpenRetryCount;
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
        ReturnReminderReceiver.ensureNotificationChannel(this);
        requestNotificationPermissionIfNeeded();

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
        cancelReturnReminder();
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
        scheduleReturnReminder();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adHandler.removeCallbacksAndMessages(null);
        unregisterNetworkCallback();
        if (noNetworkDialog != null && noNetworkDialog.isShowing()) noNetworkDialog.dismiss();
        if (bannerAdView != null) bannerAdView.destroy();
        if (nativeAd != null) nativeAd.setNativeAdEventListener(null);
        if (feedAd != null) feedAd.setLoadListener(null);
        if (interstitialAd != null) interstitialAd.setAdEventListener(null);
        if (rewardedAd != null) rewardedAd.setAdEventListener(null);
        if (appOpenAd != null) appOpenAd.setAdEventListener(null);
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

    private PendingIntent getReturnReminderPendingIntent() {
        Intent reminderIntent = new Intent(this, ReturnReminderReceiver.class)
                .setAction(ReturnReminderReceiver.ACTION_RETURN_REMINDER);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(this, RETURN_REMINDER_REQUEST_CODE, reminderIntent, flags);
    }

    private void scheduleReturnReminder() {
        if (!appStarted) return;
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent pendingIntent = getReturnReminderPendingIntent();
        try {
            alarmManager.cancel(pendingIntent);
        } catch (Exception ignored) {}
        long triggerAtMillis = System.currentTimeMillis() + RETURN_REMINDER_DELAY_MS;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Log.i(TAG, "Return reminder scheduled in " + (RETURN_REMINDER_DELAY_MS / 1000) + "s");
        } catch (SecurityException se) {
            // Fallback when exact alarms are blocked by the OS / OEM
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
                Log.i(TAG, "Return reminder scheduled (inexact fallback)");
            } catch (Exception e) {
                Log.e(TAG, "Failed to schedule return reminder", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule return reminder", e);
        }
    }

    private void cancelReturnReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;
        alarmManager.cancel(getReturnReminderPendingIntent());
    }

    /** Request Android 13+ notification permission without assuming a provider is configured. */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    /** Called by AndroidBridge when an actual FCM or HMS token is available. */
    public void registerPushToken(String provider, String token) {
        final String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase();
        final String normalizedToken = token == null ? "" : token.trim();
        if (!("fcm".equals(normalizedProvider) || "hms".equals(normalizedProvider)) || normalizedToken.isEmpty()) {
            Log.w(TAG, "Ignoring invalid push token registration request");
            return;
        }
        SharedPreferences preferences = getSharedPreferences(PUSH_PREFS, MODE_PRIVATE);
        String deviceId = preferences.getString("device_id", null);
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            preferences.edit().putString("device_id", deviceId).apply();
        }
        preferences.edit().putString("provider", normalizedProvider).putString("token", normalizedToken).apply();
        final String finalDeviceId = deviceId;
        new Thread(() -> submitPushToken(normalizedProvider, normalizedToken, finalDeviceId)).start();
    }

    private void submitPushToken(String provider, String token, String deviceId) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(ROCKCITY_API_BASE + "/users/me/push-token");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("PUT");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            String cookie = CookieManager.getInstance().getCookie(ROCKCITY_API_BASE);
            if (cookie != null && !cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
            String payload = "{\"provider\":\"" + escapeJson(provider) + "\",\"platform\":\"android\",\"deviceId\":\"" + escapeJson(deviceId) + "\",\"token\":\"" + escapeJson(token) + "\"}";
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) Log.d(TAG, "Rockcity push token registered");
            else Log.w(TAG, "Rockcity push token registration returned HTTP " + status);
        } catch (Exception error) {
            Log.w(TAG, "Rockcity push token registration deferred: " + error.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\\\", "\\\\\\\\").replace("\"", "\\\"");
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
                nativeLoading = false;
                nativeRetryScheduled = false;
                nativeRetryCount = 0;
                if (nativeAd != null) nativeAd.setNativeAdEventListener(null);
                nativeAd = ad;
                nativeAdRendered = false;
                Log.d(TAG, "Native ad loaded");
                renderNativeAd(ad);
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError error) {
                nativeLoading = false;
                nativeAd = null;
                nativeAdRendered = false;
                Log.w(TAG, "Native ad failed: " + error.getDescription());
                runOnUiThread(() -> {
                    if (nativeAdContainer != null) nativeAdContainer.setVisibility(View.GONE);
                    setAdCloseVisible(false);
                });
                scheduleNativeRetry();
            }
        };
        loadNativeAd();
    }

    private void loadNativeAd() {
        if (nativeAdLoader == null || nativeAdLoadListener == null || nativeAd != null || nativeLoading) return;
        nativeLoading = true;
        nativeAdLoader.loadAd(
                new AdRequest.Builder(NATIVE_AD_UNIT_ID).build(),
                nativeAdLoadListener);
    }

    private void scheduleNativeRetry() {
        if (nativeRetryScheduled || nativeAd != null || nativeAdLoader == null) return;
        nativeRetryScheduled = true;
        long delay = Math.min(AD_RETRY_MAX_MS,
                AD_RETRY_BASE_MS * (1L << Math.min(nativeRetryCount++, 3)));
        adHandler.postDelayed(() -> {
            nativeRetryScheduled = false;
            loadNativeAd();
        }, delay);
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
            adView.postDelayed(() -> {
                boolean hasText = title.getText().length() > 0
                        || body.getText().length() > 0
                        || domain.getText().length() > 0
                        || callToAction.getText().length() > 0;
                // A successfully loaded Yandex NativeAd is displayable even when
                // a particular creative has no text field. Do not hide a valid
                // image/media creative solely because text arrived late or is empty.
                nativeAdRendered = true;
                if (nativeShouldBeVisible) {
                    nativeAdContainer.setVisibility(View.VISIBLE);
                    setAdCloseVisible(true);
                } else {
                    nativeAdContainer.setVisibility(View.GONE);
                    setAdCloseVisible(false);
                }
                if (!hasText) Log.d(TAG, "Native ad loaded with media-only or late text assets");
            }, 250);
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
                loadNativeAd();
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
                feedPreloadRequested = false;
                feedRetryScheduled = false;
                feedRetryCount = 0;
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
                feedPreloadRequested = false;
                feedAdLoaded = false;
                Log.w(TAG, "Feed ad failed: " + error.getDescription());
                runOnUiThread(() -> {
                    if (feedContainer != null) feedContainer.setVisibility(View.GONE);
                    if (feedRecyclerView != null) feedRecyclerView.setVisibility(View.GONE);
                    if (!nativeShouldBeVisible) setAdCloseVisible(false);
                });
                scheduleFeedRetry();
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
        preloadFeedAd();
    }

    private void scheduleFeedRetry() {
        if (feedRetryScheduled || feedAdLoaded || feedAd == null) return;
        feedRetryScheduled = true;
        long delay = Math.min(AD_RETRY_MAX_MS,
                AD_RETRY_BASE_MS * (1L << Math.min(feedRetryCount++, 3)));
        adHandler.postDelayed(() -> {
            feedRetryScheduled = false;
            preloadFeedAd();
        }, delay);
    }

    private void preloadFeedAd() {
        if (feedAd == null || feedPreloadRequested || feedRetryScheduled) return;
        feedPreloadRequested = true;
        feedAd.preloadAd();
    }

    void showFeedAd() {
        runOnUiThread(() -> {
            feedShouldBeVisible = true;
            if (nativeAdContainer != null) nativeAdContainer.setVisibility(View.GONE);
            if (!feedAdLoaded) preloadFeedAd();
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
                bannerLoading = false;
                bannerRetryScheduled = false;
                bannerRetryCount = 0;
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
                bannerLoading = false;
                bannerLoadStarted = false;
                runOnUiThread(() -> {
                    if (bannerAdView != null) bannerAdView.setVisibility(View.GONE);
                });
                scheduleBannerRetry();
            }
            @Override public void onAdClicked() {
                Log.d(TAG, "Banner clicked");
            }
            @Override public void onImpression(ImpressionData impressionData) {
                Log.i(TAG, "Banner impression: " + impressionData);
            }
        });

        // Preload immediately so the first gameplay request has a ready banner.
        loadBannerAd();
    }

    private void loadBannerAd() {
        if (bannerAdView == null || bannerLoading) return;
        bannerLoadStarted = true;
        bannerLoading = true;
        bannerAdView.loadAd(new AdRequest.Builder(BANNER_AD_UNIT_ID).build());
        Log.d(TAG, "Direct Yandex banner preload requested");
    }

    private void scheduleBannerRetry() {
        if (bannerRetryScheduled || bannerLoading || bannerAdView == null) return;
        bannerRetryScheduled = true;
        long delay = Math.min(AD_RETRY_MAX_MS,
                AD_RETRY_BASE_MS * (1L << Math.min(bannerRetryCount++, 3)));
        adHandler.postDelayed(() -> {
            bannerRetryScheduled = false;
            loadBannerAd();
        }, delay);
    }

    // ── Banner show / hide ────────────────────────────────────────────────────

    void showBanner() {
        runOnUiThread(() -> {
            if (bannerAdView == null) return;
            bannerShouldBeVisible = true;
            if (!bannerLoadStarted) loadBannerAd();
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
        runOnUiThread(() -> {
            if (interstitialAd != null || interstitialLoading) return;
            if (interstitialLoader == null) {
                interstitialLoader = new InterstitialAdLoader(this);
            }
            interstitialLoading = true;
            interstitialLoader.loadAd(
                    new AdRequest.Builder(INTERSTITIAL_AD_UNIT_ID).build(),
                    new InterstitialAdLoadListener() {
                        @Override public void onAdLoaded(@NonNull InterstitialAd ad) {
                            interstitialLoading = false;
                            interstitialRetryScheduled = false;
                            interstitialRetryCount = 0;
                            interstitialAd = ad;
                            Log.d(TAG, "Interstitial loaded and cached");
                        }
                        @Override public void onAdFailedToLoad(@NonNull AdRequestError error) {
                            interstitialLoading = false;
                            interstitialAd = null;
                            Log.w(TAG, "Interstitial failed: " + error.getDescription());
                            scheduleInterstitialRetry();
                        }
                    });
        });
    }

    private void scheduleInterstitialRetry() {
        if (interstitialRetryScheduled || interstitialAd != null || interstitialLoader == null) return;
        interstitialRetryScheduled = true;
        long delay = Math.min(AD_RETRY_MAX_MS,
                AD_RETRY_BASE_MS * (1L << Math.min(interstitialRetryCount++, 3)));
        adHandler.postDelayed(() -> {
            interstitialRetryScheduled = false;
            loadInterstitialAd();
        }, delay);
    }

    void showInterstitialAd() {
        runOnUiThread(() -> {
            if (interstitialAd == null) {
                Log.d(TAG, "Interstitial not ready; keeping the next request controlled");
                loadInterstitialAd();
                return;
            }
            final InterstitialAd adToShow = interstitialAd;
            interstitialAd = null;
            adToShow.setAdEventListener(new InterstitialAdEventListener() {
                @Override public void onAdShown() { Log.d(TAG, "Interstitial shown"); }
                @Override public void onAdFailedToShow(@NonNull AdError e) {
                    adToShow.setAdEventListener(null);
                    Log.w(TAG, "Interstitial failed to show: " + e.getDescription());
                    loadInterstitialAd();
                }
                @Override public void onAdDismissed() {
                    adToShow.setAdEventListener(null);
                    loadInterstitialAd();
                }
                @Override public void onAdClicked() { Log.d(TAG, "Interstitial clicked"); }
                @Override public void onAdImpression(ImpressionData d) { Log.i(TAG, "Interstitial impression: " + d); }
            });
            loadInterstitialAd();
            adToShow.show(MainActivity.this);
        });
    }

    // ── Rewarded Ad ──────────────────────────────────────────────────────────

    void loadRewardedAd() {
        runOnUiThread(() -> {
            if (rewardedAd != null || rewardedLoading) return;
            if (rewardedLoader == null) rewardedLoader = new RewardedAdLoader(this);
            rewardedLoading = true;
            rewardedLoader.loadAd(
                    new AdRequest.Builder(REWARDED_AD_UNIT_ID).build(),
                    new RewardedAdLoadListener() {
                        @Override public void onAdLoaded(@NonNull RewardedAd ad) {
                            rewardedLoading = false;
                            rewardedRetryScheduled = false;
                            rewardedRetryCount = 0;
                            rewardedAd = ad;
                            Log.d(TAG, "Rewarded ad loaded and cached");
                        }
                        @Override public void onAdFailedToLoad(@NonNull AdRequestError error) {
                            rewardedLoading = false;
                            rewardedAd = null;
                            Log.w(TAG, "Rewarded failed: " + error.getDescription());
                            scheduleRewardedRetry();
                        }
                    });
        });
    }

    private void scheduleRewardedRetry() {
        if (rewardedRetryScheduled || rewardedAd != null || rewardedLoader == null) return;
        rewardedRetryScheduled = true;
        long delay = Math.min(AD_RETRY_MAX_MS,
                AD_RETRY_BASE_MS * (1L << Math.min(rewardedRetryCount++, 3)));
        adHandler.postDelayed(() -> {
            rewardedRetryScheduled = false;
            loadRewardedAd();
        }, delay);
    }

    void showRewardedAd(Runnable onRewarded) {
        pendingRewardCallback = onRewarded;
        runOnUiThread(() -> {
            if (rewardedAd == null) {
                Log.d(TAG, "Rewarded ad not ready; keeping the next request controlled");
                loadRewardedAd();
                return;
            }
            final RewardedAd adToShow = rewardedAd;
            rewardedAd = null;
            adToShow.setAdEventListener(new RewardedAdEventListener() {
                @Override public void onRewarded(@NonNull Reward reward) {
                    Log.d(TAG, "User earned reward: " + reward.getAmount() + " " + reward.getType());
                    if (pendingRewardCallback != null) {
                        pendingRewardCallback.run();
                        pendingRewardCallback = null;
                    }
                }
                @Override public void onAdShown() { Log.d(TAG, "Rewarded shown"); }
                @Override public void onAdDismissed() {
                    adToShow.setAdEventListener(null);
                    loadRewardedAd();
                }
                @Override public void onAdFailedToShow(@NonNull AdError e) {
                    adToShow.setAdEventListener(null);
                    Log.w(TAG, "Rewarded failed to show: " + e.getDescription());
                    loadRewardedAd();
                }
                @Override public void onAdClicked() { Log.d(TAG, "Rewarded clicked"); }
                @Override public void onAdImpression(ImpressionData d) { Log.i(TAG, "Rewarded impression: " + d); }
            });
            loadRewardedAd();
            adToShow.show(MainActivity.this);
        });
    }

    // ── App Open Ad ──────────────────────────────────────────────────────────

    void loadAppOpenAd() {
        runOnUiThread(() -> {
            if (appOpenAd != null || appOpenLoading) return;
            if (appOpenAdLoader == null) appOpenAdLoader = new AppOpenAdLoader(this);
            appOpenLoading = true;
            appOpenAdLoader.loadAd(
                    new AdRequest.Builder(APP_OPEN_AD_UNIT_ID).build(),
                    new AppOpenAdLoadListener() {
                        @Override public void onAdLoaded(@NonNull AppOpenAd ad) {
                            appOpenLoading = false;
                            appOpenRetryScheduled = false;
                            appOpenRetryCount = 0;
                            appOpenAd = ad;
                            Log.d(TAG, "App Open ad loaded and cached");
                        }
                        @Override public void onAdFailedToLoad(@NonNull AdRequestError error) {
                            appOpenLoading = false;
                            appOpenAd = null;
                            Log.w(TAG, "App Open ad failed: " + error.getDescription());
                            scheduleAppOpenRetry();
                        }
                    });
        });
    }

    private void scheduleAppOpenRetry() {
        if (appOpenRetryScheduled || appOpenAd != null || appOpenAdLoader == null) return;
        appOpenRetryScheduled = true;
        long delay = Math.min(AD_RETRY_MAX_MS,
                AD_RETRY_BASE_MS * (1L << Math.min(appOpenRetryCount++, 3)));
        adHandler.postDelayed(() -> {
            appOpenRetryScheduled = false;
            loadAppOpenAd();
        }, delay);
    }

    void showAppOpenAd() {
        runOnUiThread(() -> {
            if (appOpenAd == null || appOpenAdShowing) return;
            final AppOpenAd adToShow = appOpenAd;
            appOpenAd = null;
            appOpenAdShowing = true;
            adToShow.setAdEventListener(new AppOpenAdEventListener() {
                @Override public void onAdShown() { Log.d(TAG, "App Open shown"); }
                @Override public void onAdFailedToShow(@NonNull AdError e) {
                    appOpenAdShowing = false;
                    adToShow.setAdEventListener(null);
                    Log.w(TAG, "App Open failed to show: " + e.getDescription());
                    loadAppOpenAd();
                }
                @Override public void onAdDismissed() {
                    appOpenAdShowing = false;
                    adToShow.setAdEventListener(null);
                    loadAppOpenAd();
                }
                @Override public void onAdClicked() { Log.d(TAG, "App Open clicked"); }
                @Override public void onAdImpression(ImpressionData d) { Log.i(TAG, "App Open impression: " + d); }
            });
            loadAppOpenAd();
            adToShow.show(MainActivity.this);
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
