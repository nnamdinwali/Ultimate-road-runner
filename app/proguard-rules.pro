# Yandex Mobile Ads
-keep class com.yandex.mobile.ads.** { *; }
-dontwarn com.yandex.mobile.ads.**

# Keep the bridge so JS @JavascriptInterface methods are never stripped
-keepclassmembers class com.roadrunner.game.AndroidBridge {
    public *;
}
-keep class com.roadrunner.game.AndroidBridge { *; }
-keep class com.roadrunner.game.MainActivity { *; }
