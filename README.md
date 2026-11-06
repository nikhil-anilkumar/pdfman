# pdfman

pdfman is a small Android app that turns photos into PDF pages using grid layouts. Pick some photos, choose how many go on a page, and save.

<img src="fastlane/metadata/android/en-US/images/icon.png" width="96" alt="pdfman icon">

## Features

- Any grid from 1x1 to 6x6, with fit or crop-to-fill placement, auto rotate and optional cell borders
- 32 paper sizes (ISO A/B, North American, JIS and photo sizes), portrait or landscape
- Margin and spacing sliders, three quality levels, and a live page preview that stays pinned at the top
- Share photos into pdfman straight from your gallery
- Saved PDFs appear on the home screen and can be reopened, edited and saved again

## Privacy

pdfman works fully offline. It doesn't request the internet permission and has no ads, analytics or tracking. Photos are picked through the system photo picker, so the app only sees the ones you choose.

## Requirements

Android 9 (API 28) or newer.

## Building

You need JDK 17 and the Android SDK. Point `local.properties` at your SDK (Android Studio does this for you) or set `ANDROID_HOME`.

```bash
./gradlew assembleDebug        # debug APK, package com.pdfman.app.debug
./gradlew testDebugUnitTest    # unit tests
./gradlew assembleRelease      # release APK
```

The release APK ends up in `app/build/outputs/apk/release/`. Keep `app/build/outputs/mapping/release/mapping.txt` from each release if you want to make sense of crash traces later.

### Release signing

Release builds are signed using `keystore.properties` in the project root. That file and the `keystore/` folder are ignored by git. If `keystore.properties` isn't there, the release build is simply left unsigned, which is what F-Droid expects since it signs apps itself.

```properties
storeFile=keystore/pdfman-release.jks
storePassword=...
keyAlias=pdfman
keyPassword=...
```

Back the keystore up somewhere safe. Without it you can't ship updates to people who installed your signed build.

## Releasing a new version

1. Bump `versionCode` by one and update `versionName` in `app/build.gradle.kts`.
2. Add a changelog at `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500 characters max).
3. Commit, then tag the commit with the version name, for example `git tag v1.0.0`.
4. Push the commit and the tag.

F-Droid picks up new versions from the tags, so every release needs one.

## Store listing

The text and graphics shown on F-Droid live in `fastlane/metadata/android/`. Screenshots go in `fastlane/metadata/android/en-US/images/phoneScreenshots/` as `1.png`, `2.png` and so on.

## Contributing

Bug reports and pull requests are welcome. If you're planning something big, open an issue first so we can talk it through.

## License

pdfman is released under the [MIT License](LICENSE). Copyright (c) 2026 Nikhil Anilkumar.

The libraries it uses (AndroidX, Jetpack Compose, Kotlin and kotlinx.coroutines) are under the Apache License 2.0.
