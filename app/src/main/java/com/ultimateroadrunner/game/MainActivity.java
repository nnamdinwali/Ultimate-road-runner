package com.ultimateroadrunner.game;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import com.appodeal.ads.Appodeal;
import com.appodeal.ads.BannerCallbacks;
import com.appodeal.ads.InterstitialCallbacks;

public class MainActivity extends AppCompatActivity {

    private static final String APP_KEY = "d7441b7444df839562102f3e95a44793d98cd126509b5ce2";
    private WebView webView;
    private int levelCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAppodeal();
        initWebView();
    }

    private void initAppodeal() {
        Appodeal.setTesting(false);
        Appodeal.initialize(this, APP_KEY,
                Appodeal.BANNER | Appodeal.INTERSTITIAL, true);

        Appodeal.setBannerCallbacks(new BannerCallbacks() {
            @Override public void onBannerLoaded(int height, boolean isPrecache) {}
            @Override public void onBannerFailedToLoad() {}
            @Override public void onBannerShown() {}
            @Override public void onBannerShowFailed() {}
            @Override public void onBannerClicked() {}
            @Override public void onBannerExpired() {}
        });

        Appodeal.setInterstitialCallbacks(new InterstitialCallbacks() {
            @Override public void onInterstitialLoaded(boolean isPrecache) {}
            @Override public void onInterstitialFailedToLoad() {}
            @Override public void onInterstitialShown() {}
            @Override public void onInterstitialShowFailed() {}
            @Override public void onInterstitialClicked() {}
            @Override public void onInterstitialClosed() {}
            @Override public void onInterstitialExpired() {}
        });

        Appodeal.setBannerViewId(R.id.appodealBannerView);
        Appodeal.show(this, Appodeal.BANNER_VIEW);
    }

    private void initWebView() {
        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                injectAppodealBridge();
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        webView.addJavascriptInterface(new AppodealBridge(this), "AppodealBridge");

        webView.loadUrl("file:///android_asset/game/index.html");
    }

    private void injectAppodealBridge() {
        String js = "window.showInterstitialAd = function() { AppodealBridge.showInterstitial(); };" +
                    "window.isInterstitialReady = function() { return AppodealBridge.isInterstitialReady(); };";
        webView.evaluateJavascript(js, null);
    }

    public void showInterstitialAd() {
        if (Appodeal.isLoaded(Appodeal.INTERSTITIAL)) {
            Appodeal.show(this, Appodeal.INTERSTITIAL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Appodeal.onResume(this, Appodeal.BANNER);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Appodeal.onPause(this, Appodeal.BANNER);
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
