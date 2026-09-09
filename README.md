# TvPatcher

A from-scratch Android Gradle project for the TvPatcher companion app.

## Current scaffold

- Standard Android Gradle project.
- Detects Bytezuku (`com.byteus.bytezuku`).
- Detects normal Gorilla Tag (`com.AnotherAxiom.GorillaTag`).
- Detects modded Gorilla Tag (`com.TvMods.GorillaTag`).
- Launches either installed game.
- UI includes patch-access, cache-backup, OBB-copy, and auth status controls.
- `build.yml` is included.
- No cross-app input/cache interception is implemented.

## Important implementation note

Android does not provide a universal API for an ordinary app to copy another app's private OBB/cache data. The patch operations therefore need an explicitly user-granted elevated service such as the exact Bytezuku API supported by the target version.

The Bytezuku package name supplied for this project is:

`com.byteus.bytezuku`

Before wiring privileged calls, provide the exact Bytezuku SDK/API or AIDL/interface used by the installed version. This scaffold deliberately avoids pretending an unknown API exists.

## Build

Use JDK 17 and a compatible Android SDK. The project is configured for AGP 9.4.0 / Gradle 9.6.0.

```bash
./gradlew assembleDebug
```

APK:

`app/build/outputs/apk/debug/app-debug.apk`
