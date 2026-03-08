# SpaceTime MetaFix - Complete Setup Guide

## Quick Start: Upload to GitHub

### Step 1: Create GitHub Repository

1. Go to https://github.com/new
2. Repository name: `SpaceTime-MetaFix`
3. Description: "GPS Metadata Management Tool for Android"
4. Choose: **Public** or **Private**
5. **DO NOT** initialize with README (we already have one)
6. Click "Create repository"

### Step 2: Upload Code Using Git Bash

Open Git Bash in the project directory and run:

```bash
# Navigate to project directory
cd /path/to/SpaceTime-MetaFix

# Initialize git repository
git init

# Add all files
git add .

# Create initial commit
git commit -m "Initial commit: SpaceTime MetaFix v1.0"

# Add remote repository
git remote add origin https://github.com/Kishor-Saravanan/SpaceTime-MetaFix.git

# Push to GitHub
git branch -M main
git push -u origin main
```

### Step 3: Verify GitHub Actions

1. Go to your repository on GitHub
2. Click **Actions** tab
3. You should see "Build APK" workflow running
4. Wait for it to complete (usually 3-5 minutes)

### Step 4: Download APK

**Option A: From Actions**
1. Go to **Actions** tab
2. Click on the latest workflow run
3. Scroll down to **Artifacts**
4. Download `app-debug.apk`

**Option B: From Releases**
1. Go to **Releases** (right sidebar)
2. Download the latest APK

## Project Structure

```
SpaceTime-MetaFix/
├── .github/
│   └── workflows/
│       └── build-apk.yml          # GitHub Actions workflow
├── app/
│   ├── build.gradle                # App build configuration
│   ├── proguard-rules.pro          # ProGuard rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/kishor/gpstools/
│           │   ├── MainActivity.kt
│           │   ├── MetadataViewModel.kt
│           │   ├── FileMetadata.kt
│           │   ├── MetadataExtractor.kt
│           │   ├── VideoMetadataExtractor.kt
│           │   ├── FileScanner.kt
│           │   ├── CSVDatabaseWriter.kt
│           │   ├── CSVDatabaseReader.kt
│           │   ├── FileHandler.kt
│           │   ├── AutoTagMatcher.kt
│           │   ├── MetadataWriter.kt
│           │   └── FileUtils.kt
│           └── res/
│               ├── layout/
│               │   └── activity_main.xml
│               └── values/
│                   ├── colors.xml
│                   ├── strings.xml
│                   └── themes.xml
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle                     # Project build configuration
├── settings.gradle                  # Project settings
├── gradle.properties                # Gradle properties
├── gradlew                          # Gradle wrapper (Unix)
├── gradlew.bat                      # Gradle wrapper (Windows)
├── .gitignore
└── README.md
```

## Building Locally (Optional)

If you want to build the APK on your local machine:

### Prerequisites

1. **Install Android Studio**
   - Download from: https://developer.android.com/studio
   - Install Android SDK API 34

2. **Install JDK 17**
   - Download from: https://adoptium.net/

### Build Steps

```bash
# Clone repository
git clone https://github.com/Kishor-Saravanan/SpaceTime-MetaFix.git
cd SpaceTime-MetaFix

# Build debug APK
./gradlew assembleDebug

# APK location
app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK (Signed)

```bash
# Generate keystore (first time only)
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias spacetime

# Build release APK
./gradlew assembleRelease

# APK location
app/build/outputs/apk/release/app-release.apk
```

## Installing on Android Device

### Method 1: Direct Install

1. Download `app-debug.apk` to your phone
2. Open the APK file
3. Allow "Install from Unknown Sources" if prompted
4. Click Install

### Method 2: ADB Install

```bash
# Connect phone via USB
# Enable USB Debugging on phone

# Install APK
adb install app-debug.apk
```

## Troubleshooting

### GitHub Actions Build Fails

**Issue**: Workflow fails with "Permission denied"

**Solution**:
```bash
git update-index --chmod=+x gradlew
git commit -m "Make gradlew executable"
git push
```

### Build Error: "SDK not found"

**Solution**: 
1. Install Android Studio
2. Open Android Studio
3. Go to SDK Manager
4. Install Android SDK API 34

### APK Won't Install

**Solution**:
1. Settings → Security → Allow Unknown Sources
2. Or Settings → Apps → Special Access → Install Unknown Apps

### Permission Issues on Android 11+

**Solution**:
1. Open app
2. Grant storage permissions when prompted
3. Settings → Apps → SpaceTime MetaFix → Permissions
4. Enable "All files access"

## Making Changes

### Modify App Name

Edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">Your New Name</string>
```

### Change App Icon

Replace files in:
- `app/src/main/res/mipmap-hdpi/ic_launcher.png`
- `app/src/main/res/mipmap-mdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`

### Change Package Name

1. Update in `app/build.gradle`:
```gradle
android {
    namespace 'com.yourname.appname'
    defaultConfig {
        applicationId "com.yourname.appname"
    }
}
```

2. Rename package directories and update imports

### Add New Features

1. Create new branch: `git checkout -b feature/your-feature`
2. Make changes
3. Commit: `git commit -m "Add feature"`
4. Push: `git push origin feature/your-feature`
5. Create Pull Request on GitHub

## Continuous Integration

The GitHub Actions workflow automatically:

1. **Triggers on**:
   - Push to `main` branch
   - Pull requests to `main`

2. **Actions**:
   - Sets up JDK 17
   - Builds debug APK
   - Uploads artifact
   - Creates release (on main branch)

3. **Outputs**:
   - Downloadable APK in Actions artifacts
   - Tagged release with APK attached

## Version Management

To create a new version:

```bash
# Update version in app/build.gradle
versionCode = 2
versionName = "1.1"

# Commit and tag
git add app/build.gradle
git commit -m "Version 1.1"
git tag -a v1.1 -m "Version 1.1"
git push origin main --tags
```

## Support

For issues:
- GitHub Issues: https://github.com/Kishor-Saravanan/SpaceTime-MetaFix/issues
- Email: (your email)

## Next Steps

1. ✅ Upload code to GitHub
2. ✅ Verify Actions build succeeds
3. ✅ Download and test APK
4. 📱 Install on Android device
5. 🧪 Test with sample media files
6. 🚀 Share with users

---

**Important**: The initial build may take 5-10 minutes. Subsequent builds are faster due to caching.
