# Add project specific ProGuard rules here.

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.redshirt.warpcore.data.** { *; }

# Nordic BLE
-keep class no.nordicsemi.** { *; }
-dontwarn no.nordicsemi.**