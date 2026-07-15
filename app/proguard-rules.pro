# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve generic type signatures (essential for Gson's TypeToken) and annotations
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Keep Gson annotations and TypeToken subclasses
-keep class com.google.gson.annotations.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

-keep class com.kernelpanic.baladdns.data.models.** { *; }
-keep class com.kernelpanic.baladdns.data.nextdns.api.** { *; }
-keep class com.kernelpanic.baladdns.data.wifi.WifiSsid { *; }
-keep class com.kernelpanic.baladdns.data.wifi.WifiRuleSuspension { *; }
-keep class com.kernelpanic.baladdns.data.wifi.WifiSuspensionPhase { *; }
-keep class com.kernelpanic.baladdns.data.wifi.StoredWifiRulesSnapshot { *; }
-keep class com.kernelpanic.baladdns.data.wifi.WifiRulesConfiguration { *; }
-keep class com.kernelpanic.baladdns.data.runtime.RuntimeMonitoringPreferences { *; }
-keep class com.kernelpanic.baladdns.data.runtime.RuntimeMonitoringSystemState { *; }
-keep class com.kernelpanic.baladdns.data.runtime.RuntimeMonitoringState { *; }
-keep class com.kernelpanic.baladdns.data.runtime.RuntimeServicePlan { *; }
-keep class com.kernelpanic.baladdns.data.activation.ActivationState { *; }
-keep class com.kernelpanic.baladdns.data.activation.ActivationMode { *; }
-keep class com.kernelpanic.baladdns.domain.AppCapabilities { *; }
-keep class com.kernelpanic.baladdns.domain.AppDestination { *; }
-keep class com.kernelpanic.baladdns.domain.MainTab { *; }

-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }