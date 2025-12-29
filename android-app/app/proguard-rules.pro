# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep BMS data classes
-keep class com.smartbms.utility.data.** { *; }

# Keep Bluetooth classes
-keep class com.smartbms.utility.bluetooth.** { *; }
