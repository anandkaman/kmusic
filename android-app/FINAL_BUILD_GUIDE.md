# KMusic Android App - Final Build Guide

## Overview
This guide will help you build the final release APK for the KMusic Android application. The app is ready to build, and this document contains all the steps needed to create a production-ready APK.

---

## Prerequisites

### 1. Java Development Kit (JDK)
- **JDK 17** is installed at: `C:\Program Files\Java\jdk-17`
- Verify: `java -version`

### 2. Android SDK
- **Android SDK** is installed at: `C:\Android\Sdk`
- Configured in: `local.properties`

### 3. Gradle
- **Gradle 8.4** (configured via wrapper)
- The project uses the Gradle wrapper, so no separate installation needed

### 4. Keystore
- **Keystore file**: `kmusic-release.jks` (located in project root)
- You have created this keystore and need to configure the credentials

---

## Build Configuration

### Current Setup

The project has been configured with:
- ✅ KSP (Kotlin Symbol Processing) instead of deprecated kapt
- ✅ Gradle 8.4 for compatibility
- ✅ Android Gradle Plugin 8.2.2
- ✅ Kotlin 1.9.22
- ✅ Launcher icons created
- ✅ All dependencies properly configured
- ✅ Compilation errors fixed

### Key Files

1. **gradle.properties** - Contains keystore configuration placeholders
2. **app/build.gradle** - Build configuration with signing config
3. **local.properties** - Android SDK path (git-ignored)

---

## Step-by-Step: Building Release APK

### Step 1: Configure Keystore Credentials

Edit `gradle.properties` and update these lines with your actual keystore credentials:

```properties
# Keystore configuration (update these values with your actual credentials)
KMUSIC_KEYSTORE_PASSWORD=your_actual_keystore_password
KMUSIC_KEY_ALIAS=your_actual_key_alias
KMUSIC_KEY_PASSWORD=your_actual_key_password
```

**Important:**
- Replace `your_actual_keystore_password` with the password you set when creating the keystore
- Replace `your_actual_key_alias` with the alias name you used
- Replace `your_actual_key_password` with the key password you set

### Step 2: Set Environment Variables

Before building, set the JAVA_HOME environment variable:

**In Git Bash or WSL:**
```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"
```

**In Windows Command Prompt:**
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
```

**In PowerShell:**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

### Step 3: Clean Previous Builds

Navigate to the project directory and clean:

```bash
cd d:\streaming\android-app
bash gradlew clean
```

### Step 4: Build Release APK

Build the signed release APK:

```bash
bash gradlew assembleRelease
```

This will:
- Compile the Kotlin code
- Process resources
- Run ProGuard (code obfuscation and minification)
- Shrink resources
- Sign the APK with your keystore
- Create the final release APK

**Build time:** Approximately 1-3 minutes depending on your system

### Step 5: Locate the APK

After a successful build, find your APK at:

```
app/build/outputs/apk/release/app-release.apk
```

Check the file:
```bash
ls -lh app/build/outputs/apk/release/app-release.apk
```

---

## Build Variants

### Debug Build (Already Built)
- **Command:** `bash gradlew assembleDebug`
- **Location:** `app/build/outputs/apk/debug/app-debug.apk`
- **Size:** ~12 MB
- **Features:**
  - Not minified
  - Includes debug symbols
  - Different package name: `com.kmusic.debug`
  - Not signed with release keystore

### Release Build (Production)
- **Command:** `bash gradlew assembleRelease`
- **Location:** `app/build/outputs/apk/release/app-release.apk`
- **Expected Size:** ~6-8 MB (smaller due to minification)
- **Features:**
  - Code obfuscation via ProGuard
  - Resource shrinking enabled
  - Signed with release keystore
  - Optimized for production
  - Package name: `com.kmusic`

---

## Build Configuration Details

### Signing Configuration (from app/build.gradle)

```gradle
signingConfigs {
    release {
        storeFile file(rootProject.file("kmusic-release.jks"))
        storePassword project.hasProperty("KMUSIC_KEYSTORE_PASSWORD") ?
            project.getProperty("KMUSIC_KEYSTORE_PASSWORD") : ""
        keyAlias project.hasProperty("KMUSIC_KEY_ALIAS") ?
            project.getProperty("KMUSIC_KEY_ALIAS") : ""
        keyPassword project.hasProperty("KMUSIC_KEY_PASSWORD") ?
            project.getProperty("KMUSIC_KEY_PASSWORD") : ""
    }
}
```

### ProGuard Configuration

The release build uses ProGuard with:
- `minifyEnabled = true` - Code shrinking and obfuscation
- `shrinkResources = true` - Remove unused resources
- `proguard-android-optimize.txt` - Optimized rules
- Custom rules in `app/proguard-rules.pro`

---

## Troubleshooting

### Build Fails with "SDK location not found"
**Solution:** Verify `local.properties` exists with:
```properties
sdk.dir=C:\\Android\\Sdk
```

### Build Fails with "JAVA_HOME not set"
**Solution:** Set JAVA_HOME environment variable before running gradlew:
```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
```

### Build Fails with "Keystore password incorrect"
**Solution:** Double-check the credentials in `gradle.properties`

### Build Fails with Compilation Errors
**Solution:** Run a clean build:
```bash
bash gradlew clean assembleRelease
```

### Gradle Daemon Issues
**Solution:** Stop all Gradle daemons and rebuild:
```bash
bash gradlew --stop
bash gradlew assembleRelease
```

---

## Verification Steps

After building the release APK, verify it:

### 1. Check APK Signature
```bash
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk
```

Should show: "jar verified."

### 2. Check APK Contents
```bash
unzip -l app/build/outputs/apk/release/app-release.apk
```

### 3. Get APK Info
Using Android SDK build tools:
```bash
"C:\Android\Sdk\build-tools\34.0.0\aapt" dump badging app/build/outputs/apk/release/app-release.apk
```

---

## App Information

### App Details
- **Package Name:** `com.kmusic`
- **Version Code:** 1
- **Version Name:** 1.0.0
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Compile SDK:** 34

### Permissions Required
- INTERNET - For streaming music
- ACCESS_NETWORK_STATE - Check network connectivity
- FOREGROUND_SERVICE - Background music playback
- FOREGROUND_SERVICE_MEDIA_PLAYBACK - Media playback service
- WAKE_LOCK - Keep playing when screen is off
- POST_NOTIFICATIONS - Show playback notifications
- READ_MEDIA_AUDIO - Access audio files (Android 13+)

---

## Distribution

### Before Distribution

1. **Test the APK thoroughly:**
   - Install on multiple Android devices
   - Test all features (login, playback, settings)
   - Verify server connectivity
   - Test offline behavior

2. **Check file size:**
   - Should be 6-8 MB for release build
   - If larger, verify ProGuard is enabled

3. **Verify signing:**
   - Ensure APK is signed with your release keystore
   - Keep your keystore file and passwords secure

### Distribution Options

1. **Direct APK Distribution:**
   - Share `app-release.apk` directly
   - Users need to enable "Install from Unknown Sources"

2. **Google Play Store:**
   - Create a Google Play Developer account
   - Follow Play Store submission guidelines
   - Upload app-release.apk or generate AAB (Android App Bundle)

3. **Internal Distribution:**
   - Use Firebase App Distribution
   - Use TestFlight alternatives for Android
   - Email or cloud storage for testers

---

## Quick Reference Commands

```bash
# Navigate to project
cd d:\streaming\android-app

# Set JAVA_HOME (Git Bash/WSL)
export JAVA_HOME="/c/Program Files/Java/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"

# Clean build
bash gradlew clean

# Build debug APK (for testing)
bash gradlew assembleDebug

# Build release APK (for production)
bash gradlew assembleRelease

# View all build tasks
bash gradlew tasks

# Check Gradle version
bash gradlew --version

# Stop Gradle daemon
bash gradlew --stop
```

---

## Important Security Notes

### Keep These Secret
- ❌ **kmusic-release.jks** - Never commit to git or share publicly
- ❌ **gradle.properties** (with actual passwords) - Add to .gitignore
- ❌ **Keystore passwords** - Store securely, never in code

### Backup
- ✅ Backup your keystore file to a secure location
- ✅ Save keystore passwords in a password manager
- ✅ If you lose the keystore, you cannot update the app on Play Store

---

## Next Steps After Building

1. **Install and test the release APK** on real devices
2. **Configure your backend server** (based on server settings in the app)
3. **Prepare store listing** if publishing to Google Play
4. **Create promotional materials** (screenshots, description, icon)
5. **Set up analytics** (optional, would require adding Firebase or similar)

---

## Support

For build issues or questions:
1. Check the build output for specific error messages
2. Review this guide's Troubleshooting section
3. Verify all prerequisites are met
4. Check that files haven't been modified unintentionally

---

## Build History

- **First successful debug build:** December 4, 2025
- **Configuration:** Migrated from kapt to KSP, Gradle 8.4
- **Issues resolved:**
  - Duplicate resources in strings.xml
  - Missing launcher icons
  - API compatibility issues
  - MediaSession API updates

---

**Good luck with your release! 🎵**
