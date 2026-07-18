package com.roadrunner.game;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "URR";

    private static final String PRIVACY_POLICY_URL =
            "https://nnamdinwali.github.io/ultimate-road-runner-privacy/";

    private WebView webView;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWebView();
        initPrivacyPolicyButton();
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

    // ── Privacy Policy (native overlay — GDevelop-proof) ─────────────────────

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

    // ── Privacy Policy ───────────────────────────────────────────────────────

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
