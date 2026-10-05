# Release builds are minified. Keep rules are deliberately minimal: the app
# parses JSON with org.json (platform class) and OkHttp/ML Kit/Media3 ship their
# own consumer rules. Add rules here only for a verified release-build failure.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
