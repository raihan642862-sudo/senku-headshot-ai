# SENKU HEADSHOT AI ProGuard Rules
-keepattributes *Annotation*,InnerClasses,Signature
-keep class com.senku.headshotai.data.local.** { *; }
-keep class com.senku.headshotai.domain.model.** { *; }
-dontwarn kotlinx.coroutines.**
