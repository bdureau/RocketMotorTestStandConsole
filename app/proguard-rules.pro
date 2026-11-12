# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------------------------
# Rules for the release build (minifyEnabled true).
# The local libraries below are kept entirely: some of them use reflection or are only
# referenced from code that R8 cannot fully analyse (USB drivers chosen at run time).
# ---------------------------------------------------------------------------------------------

# AFreeChart (charts on Android < 8)
-keep class org.afree.** { *; }
-dontwarn org.afree.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# USB serial library (felhr) and its okio dependency
-keep class com.felhr.** { *; }
-dontwarn com.felhr.**
-keep class okio.** { *; }
-dontwarn okio.**

# Physicaloid (firmware upload)
-keep class com.physicaloid.** { *; }
-dontwarn com.physicaloid.**

# ViewTooltip
-keep class com.github.florent37.viewtooltip.** { *; }
-dontwarn com.github.florent37.viewtooltip.**

# Keep the line numbers in the crash reports of the Play Console
-keepattributes SourceFile,LineNumberTable
