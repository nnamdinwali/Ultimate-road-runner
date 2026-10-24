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


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.messaging.FirebaseMessaging;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerCallbacks;
import com.appodeal.ads.BannerView;
import com.appodeal.ads.InterstitialCallbacks;
import com.appodeal.ads.RewardedVideoCallbacks;
import com.appodeal.ads.NativeCallbacks;
import com.appodeal.ads.NativeAd;
import com.appodeal.ads.nativead.NativeAdViewNewsFeed;
import com.appodeal.ads.initializing.ApdInitializationCallback;
import com.appodeal.ads.initializing.ApdInitializationError;

import java.util.List;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "URR";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 4101;
    private static final int RETURN_REMINDER_REQUEST_CODE = 4104;
    private static final long RETURN_REMINDER_DELAY_MS = 2L * 60L * 1000L; // TEST: 2 minutes // TEST: 2 minutes // TEST: 2 minutes // TEST: 2 minutes (change back to 3 hours later)
    private static final String ROCKCITY_API_BASE = "https://gamezoneapi-cp623ub2.manus.space/api";
    private static final String PUSH_PREFS = "rockcity_push";

    private static final String PRIVACY_POLICY_URL =
            "https://nnamdinwali.github.io/ultimate-road-runner-privacy/";

    private static final String APPODEAL_APP_KEY =
            "af449661525c1513a9ee6bf7cc28f1fe77d5f2a25af88f18";
    private static final int AD_TYPES =
            Appodeal.BANNER | Appodeal.INTERSTITIAL | Appodeal.REWARDED_VIDEO | Appodeal.NATIVE;

    WebView webView;
    private BannerView bannerView;
    private NativeAdViewNewsFeed nativeAdView;
    private boolean adsInitialized = false;
    private boolean bannerRequested = true;
    private boolean nativeAdRequested = false;
    private boolean nativeAdLoaded = false;
    private boolean pendingInterstitial = false;
    private boolean pendingRewarded = false;
    private boolean rewardEarned = false;
    private Runnable pendingRewardCallback;
    private boolean appWasInBackground = false;
    private boolean isFirstStart = true;
    private final Handler adHandler = new Handler(Looper.getMainLooper());

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

        connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        registerNetworkCallback();
        ReturnReminderReceiver.ensureNotificationChannel(this);
        requestNotificationPermissionIfNeeded();
        fetchAndRegisterFcmToken();

        if (!isNetworkAvailable()) {
            showNoNetworkDialog();
        } else {
            startApp();
        }
    }

    /** Called once we confirm there is a network connection. */
    private void startApp() {
        appStarted = true;
        initWebView();
        initPrivacyPolicyButton();
        initAppodeal();
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
        if (adsInitialized) {
            Appodeal.setBannerCallbacks(null);
            Appodeal.setInterstitialCallbacks(null);
            Appodeal.setRewardedVideoCallbacks(null);
            Appodeal.setNativeCallbacks(null);
        }
        if (nativeAdView != null) {
            try { nativeAdView.unregisterView(); } catch (Exception ignored) {}
            nativeAdView.destroy();
            nativeAdView = null;
        }
        bannerView = null;
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

        // Play-safe local reminder: NOT setAlarmClock (that shows a clock icon and is for real alarms).
        // Games use inexact/allow-while-idle alarms + a normal notification channel.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Log.i(TAG, "Return reminder scheduled in " + (RETURN_REMINDER_DELAY_MS / 1000) + "s");
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

    /** Fetch FCM device token and store/register it (for real push like energy-full games). */
    private void fetchAndRegisterFcmToken() {
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> {
                        if (token == null || token.trim().isEmpty()) return;
                        Log.i(TAG, "FCM token ready");
                        registerPushToken("fcm", token);
                    })
                    .addOnFailureListener(e -> Log.w(TAG, "FCM token fetch failed", e));
        } catch (Exception e) {
            Log.w(TAG, "FCM not available yet", e);
        }
    }

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

    // ── Appodeal ads ─────────────────────────────────────────────────────────

    private void initAppodeal() {
        bannerView = findViewById(R.id.appodealBannerView);
        nativeAdView = findViewById(R.id.appodealNativeAdView);
        if (bannerView != null) bannerView.setVisibility(View.GONE);
        if (nativeAdView != null) nativeAdView.setVisibility(View.GONE);

        Appodeal.setAutoCache(AD_TYPES, true);
        Appodeal.setBannerViewId(R.id.appodealBannerView);
        Appodeal.setSharedAdsInstanceAcrossActivities(true);
        registerAppodealCallbacks();
        Appodeal.initialize(this, APPODEAL_APP_KEY, AD_TYPES,
                new ApdInitializationCallback() {
                    @Override public void onInitializationFinished(List<ApdInitializationError> errors) {
                        adsInitialized = true;
                        if (errors != null && !errors.isEmpty()) {
                            for (ApdInitializationError error : errors) {
                                Log.w(TAG, "Appodeal initialization issue: " + error);
                            }
                        } else {
                            Log.i(TAG, "Appodeal initialized successfully");
                        }
                        Appodeal.cache(MainActivity.this, Appodeal.INTERSTITIAL);
                        Appodeal.cache(MainActivity.this, Appodeal.REWARDED_VIDEO);
                        Appodeal.cache(MainActivity.this, Appodeal.BANNER);
                        Appodeal.cache(MainActivity.this, Appodeal.NATIVE);
                        showBanner();
                    }
                });
    }

    private void registerAppodealCallbacks() {
        Appodeal.setBannerCallbacks(new BannerCallbacks() {
            @Override public void onBannerLoaded(int height, boolean isPrecache) {
                Log.i(TAG, "Banner loaded");
                if (bannerRequested) showBanner();
            }
            @Override public void onBannerFailedToLoad() {
                Log.w(TAG, "Banner failed to load; retrying cache");
                adHandler.postDelayed(() -> Appodeal.cache(MainActivity.this, Appodeal.BANNER), 3000L);
            }
            @Override public void onBannerShown() { Log.i(TAG, "Banner shown"); }
            @Override public void onBannerShowFailed() { Log.w(TAG, "Banner show failed"); }
            @Override public void onBannerClicked() { Log.i(TAG, "Banner clicked"); }
            @Override public void onBannerExpired() { Appodeal.cache(MainActivity.this, Appodeal.BANNER); }
        });

        Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
            @Override public void onInterstitialLoaded(boolean isPrecache) {
                Log.i(TAG, "Interstitial loaded");
                if (pendingInterstitial) {
                    pendingInterstitial = false;
                    showInterstitialAd();
                }
            }
            @Override public void onInterstitialFailedToLoad() { pendingInterstitial = false; }
            @Override public void onInterstitialShown() { Log.i(TAG, "Interstitial shown"); }
            @Override public void onInterstitialShowFailed() { Appodeal.cache(MainActivity.this, Appodeal.INTERSTITIAL); }
            @Override public void onInterstitialClicked() { Log.i(TAG, "Interstitial clicked"); }
            @Override public void onInterstitialClosed() { Appodeal.cache(MainActivity.this, Appodeal.INTERSTITIAL); }
            @Override public void onInterstitialExpired() { Appodeal.cache(MainActivity.this, Appodeal.INTERSTITIAL); }
        });

        Appodeal.setRewardedVideoCallbacks(new RewardedVideoCallbacks() {
            @Override public void onRewardedVideoLoaded(boolean isPrecache) {
                Log.i(TAG, "Rewarded video loaded");
                if (pendingRewarded) showRewardedAdInternal();
            }
            @Override public void onRewardedVideoFailedToLoad() {
                pendingRewarded = false;
                fireJsEvent("reward:cancelled");
            }
            @Override public void onRewardedVideoShown() { Log.i(TAG, "Rewarded video shown"); }
            @Override public void onRewardedVideoShowFailed() {
                pendingRewarded = false;
                fireJsEvent("reward:cancelled");
                Appodeal.cache(MainActivity.this, Appodeal.REWARDED_VIDEO);
            }
            @Override public void onRewardedVideoClicked() { Log.i(TAG, "Rewarded clicked"); }
            @Override public void onRewardedVideoFinished(double amount, String name) {
                rewardEarned = true;
                if (pendingRewardCallback != null) {
                    pendingRewardCallback.run();
                    pendingRewardCallback = null;
                }
                fireJsEvent("reward:double_score");
            }
            @Override public void onRewardedVideoClosed(boolean finished) {
                if (!rewardEarned && !finished) {
                    pendingRewardCallback = null;
                    fireJsEvent("reward:cancelled");
                }
                rewardEarned = false;
                pendingRewarded = false;
                Appodeal.cache(MainActivity.this, Appodeal.REWARDED_VIDEO);
            }
            @Override public void onRewardedVideoExpired() { Appodeal.cache(MainActivity.this, Appodeal.REWARDED_VIDEO); }
        });

        Appodeal.setNativeCallbacks(new NativeCallbacks() {
            @Override public void onNativeLoaded() {
                nativeAdLoaded = true;
                if (nativeAdRequested) showNativeAdInternal();
            }
            @Override public void onNativeFailedToLoad() {
                nativeAdLoaded = false;
                if (nativeAdRequested) adHandler.postDelayed(() -> Appodeal.cache(MainActivity.this, Appodeal.NATIVE), 4000L);
            }
            @Override public void onNativeShown(NativeAd ad) { Log.i(TAG, "Native ad shown"); }
            @Override public void onNativeShowFailed(NativeAd ad) { Log.w(TAG, "Native ad show failed"); }
            @Override public void onNativeClicked(NativeAd ad) { Log.i(TAG, "Native ad clicked"); }
            @Override public void onNativeExpired() { nativeAdLoaded = false; }
        });
    }

    void showBanner() {
        runOnUiThread(() -> {
            bannerRequested = true;
            if (!adsInitialized) return;
            boolean shown = false;
            if (bannerView != null) {
                bannerView.setVisibility(View.VISIBLE);
                shown = Appodeal.show(this, Appodeal.BANNER_VIEW);
            }
            if (!shown) Appodeal.cache(this, Appodeal.BANNER);
        });
    }

    void hideBanner() {
        bannerRequested = false;
        runOnUiThread(() -> {
            if (bannerView != null) bannerView.setVisibility(View.GONE);
        });
    }

    private void showNativeAdInternal() {
        runOnUiThread(() -> {
            if (!nativeAdRequested || nativeAdView == null || !Appodeal.isLoaded(Appodeal.NATIVE)) return;
            List<NativeAd> ads = Appodeal.getNativeAds(1);
            if (ads == null || ads.isEmpty()) {
                Appodeal.cache(this, Appodeal.NATIVE);
                return;
            }
            try { nativeAdView.unregisterView(); } catch (Exception ignored) {}
            if (nativeAdView.registerView(ads.get(0))) {
                nativeAdView.setVisibility(View.VISIBLE);
                nativeAdLoaded = true;
            }
        });
    }

    void showNativeAd() {
        nativeAdRequested = true;
        if (nativeAdLoaded) showNativeAdInternal();
        else Appodeal.cache(this, Appodeal.NATIVE);
    }

    void hideNativeAd() {
        runOnUiThread(() -> {
            if (nativeAdView != null) {
                try { nativeAdView.unregisterView(); } catch (Exception ignored) {}
                nativeAdView.setVisibility(View.GONE);
            }
            nativeAdRequested = false;
        });
    }

    // Keep the existing feed bridge harmless and compatible with old game calls.
    void showFeedAd() { showNativeAd(); }
    void hideFeedAd() { hideNativeAd(); }

    void showInterstitialAd() {
        runOnUiThread(() -> {
            if (!adsInitialized) { pendingInterstitial = true; return; }
            if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                pendingInterstitial = !Appodeal.show(this, Appodeal.INTERSTITIAL);
                if (pendingInterstitial) Appodeal.cache(this, Appodeal.INTERSTITIAL);
            } else {
                pendingInterstitial = true;
                Appodeal.cache(this, Appodeal.INTERSTITIAL);
            }
        });
    }

    private void showRewardedAdInternal() {
        runOnUiThread(() -> {
            if (!adsInitialized) { pendingRewarded = true; return; }
            if (Appodeal.isLoaded(Appodeal.REWARDED_VIDEO)) {
                pendingRewarded = false;
                rewardEarned = false;
                if (!Appodeal.show(this, Appodeal.REWARDED_VIDEO)) {
                    fireJsEvent("reward:cancelled");
                }
            } else {
                pendingRewarded = true;
                Appodeal.cache(this, Appodeal.REWARDED_VIDEO);
            }
        });
    }

    void showRewardedAd(Runnable onRewarded) {
        // Preserve the existing bridge callback as well as the Appodeal JS event.
        pendingRewardCallback = onRewarded;
        showRewardedAdInternal();
    }

    void loadInterstitialAd() { Appodeal.cache(this, Appodeal.INTERSTITIAL); }
    void loadRewardedAd() { Appodeal.cache(this, Appodeal.REWARDED_VIDEO); }
    void showAppOpenAd() { /* Appodeal migration keeps the existing lifecycle hook harmless. */ }

    private void fireJsEvent(String event) {
        adHandler.post(() -> {
            if (webView != null) {
                String js = "try{if(window.app&&typeof window.app.fire==='function'){app.fire('" + event + "');}}catch(e){}";
                webView.evaluateJavascript(js, null);
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
