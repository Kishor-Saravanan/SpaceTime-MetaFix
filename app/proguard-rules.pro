# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep ExifInterface
-keep class androidx.exifinterface.media.** { *; }

# Keep Apache Commons CSV
-keep class org.apache.commons.csv.** { *; }

# Keep MP4Parser
-keep class com.coremedia.** { *; }
-keep class com.googlecode.mp4parser.** { *; }
-keep class org.mp4parser.** { *; }

-dontwarn org.apache.commons.**
-dontwarn com.coremedia.**
-dontwarn com.googlecode.mp4parser.**
