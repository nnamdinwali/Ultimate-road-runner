package com.ultimateroadrunner.game;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import androidx.appcompat.app.AppCompatActivity;
import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerCallbacks;
import com.appodeal.ads.InterstitialCallbacks;
import com.appodeal.ads.RewardedVideoCallbacks;

public class MainActivity extends AppCompatActivity {

    private static final String APP_KEY = "d7441b7444df839562102f3e95a44793d98cd126509b5ce2";
    WebView webView;
    private View bannerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bannerContainer = findViewById(R.id.bannerContainer);
        bannerContainer.setVisibility(View.GONE);

        initWebView();
        initAppodeal();
    }

    private void initAppodeal() {
        Appodeal.setTesting(false);

        // Appodeal 3.x: no consent boolean in initialize()
        Appodeal.initialize(this, APP_KEY,
                Appodeal.BANNER | Appodeal.INTERSTITIAL | Appodeal.REWARDED_VIDEO);

        Appodeal.setBannerCallbacks(new BannerCallbacks() {
            @Override public void onBannerLoaded(int height, boolean isPrecache) {}
            @Override public void onBannerFailedToLoad() {}
            @Override public void onBannerShown() {
                runOnUiThread(() -> fireJs("window.onBannerAdShown && window.onBannerAdShown()"));
            }
            @Override public void onBannerShowFailed() {}
            @Override public void onBannerClicked() {}
            @Override public void onBannerExpired() {}
        });

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

        Appodeal.setBannerViewId(R.id.appodealBannerView);
    }

    private void initWebView() {
        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/game/index.html");
    }

    void fireJs(String js) {
        webView.evaluateJavascript(js, null);
    }

    void showBanner() {
        runOnUiThread(() -> {
            bannerContainer.setVisibility(View.VISIBLE);
            Appodeal.show(this, Appodeal.BANNER_VIEW);
        });
    }

    void hideBanner() {
        runOnUiThread(() -> {
            bannerContainer.setVisibility(View.GONE);
            Appodeal.hide(this, Appodeal.BANNER_VIEW);
            fireJs("window.onBannerAdHidden && window.onBannerAdHidden()");
        });
    }

    void showInterstitialAd() {
        runOnUiThread(() -> {
            if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
                Appodeal.show(this, Appodeal.INTERSTITIAL);
            } else {
                fireJs("window.onInterstitialAdFailed && window.onInterstitialAdFailed()");
            }
        });
    }

    void showRewardedAd() {
        runOnUiThread(() -> {
            if (Appodeal.isLoaded(Appodeal.REWARDED_VIDEO)) {
                Appodeal.show(this, Appodeal.REWARDED_VIDEO);
            } else {
                fireJs("window.onRewardedAdFailed && window.onRewardedAdFailed()");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
