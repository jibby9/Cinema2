# ProGuard & R8 configuration rules for IPTV and Media3 robustness

# Preserve our IPTV and config models from being obfuscated, ensuring serialization/caching remains intact
-keepclassmembers class com.example.XtreamAccount { *; }
-keepclassmembers class com.example.M3UConfig { *; }
-keepclassmembers class com.example.IptvCategory { *; }
-keepclassmembers class com.example.IptvChannel { *; }
-keepclassmembers class com.example.IptvHistoryItem { *; }
-keepclassmembers class com.example.EpgProgramme { *; }

# Keep classes themselves and their properties
-keep class com.example.XtreamAccount { *; }
-keep class com.example.M3UConfig { *; }
-keep class com.example.IptvCategory { *; }
-keep class com.example.IptvChannel { *; }
-keep class com.example.IptvHistoryItem { *; }
-keep class com.example.EpgProgramme { *; }

# Keep Moshi adapter classes if generated
-keep class com.example.**JsonAdapter { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }

# Media3 / ExoPlayer R8 and Reflection rules
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# Maintain general attributes for JSON/Serialization reflection support
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
-dontwarn com.squareup.moshi.**
