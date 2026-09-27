# R8 rules for release builds. The AndroidX/Compose/Room/DataStore libraries and the Kotlin
# runtime ship their own consumer rules, so this app needs very little of its own.

# Keep file names and line numbers so a de-obfuscated crash trace (via the build's mapping.txt)
# still points at real source lines, while hiding the original file names in the shipped APK.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
