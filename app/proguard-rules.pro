# pdfman R8 rules.
# Compose, AndroidX and Kotlin ship their own consumer rules, and the app uses no reflection,
# so nothing extra needs to be kept. Saved creations store enum names (Enum.name / valueOf),
# which R8 preserves because the names are constructor string constants.

# Keep line numbers in crash stack traces; mapping.txt is in app/build/outputs/mapping/release/.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
