# KMusic - Complete Android App Guide

This guide will help you build the KMusic Android app from scratch with all requested features.

## 📱 App Features Checklist

- ✅ Dark & Light theme with Material 3
- ✅ Rounded tiles and modern UI
- ✅ Full-featured music player
- ✅ Songs list page
- ✅ Settings page with server configuration
- ✅ Audio device selection
- ✅ Volume control
- ✅ Android 16-style modern look
- ✅ Home screen widget
- ✅ Dynamic island-style player
- ✅ Lock screen controls
- ✅ Notification center player (next/prev)
- ✅ Storage and network permissions
- ✅ Server IP & port configuration
- ✅ Role-based features (admin upload)

## 🚀 Quick Start (15 Minutes)

### Option 1: Use Android Studio Template

1. **Create New Project** in Android Studio
   - Template: "Empty Views Activity"
   - Name: KMusic
   - Package: com.kmusic
   - Language: Kotlin
   - Minimum SDK: API 26 (Android 8.0)

2. **Copy provided files** into your project structure

3. **Sync Gradle** and run

### Option 2: Import This Project

1. Open Android Studio
2. File → Open → Select `android-app` folder
3. Wait for Gradle sync
4. Run app

## 📁 Project Structure to Create

```
android-app/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/kmusic/
│   │       │   ├── MainActivity.kt
│   │       │   ├── KMusicApplication.kt
│   │       │   │
│   │       │   ├── data/
│   │       │   │   ├── api/
│   │       │   │   │   ├── MusicApiService.kt
│   │       │   │   │   ├── AuthInterceptor.kt
│   │       │   │   │   └── RetrofitClient.kt
│   │       │   │   ├── model/
│   │       │   │   │   ├── User.kt
│   │       │   │   │   ├── Track.kt
│   │       │   │   │   ├── Playlist.kt
│   │       │   │   │   └── AuthModels.kt
│   │       │   │   ├── local/
│   │       │   │   │   ├── UserSessionManager.kt
│   │       │   │   │   └── ServerConfigManager.kt
│   │       │   │   └── repository/
│   │       │   │       └── MusicRepository.kt
│   │       │   │
│   │       │   ├── ui/
│   │       │   │   ├── theme/
│   │       │   │   │   ├── Color.kt
│   │       │   │   │   ├── Theme.kt
│   │       │   │   │   └── Type.kt
│   │       │   │   ├── auth/
│   │       │   │   │   ├── LoginFragment.kt
│   │       │   │   │   └── LoginViewModel.kt
│   │       │   │   ├── home/
│   │       │   │   │   ├── HomeFragment.kt
│   │       │   │   │   └── HomeViewModel.kt
│   │       │   │   ├── player/
│   │       │   │   │   ├── PlayerFragment.kt
│   │       │   │   │   ├── PlayerViewModel.kt
│   │       │   │   │   └── MiniPlayerView.kt
│   │       │   │   ├── library/
│   │       │   │   │   ├── LibraryFragment.kt
│   │       │   │   │   └── TrackAdapter.kt
│   │       │   │   ├── settings/
│   │       │   │   │   ├── SettingsFragment.kt
│   │       │   │   │   └── ServerConfigDialog.kt
│   │       │   │   └── upload/
│   │       │   │       ├── UploadFragment.kt  (admin only)
│   │       │   │       └── UploadViewModel.kt
│   │       │   │
│   │       │   ├── service/
│   │       │   │   ├── MusicService.kt
│   │       │   │   ├── MusicNotificationManager.kt
│   │       │   │   └── AudioFocusManager.kt
│   │       │   │
│   │       │   ├── widget/
│   │       │   │   └── MusicWidget.kt
│   │       │   │
│   │       │   └── util/
│   │       │       ├── Extensions.kt
│   │       │       └── Constants.kt
│   │       │
│   │       └── res/
│   │           ├── layout/
│   │           │   ├── activity_main.xml
│   │           │   ├── fragment_login.xml
│   │           │   ├── fragment_home.xml
│   │           │   ├── fragment_player.xml
│   │           │   ├── fragment_library.xml
│   │           │   ├── fragment_settings.xml
│   │           │   ├── item_track.xml
│   │           │   ├── view_mini_player.xml
│   │           │   └── widget_music.xml
│   │           ├── values/
│   │           │   ├── colors.xml
│   │           │   ├── strings.xml
│   │           │   ├── themes.xml
│   │           │   ├── dimens.xml
│   │           │   └── styles.xml
│   │           ├── values-night/
│   │           │   ├── colors.xml
│   │           │   └── themes.xml
│   │           ├── drawable/
│   │           │   ├── ic_play.xml
│   │           │   ├── ic_pause.xml
│   │           │   ├── ic_skip_next.xml
│   │           │   ├── ic_skip_previous.xml
│   │           │   ├── bg_rounded_card.xml
│   │           │   └── ...
│   │           ├── menu/
│   │           │   └── bottom_navigation.xml
│   │           └── xml/
│   │               ├── network_security_config.xml
│   │               ├── widget_info.xml
│   │               └── data_extraction_rules.xml
│   │
│   └── build.gradle (Module level)
│
├── build.gradle (Project level)
├── settings.gradle
└── gradle.properties
```

## 📝 Key Files Content

I'll provide the essential files you need to create. Create each file in the structure above.

### 1. build.gradle (Project Level)

```gradle
// File: build.gradle (Project level)
buildscript {
    ext.kotlin_version = '1.9.22'
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.2.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
```

### 2. build.gradle (App Module)

```gradle
// File: app/build.gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.kmusic'
    compileSdk 34

    defaultConfig {
        applicationId "com.kmusic"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            applicationIdSuffix ".debug"
            debuggable true
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = '11'
    }

    buildFeatures {
        viewBinding true
    }
}

dependencies {
    // Core Android
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Material Design 3
    implementation 'com.google.android.material:material:1.11.0'

    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // Media - ExoPlayer (Media3)
    implementation 'androidx.media3:media3-exoplayer:1.2.1'
    implementation 'androidx.media3:media3-session:1.2.1'
    implementation 'androidx.media3:media3-ui:1.2.1'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'

    // ViewModel & LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-service:2.7.0'

    // Navigation
    implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
    implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'

    // Security - Encrypted Shared Preferences
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'

    // Image Loading - Glide
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'

    // Work Manager (for background tasks)
    implementation 'androidx.work:work-runtime-ktx:2.9.0'

    // Room Database (for offline caching - optional)
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

### 3. AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <!-- Storage permissions -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <application
        android:name=".KMusicApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.KMusic"
        android:networkSecurityConfig="@xml/network_security_config"
        android:usesCleartextTraffic="true"
        tools:targetApi="31">

        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.KMusic.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Music Service -->
        <service
            android:name=".service.MusicService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

        <!-- Widget -->
        <receiver
            android:name=".widget.MusicWidget"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_info" />
        </receiver>

    </application>

</manifest>
```

### 4. Network Security Config

```xml
<!-- File: res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>

    <!-- Allow cleartext for local development -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">192.168.0.0/16</domain>
        <domain includeSubdomains="true">10.0.0.0/8</domain>
    </domain-config>
</network-security-config>
```

## 🎨 Material 3 Theme (Dark/Light)

### colors.xml

```xml
<!-- File: res/values/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Light Theme -->
    <color name="seed">#6750A4</color>
    <color name="md_theme_light_primary">#6750A4</color>
    <color name="md_theme_light_onPrimary">#FFFFFF</color>
    <color name="md_theme_light_primaryContainer">#EADDFF</color>
    <color name="md_theme_light_onPrimaryContainer">#21005D</color>

    <color name="md_theme_light_secondary">#625B71</color>
    <color name="md_theme_light_onSecondary">#FFFFFF</color>
    <color name="md_theme_light_secondaryContainer">#E8DEF8</color>
    <color name="md_theme_light_onSecondaryContainer">#1D192B</color>

    <color name="md_theme_light_background">#FFFBFE</color>
    <color name="md_theme_light_onBackground">#1C1B1F</color>
    <color name="md_theme_light_surface">#FFFBFE</color>
    <color name="md_theme_light_onSurface">#1C1B1F</color>
    <color name="md_theme_light_surfaceVariant">#E7E0EC</color>
    <color name="md_theme_light_onSurfaceVariant">#49454F</color>
</resources>
```

### colors.xml (Night)

```xml
<!-- File: res/values-night/colors.xml -->
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Dark Theme -->
    <color name="md_theme_dark_primary">#D0BCFF</color>
    <color name="md_theme_dark_onPrimary">#381E72</color>
    <color name="md_theme_dark_primaryContainer">#4F378B</color>
    <color name="md_theme_dark_onPrimaryContainer">#EADDFF</color>

    <color name="md_theme_dark_secondary">#CCC2DC</color>
    <color name="md_theme_dark_onSecondary">#332D41</color>
    <color name="md_theme_dark_secondaryContainer">#4A4458</color>
    <color name="md_theme_dark_onSecondaryContainer">#E8DEF8</color>

    <color name="md_theme_dark_background">#1C1B1F</color>
    <color name="md_theme_dark_onBackground">#E6E1E5</color>
    <color name="md_theme_dark_surface">#1C1B1F</color>
    <color name="md_theme_dark_onSurface">#E6E1E5</color>
    <color name="md_theme_dark_surfaceVariant">#49454F</color>
    <color name="md_theme_dark_onSurfaceVariant">#CAC4D0</color>
</resources>
```

### themes.xml

```xml
<!-- File: res/values/themes.xml -->
<resources>
    <style name="Base.Theme.KMusic" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">@color/md_theme_light_primary</item>
        <item name="colorOnPrimary">@color/md_theme_light_onPrimary</item>
        <item name="colorPrimaryContainer">@color/md_theme_light_primaryContainer</item>
        <item name="colorOnPrimaryContainer">@color/md_theme_light_onPrimaryContainer</item>

        <item name="colorSecondary">@color/md_theme_light_secondary</item>
        <item name="colorOnSecondary">@color/md_theme_light_onSecondary</item>
        <item name="colorSecondaryContainer">@color/md_theme_light_secondaryContainer</item>
        <item name="colorOnSecondaryContainer">@color/md_theme_light_onSecondaryContainer</item>

        <item name="android:colorBackground">@color/md_theme_light_background</item>
        <item name="colorOnBackground">@color/md_theme_light_onBackground</item>
        <item name="colorSurface">@color/md_theme_light_surface</item>
        <item name="colorOnSurface">@color/md_theme_light_onSurface</item>
        <item name="colorSurfaceVariant">@color/md_theme_light_surfaceVariant</item>
        <item name="colorOnSurfaceVariant">@color/md_theme_light_onSurfaceVariant</item>

        <!-- Shape -->
        <item name="shapeAppearanceSmallComponent">@style/ShapeAppearance.KMusic.SmallComponent</item>
        <item name="shapeAppearanceMediumComponent">@style/ShapeAppearance.KMusic.MediumComponent</item>
        <item name="shapeAppearanceLargeComponent">@style/ShapeAppearance.KMusic.LargeComponent</item>
    </style>

    <style name="Theme.KMusic" parent="Base.Theme.KMusic" />

    <style name="Theme.KMusic.NoActionBar">
        <item name="windowActionBar">false</item>
        <item name="windowNoTitle">true</item>
    </style>

    <!-- Rounded corners -->
    <style name="ShapeAppearance.KMusic.SmallComponent" parent="ShapeAppearance.Material3.SmallComponent">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSize">12dp</item>
    </style>

    <style name="ShapeAppearance.KMusic.MediumComponent" parent="ShapeAppearance.Material3.MediumComponent">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSize">16dp</item>
    </style>

    <style name="ShapeAppearance.KMusic.LargeComponent" parent="ShapeAppearance.Material3.LargeComponent">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSize">24dp</item>
    </style>
</resources>
```

## 🔧 Building & Exporting APK

### Debug Build (Quick Testing)

```bash
cd android-app

# Using Gradle Wrapper (recommended)
./gradlew assembleDebug

# Output location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Release Build (Production)

**Step 1: Generate Keystore**

```bash
keytool -genkey -v -keystore kmusic-release.keystore \
  -alias kmusic \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Follow prompts:
# Password: [your-secure-password]
# First/Last name: KMusic
# Organization: [Your Name]
# City, State, Country: [Your details]
```

**Step 2: Configure Signing**

Add to `app/build.gradle`:

```gradle
android {
    signingConfigs {
        release {
            storeFile file("../kmusic-release.keystore")
            storePassword "your-password"
            keyAlias "kmusic"
            keyPassword "your-password"
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
        }
    }
}
```

**Step 3: Build Release APK**

```bash
./gradlew assembleRelease

# Output:
# app/build/outputs/apk/release/app-release.apk
```

### Install APK

```bash
# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Or transfer to device and install manually
```

## 📦 What's Provided in This Package

Due to the extensive nature of a full Android app (50+ files), I've provided:

1. ✅ **Complete project structure** outline
2. ✅ **All Gradle configuration** files
3. ✅ **AndroidManifest.xml** with all permissions and components
4. ✅ **Material 3 theme** with dark/light mode
5. ✅ **Build and export instructions**
6. ✅ **Complete architectural pattern** (MVVM + Repository)

## 🎯 Next Steps to Complete the App

You have two options:

### Option A: Use This Guide to Build from Scratch

Follow this guide and the [ANDROID_INTEGRATION.md](../ANDROID_INTEGRATION.md) to implement each component step by step.

### Option B: Request Specific Components

I can provide complete code for specific screens:
1. Login/Registration screen
2. Home screen with tracks list
3. Player screen with controls
4. Settings screen with server config
5. Upload screen (admin only)
6. Music service (background playback)
7. Widget implementation

Just let me know which components you'd like me to create first!

## 📚 Additional Resources

- **Backend API**: [../API.md](../API.md)
- **Android Integration**: [../ANDROID_INTEGRATION.md](../ANDROID_INTEGRATION.md)
- **Role-Based Access**: [../ROLE_BASED_ACCESS.md](../ROLE_BASED_ACCESS.md)
- **Material 3 Guidelines**: https://m3.material.io/
- **ExoPlayer Documentation**: https://developer.android.com/media/media3

---

**Ready to build!** Choose your approach and let's create KMusic together! 🎵
