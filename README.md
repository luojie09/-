# Our Secret Base Android

Jetpack Compose Android client for "我们的秘密基地".

## Current Version

- Version: `1.2.1` (`versionCode` 7).
- The approved Plan B UI is now the official UI on `main`.
- `feature/ui-plan-b` remains available as the original UI revision branch.
- CI currently produces a debug APK; this is not a store-signed release build.
- Mobile push notifications are deferred and are not included in this version.
- Realtime's Auth runtime dependency is included to fix the post-pairing crash.

## Stack

- Kotlin 2.1.20
- Android Gradle Plugin 8.5.2
- Jetpack Compose Material 3
- Official Compose Preview Screenshot Testing
- GitHub Actions CI

## Local commands

Use the Gradle Wrapper from the repository root:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:validateDebugScreenshotTest
```

If you need to refresh screenshot baselines locally:

```bash
./gradlew :app:updateDebugScreenshotTest
```

## Preview data

The preview data is fully static and does not depend on:

- network
- DataStore
- Android Context
- current system date

Preview entry points:

- `app/src/main/java/com/secretbase/app/ui/home/HomeScreenPreviews.kt`
- `app/src/screenshotTest/kotlin/com/secretbase/app/ui/home/HomeScreenScreenshotPreviews.kt`
- `app/src/screenshotTest/kotlin/com/secretbase/app/ui/messagewall/MessageWallScreenshotPreviews.kt`
- `app/src/screenshotTest/kotlin/com/secretbase/app/ui/wishlist/WishListScreenshotPreviews.kt`
- `app/src/screenshotTest/kotlin/com/secretbase/app/ui/anniversary/AnniversaryScreenshotPreviews.kt`

## Screenshot testing

Reference screenshots live under:

```text
app/src/screenshotTestDebug/reference/
```

Generated reports and outputs:

```text
app/build/reports/screenshotTest/preview/debug/
app/build/outputs/screenshotTest/
app/build/outputs/screenshotTest-results/
```

## GitHub Actions

Workflow file:

```text
.github/workflows/android-ci.yml
```

The workflow runs on:

- push to `main`
- `pull_request`
- manual `workflow_dispatch`

Artifacts uploaded by CI:

- `app-debug-apk`
- `homepage-preview`

Build configuration and secure migration checks must pass before compilation.
The live Supabase health check reports a warning without blocking an offline APK
build. An APK artifact does not guarantee that the backend is currently reachable;
restore the backend before relying on cloud sync or device pairing.

Native startup regression checks run in `.github/workflows/android-startup-smoke.yml`
on Android API 26 and 34. They seed an unusable test session on a disposable emulator,
verify both roles render the real home screen for 60 seconds, then repeat a cold
start with the app's network blocked. These checks do not validate real pairing or
cloud data synchronization. Logs, UI hierarchy dumps, and screenshots are uploaded
as `paired-startup-api-*` artifacts.

## APK Updates

Debug APKs from separate CI runners may have different signing certificates.
Do not uninstall an existing app or clear its data to work around an update error.
Preserve the original signing keystore for compatible updates, and configure a
stable private release keystore before distributing production updates. A new
keystore cannot replace a lost original key for an in-place update.

## Notes

- `local.properties` is intentionally not committed.
- No Android Studio specific path is hard-coded in the project files.
- CI uses the project Gradle Wrapper and JDK 17.
