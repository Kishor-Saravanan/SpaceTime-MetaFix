# SpaceTime MetaFix

GPS Metadata Management Tool for Android

## Overview

SpaceTime MetaFix is an Android application that manages GPS metadata in photos and videos. It extracts metadata, creates searchable databases, and automatically tags files without GPS data by matching timestamps.

## Features

- **Multi-folder scanning**: Select multiple source directories containing media files
- **Comprehensive metadata extraction**: Extracts all EXIF and QuickTime metadata
- **CSV database generation**: Creates timestamped database files for easy analysis
- **Smart file handling**: Move, copy, or leave files without GPS metadata
- **AutoTag**: Automatically tags files by matching timestamps with nearby GPS-tagged files
- **Configurable time window**: Adjust matching tolerance (±5 to ±60 minutes)

## Supported File Types

### Photos
- JPG/JPEG
- PNG
- HEIC

### Videos
- MP4
- MOV
- M4V

## How It Works

### 1. Database Build

1. **Select Source Folders**: Choose one or more directories containing media files
2. **Select Destination Folder**: Where database and processed files will be saved
3. **Choose File Action**: 
   - **MOVE**: Move files without GPS to `NOGPS_YYYYMMDD_HHMM` folder
   - **COPY**: Copy files without GPS to `NOGPS_YYYYMMDD_HHMM` folder
   - **NOTHING**: Create database only, don't move files

4. **Build Database**: Extracts metadata and creates `database_YYYYMMDD_HHMM.csv`

### 2. AutoTag Process

1. **Set Time Window**: Configure matching tolerance (default: ±15 minutes)
2. **Start AutoTag**: Finds files in NOGPS folder and matches them with GPS-tagged files
3. **Write GPS Data**: Only updates GPS coordinates, preserving all other metadata

## Metadata Fields

The following metadata is extracted and logged:

- FileName
- FileSize
- FileType
- Make
- Model
- GPSLatitude
- GPSLongitude
- GPSAltitude
- DateTimeOriginal
- CreateDate
- ModifyDate
- MediaCreateDate
- MediaModifyDate
- TrackCreateDate
- TrackModifyDate
- OffsetTime
- OffsetTimeOriginal
- OffsetTimeDigitized

## Building from Source

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 17
- Android SDK API 34
- Git

### Clone and Build

```bash
git clone https://github.com/Kishor-Saravanan/SpaceTime-MetaFix.git
cd SpaceTime-MetaFix
./gradlew assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions Build

This project includes automated APK building via GitHub Actions.

### Automatic Build

Every push to `main` branch triggers:
1. Build the project
2. Generate debug APK
3. Upload as artifact
4. Create release with APK

### Download APK

1. Go to **Actions** tab
2. Select latest workflow run
3. Download `app-debug` artifact

OR

1. Go to **Releases**
2. Download latest APK

## Permissions

The app requires the following permissions:

- `READ_EXTERNAL_STORAGE` (Android ≤12)
- `WRITE_EXTERNAL_STORAGE` (Android ≤10)
- `MANAGE_EXTERNAL_STORAGE` (Android 11+)
- `READ_MEDIA_IMAGES` (Android 13+)
- `READ_MEDIA_VIDEO` (Android 13+)

## Technical Details

### Architecture

- **Language**: Kotlin
- **Minimum SDK**: API 26 (Android 8.0)
- **Target SDK**: API 34 (Android 14)
- **Libraries**:
  - AndroidX ExifInterface for photo metadata
  - MP4Parser for video metadata
  - Apache Commons CSV for database operations
  - Kotlin Coroutines for async operations

### File Structure

```
destination/
├── database_20260306_1530.csv
└── NOGPS_20260306_1530/
    ├── IMG_001.jpg
    ├── VID_010.mp4
    └── ...
```

## Video GPS Handling

Video files store GPS data in QuickTime metadata atoms:
- `QuickTime:GPSCoordinates`
- `QuickTime:LocationInformation`
- `com.apple.quicktime.location.ISO6709`

The app extracts and writes GPS data while preserving video integrity.

## Safety Features

- **Metadata preservation**: Only GPS fields are modified during AutoTag
- **Orientation safe**: Does not modify image orientation or rotation
- **Non-destructive**: Original timestamps and camera data preserved
- **Backup recommended**: Always backup files before processing

## Limitations

- Requires Android 8.0 or higher
- Storage permissions must be granted manually on Android 11+
- Large file sets may take time to process
- Video GPS writing may not work for all formats

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## License

This project is open source. See LICENSE file for details.

## Author

Kishor Saravanan

## Support

For issues and questions:
- GitHub Issues: https://github.com/Kishor-Saravanan/SpaceTime-MetaFix/issues

---

**Note**: This app modifies file metadata. Always backup your files before processing.
