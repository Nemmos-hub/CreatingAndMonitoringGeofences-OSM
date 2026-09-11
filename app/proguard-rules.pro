# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# BroadcastReceiver
-keep class * extends android.content.BroadcastReceiver {
    <init>(...);
}