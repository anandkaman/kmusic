# KMusic - Android Music Streaming App

Beautiful, modern Android music streaming app with Material You design, supporting dark/light themes, lock screen controls, widgets, and dynamic playback.

## Features

- 🎨 **Material 3 Design** - Modern UI with dynamic colors
- 🌓 **Dark/Light Theme** - Automatic theme switching
- 🎵 **ExoPlayer Integration** - Professional audio playback
- 🔒 **Lock Screen Controls** - Full media controls on lock screen
- 📱 **Notification Controls** - Play/pause/next/previous from notification
- 📊 **Widgets** - Home screen widget for quick access
- ⚙️ **Server Configuration** - Configure your streaming server IP and port
- 🔐 **Role-Based Access** - Admin upload, user playback
- 📂 **Browse & Search** - Easy music discovery
- 🎧 **Playlists** - Create and manage playlists
- 📊 **Play History** - Track listening habits

## Screenshots

[Screenshots will be here after UI implementation]

## Requirements

- Android 8.0 (API 26) or higher
- Internet connection to streaming server
- Storage permission for offline caching (optional)
- Network permission for streaming

## Building the App

### Prerequisites

1. **Android Studio** - Arctic Fox or newer
2. **JDK 11** or higher
3. **Android SDK** - API 34

### Setup Steps

1. **Clone the repository**
   ```bash
   cd android-app
   ```

2. **Open in Android Studio**
   - File → Open → Select `android-app` folder
   - Wait for Gradle sync

3. **Configure the app**
   - No additional configuration needed
   - Server settings are configurable in-app

4. **Build the app**
   ```bash
   # Debug build
   ./gradlew assembleDebug

   # Release build
   ./gradlew assembleRelease
   ```

### Export APK

#### Method 1: Using Android Studio

1. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait for build to complete
3. Click "locate" in the notification
4. APK will be in: `app/build/outputs/apk/debug/app-debug.apk`

#### Method 2: Using Command Line

```bash
# Debug APK (for testing)
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK (for distribution)
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

#### Method 3: Signed Release APK

1. **Generate keystore** (first time only):
   ```bash
   keytool -genkey -v -keystore kmusic-release.keystore \
     -alias kmusic -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Create signing config** in `app/build.gradle`:
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
           }
       }
   }
   ```

3. **Build signed APK**:
   ```bash
   ./gradlew assembleRelease
   ```

4. **APK location**: `app/build/outputs/apk/release/app-release.apk`

## Installation

### Direct APK Installation

1. Transfer APK to your Android device
2. Enable "Install from Unknown Sources" in Settings
3. Open APK file
4. Tap "Install"

### ADB Installation

```bash
adb install app/build/outputs/apk/debug/app-debug.apk

# Or for release
adb install app/build/outputs/apk/release/app-release.apk
```

## First Time Setup

1. **Launch KMusic**
2. **Grant Permissions**
   - Storage access (for caching)
   - Network access (for streaming)
3. **Configure Server**
   - Settings → Server Configuration
   - Enter your server IP address (e.g., `192.168.1.100`)
   - Enter port (default: `8080`)
   - Save
4. **Login**
   - Use super admin: `kaman` / `Johnedoms2@` (for upload)
   - Or register a new user account

## Configuration

### Server Settings

In-app configuration (Settings → Server):
```
Server Address: 192.168.1.100
Port: 8080
Use HTTPS: No (enable if you have SSL)
```

### Port Forwarding (Home Server)

If accessing from outside your network:

1. **Router Configuration**
   - Forward port 8080 (or your custom port)
   - To your server's local IP

2. **In KMusic**
   - Server Address: Your public IP or domain
   - Port: Your forwarded port

### Example Configurations

**Local Network:**
```
Server: 192.168.1.100
Port: 8080
```

**Port Forwarded:**
```
Server: your-public-ip.com
Port: 8080
```

**HTTPS with Domain:**
```
Server: music.yourdomain.com
Port: 443
Use HTTPS: Yes
```

## Features Guide

### Home Screen
- Browse recent tracks
- Quick access to playlists
- Search functionality

### Player Screen
- Album art display
- Play/pause/skip controls
- Progress bar with seeking
- Volume control
- Repeat/shuffle modes
- Queue management

### Lock Screen
- Album art
- Track info (title, artist)
- Play/pause button
- Next/previous buttons
- Works even when screen is off

### Notification
- Persistent notification when playing
- Play/pause/next/previous controls
- Album art thumbnail
- Swipe to dismiss when stopped

### Widget
- Shows currently playing track
- Quick play/pause button
- Tap to open app
- Updates in real-time

### Settings
- Theme selection (Light/Dark/Auto)
- Server configuration
- Audio quality settings
- Cache management
- Account management
- **Upload Music** (admin only)

## Project Structure

```
android-app/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/kmusic/
│   │       │   ├── data/
│   │       │   │   ├── api/          # Retrofit API services
│   │       │   │   ├── model/        # Data models
│   │       │   │   ├── local/        # SharedPreferences, Database
│   │       │   │   └── repository/   # Repository pattern
│   │       │   ├── ui/
│   │       │   │   ├── home/         # Home screen
│   │       │   │   ├── player/       # Player screen
│   │       │   │   ├── settings/     # Settings screen
│   │       │   │   ├── auth/         # Login/Register
│   │       │   │   └── upload/       # Upload (admin only)
│   │       │   ├── service/
│   │       │   │   └── MusicService.kt  # Background playback
│   │       │   ├── widget/
│   │       │   │   └── MusicWidget.kt   # Home screen widget
│   │       │   └── MainActivity.kt
│   │       ├── res/
│   │       │   ├── layout/           # XML layouts
│   │       │   ├── values/           # Themes, colors, strings
│   │       │   ├── drawable/         # Icons, images
│   │       │   └── xml/              # Widget info, network config
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
├── build.gradle
└── settings.gradle
```

## Troubleshooting

### Cannot connect to server
- Check server is running: `curl http://your-ip:8080/health`
- Verify IP address in settings
- Check firewall allows port 8080
- Try ping: `ping your-server-ip`

### Playback stutters
- Check network speed
- Lower audio quality in settings
- Enable caching

### Upload not working
- Verify logged in as admin user
- Check file format (AAC, MP3, FLAC supported)
- Check file size (max 100MB by default)

### Widget not updating
- Long press widget → Widget settings
- Force stop and restart app
- Check battery optimization settings

## Development

### Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **UI**: Jetpack Compose + XML views
- **Networking**: Retrofit + OkHttp
- **Media**: ExoPlayer (Media3)
- **DI**: Manual dependency injection
- **Storage**: EncryptedSharedPreferences
- **Async**: Coroutines + Flow

### Dependencies

```gradle
// Core
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'

// Material Design 3
implementation 'com.google.android.material:material:1.11.0'

// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'

// Media
implementation 'androidx.media3:media3-exoplayer:1.2.1'
implementation 'androidx.media3:media3-session:1.2.1'
implementation 'androidx.media3:media3-ui:1.2.1'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

// ViewModel & LiveData
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'

// Navigation
implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'

// Security
implementation 'androidx.security:security-crypto:1.1.0-alpha06'

// Image Loading
implementation 'com.github.bumptech.glide:glide:4.16.0'
```

## Contributing

This is a personal/family project, but suggestions are welcome!

## License

MIT License

## Support

- Backend Documentation: [../README.md](../README.md)
- API Reference: [../API.md](../API.md)
- Android Integration: [../ANDROID_INTEGRATION.md](../ANDROID_INTEGRATION.md)

---

**Made with ❤️ for family music streaming**
