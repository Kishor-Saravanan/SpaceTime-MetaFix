# SpaceTime MetaFix - Quick Start Guide

## Upload to GitHub (3 Minutes)

### 1. Open Git Bash

Navigate to your project folder:
```bash
cd /path/to/SpaceTime-MetaFix
```

### 2. Run These Commands

```bash
git init
git add .
git commit -m "Initial build"
git remote add origin https://github.com/Kishor-Saravanan/SpaceTime-MetaFix.git
git branch -M main
git push -u origin main
```

### 3. Get Your APK

**Option A**: GitHub Actions (Automatic)
1. Go to your repo → **Actions** tab
2. Wait 3-5 minutes for build to complete
3. Download `app-debug.apk` from Artifacts

**Option B**: GitHub Releases
1. Go to **Releases** (right sidebar)
2. Download latest APK

## Using the App

### First Time Setup

1. Install APK on Android device
2. Grant storage permissions:
   - Settings → Apps → SpaceTime MetaFix → Permissions
   - Enable "Files and media" or "All files access"

### Basic Workflow

```
1. SELECT SOURCE FOLDERS
   ↓
   (Choose folders with photos/videos)
   
2. SELECT DESTINATION FOLDER
   ↓
   (Where to save database and NOGPS folder)
   
3. CHOOSE ACTION
   ↓
   - MOVE: Move files without GPS
   - COPY: Copy files without GPS
   - NOTHING: Just create database
   
4. BUILD DATABASE
   ↓
   Creates: database_YYYYMMDD_HHMM.csv
   Creates: NOGPS_YYYYMMDD_HHMM/ (if MOVE/COPY)
   
5. CONFIRM AUTOTAG
   ↓
   Dialog: "Start AutoTag process?"
   
6. START AUTOTAG
   ↓
   Matches timestamps within time window
   Writes GPS to files in NOGPS folder
```

### Output Example

```
/sdcard/MyPhotos/
├── database_20260306_1530.csv
└── NOGPS_20260306_1530/
    ├── IMG_001.jpg  ← Will get GPS from nearest match
    ├── IMG_002.jpg  ← Will get GPS from nearest match
    └── VID_010.mp4  ← Will get GPS from nearest match
```

## Common Issues

### "Permission Denied" in GitHub Actions
```bash
chmod +x gradlew
git add gradlew
git commit -m "Fix permissions"
git push
```

### "Storage Permission Required"
- Android 11+: Settings → Apps → Special Access → All files access
- Android 10-: Automatically granted

### "No files found"
- Make sure folders contain: jpg, jpeg, png, heic, mp4, mov, m4v
- Check folder permissions

## Features at a Glance

✅ **Extracts metadata** from photos (EXIF) and videos (QuickTime)  
✅ **Creates CSV database** with all metadata fields  
✅ **Separates files** without GPS into NOGPS folder  
✅ **AutoTag**: Matches timestamps to add GPS  
✅ **Time window**: ±5 to ±60 minutes (configurable)  
✅ **Safe**: Only modifies GPS, preserves everything else  

## Supported Files

| Type   | Extensions           |
|--------|---------------------|
| Photos | jpg, jpeg, png, heic|
| Videos | mp4, mov, m4v       |

## Metadata Extracted

- GPS coordinates (latitude, longitude, altitude)
- Timestamps (original, create, modify)
- Camera info (make, model)
- File info (name, size, type)
- Time offsets (timezone data)

## Quick Commands

### Update Repository
```bash
git add .
git commit -m "Update"
git push
```

### Create New Version
```bash
# Edit app/build.gradle: versionCode and versionName
git add app/build.gradle
git commit -m "Version 1.1"
git tag v1.1
git push --tags
```

### Download Latest Code
```bash
git pull origin main
```

## Getting Help

- **Issues**: https://github.com/Kishor-Saravanan/SpaceTime-MetaFix/issues
- **Docs**: See README.md and SETUP_GUIDE.md
- **Source**: All code is in `app/src/main/java/com/kishor/gpstools/`

---

**Ready to use?** Install the APK and start organizing your photos! 📸📍
