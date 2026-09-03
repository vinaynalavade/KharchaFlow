# Leaf — GitHub Release & In-App APK Update Specification

This document defines the release specification and publication procedures for Leaf Android application releases on GitHub Releases (`vinaynalavade/Leaf`).

The Leaf in-app update client supports seamless, reliable update checks, asset downloads, integrity validation, and package installer handoff.

---

## 1. Quick Release Workflow (Standard Procedure)

For every new release (e.g. `v1.0.5`):

### Step 1: Update App Version
In `app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 6
    versionName = "1.0.5"
}
```

### Step 2: Build the Signed Release APK
Run the Gradle release task:
```powershell
.\gradlew.bat clean assembleRelease
```
The output APK is located at:
`app/build/outputs/apk/release/app-release.apk`

### Step 3: Name the Release APK
Rename the generated APK according to the version:
```powershell
Copy-Item "app\build\outputs\apk\release\app-release.apk" "Leaf_v1.0.5.apk"
```

### Step 4: Publish to GitHub Releases
1. Create a tag matching the version (e.g. `v1.0.5`).
2. Draft a new release on GitHub.
3. Include the standard Version Information table in the release description:
   ```markdown
   ## 📦 Version Information
   | | |
   |---|---|
   | **Version** | `1.0.5` |
   | **Version Code** | `6` |
   | **Release Type** | APK / Direct Distribution |
   ```
4. Attach the release APK: `Leaf_v1.0.5.apk`.
5. Publish the release.

The in-app updater automatically discovers the release, extracts the remote `versionCode` (6 > 5), downloads the APK, verifies package integrity, and launches the Android installer.

---

## 2. Optional Enhanced Security Assets

For additional validation support, you can also attach either/both of the following optional assets:

### Optional: SHA-256 Checksum Asset (`Leaf_vX.Y.Z.apk.sha256`)
```powershell
$hash = (Get-FileHash -Path "Leaf_v1.0.5.apk" -Algorithm SHA256).Hash.ToLower()
"$hash  Leaf_v1.0.5.apk" | Out-File -FilePath "Leaf_v1.0.5.apk.sha256" -Encoding ascii -NoNewline
```

### Optional: Machine-Readable Manifest (`release.json`)
```json
{
  "versionName": "1.0.5",
  "versionCode": 6,
  "apkFileName": "Leaf_v1.0.5.apk",
  "sha256FileName": "Leaf_v1.0.5.apk.sha256",
  "releaseNotes": "• What's new in v1.0.5\n• Performance optimizations",
  "releaseUrl": "https://github.com/vinaynalavade/Leaf/releases/tag/v1.0.5"
}
```

---

## 3. Client Validation & Update Lifecycle

1. **Discovery**: Queries `https://api.github.com/repos/vinaynalavade/Leaf/releases/latest`.
2. **Version Code Evaluation**:
   - `remote versionCode > installed versionCode` $\to$ **Update Available**
   - `remote versionCode == installed versionCode` $\to$ **Up To Date** ("You're using the latest version.")
   - `remote versionCode < installed versionCode` $\to$ **Rejected** (Downgrade protection)
3. **APK Streaming**: Streams the APK over HTTPS with progress indicator, storing in app-private cache (`cacheDir/updates/`).
4. **Integrity Validation**:
   - SHA-256 verified if checksum is provided via asset digest, `.sha256` asset, or `release.json`.
   - Android PackageArchive validation: verifies package name `com.vinaynalavade.expensetracker` and `versionCode`.
5. **Android Installer Handoff**: Hands off verified APK to Android OS official Package Installer via `FileProvider` (`content://com.vinaynalavade.expensetracker.fileprovider/updates/...`).
