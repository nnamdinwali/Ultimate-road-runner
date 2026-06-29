-keep class com.appodeal.** { *; }
-keep interface com.appodeal.** { *; }
-keep class com.explorestack.** { *; }
-keep interface com.explorestack.** { *; }
-dontwarn com.appodeal.**
-dontwarn com.explorestack.**

# Keep the AndroidBridge so JS can call it via @JavascriptInterface
-keepclassmembers class com.ultimateroadrunner.game.AndroidBridge {
    public *;
}
-keep class com.ultimateroadrunner.game.AndroidBridge { *; }
-keep class com.ultimateroadrunner.game.MainActivity { *; }
