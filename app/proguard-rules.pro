# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve generic type signatures (essential for Gson's TypeToken) and annotations
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Keep Gson annotations and TypeToken subclasses
-keep class com.google.gson.annotations.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

-keep class com.eyalm.adns.data.models.** { *; }
-keep class com.eyalm.adns.data.nextdns.api.** { *; }
-keep class com.eyalm.adns.data.wifi.WifiSsid { *; }
-keep class com.eyalm.adns.data.wifi.WifiRuleSuspension { *; }
-keep class com.eyalm.adns.data.wifi.WifiSuspensionPhase { *; }
-keep class com.eyalm.adns.data.wifi.StoredWifiRulesSnapshot { *; }
-keep class com.eyalm.adns.data.wifi.WifiRulesConfiguration { *; }
-keep class com.eyalm.adns.data.runtime.RuntimeMonitoringPreferences { *; }
-keep class com.eyalm.adns.data.runtime.RuntimeMonitoringSystemState { *; }
-keep class com.eyalm.adns.data.runtime.RuntimeMonitoringState { *; }
-keep class com.eyalm.adns.data.runtime.RuntimeServicePlan { *; }
-keep class com.eyalm.adns.data.activation.ActivationState { *; }
-keep class com.eyalm.adns.data.activation.ActivationMode { *; }
-keep class com.eyalm.adns.domain.AppCapabilities { *; }
-keep class com.eyalm.adns.domain.AppDestination { *; }
-keep class com.eyalm.adns.domain.MainTab { *; }

-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }