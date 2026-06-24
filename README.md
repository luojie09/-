# Our Secret Base Android

Jetpack Compose Android homepage prototype for "我们的秘密基地".

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

The homepage preview data is fully static and does not depend on:

- network
- DataStore
- Android Context
- current system date

Preview entry points:

- `app/src/main/java/com/secretbase/app/ui/home/HomeScreenPreviews.kt`
- `app/src/screenshotTest/kotlin/com/secretbase/app/ui/home/HomeScreenScreenshotPreviews.kt`

## Screenshot testing

Reference screenshots live under:

```text
app/src/screenshotTestDebug/reference/
```

Generated reports and outputs:

```text
app/build/reports/screenshotTest/preview/debug/
app/build/outputs/screenshotTest/
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

## Notes

- `local.properties` is intentionally not committed.
- No Android Studio specific path is hard-coded in the project files.
- CI uses the project Gradle Wrapper and JDK 17.
