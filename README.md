# Gita Wallpaper

A native Android app that displays random Bhagavad Gita verses as your wallpaper each time you unlock your phone.

## Features

- 🕉️ **Random Gita Verses**: Displays a new verse on each phone unlock
- 📜 **Bilingual Display**: Sanskrit (Devanagari) + Hindi or English translation
- ⚙️ **Configurable**: Choose your preferred translation language
- 🔋 **Battery Efficient**: 30-second throttling prevents excessive updates
- 🔒 **Privacy First**: No ads, no analytics, no data collection
- 📱 **Offline**: Works completely offline with bundled verses

## Screenshots

*Coming soon*

## Installation

### From GitHub Releases
1. Download the latest APK from [Releases](../../releases)
2. Enable "Install unknown apps" for your browser
3. Install the APK

### Build from Source
```bash
git clone https://github.com/your-username/gita-wallpaper.git
cd gita-wallpaper
./gradlew assembleDebug
```

APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Requirements

- Android 8.0 (API 26) or higher
- SET_WALLPAPER permission (auto-granted)

## How It Works

1. **UnlockReceiver** listens for `ACTION_USER_PRESENT` broadcasts
2. **WallpaperUpdateService** selects a random verse from 20 bundled shlokas
3. **WallpaperRenderer** creates a beautiful wallpaper with:
   - Dark gradient background
   - Golden Sanskrit text
   - Translation in your preferred language
4. **WallpaperManager** sets the generated image as your wallpaper

## Configuration

Open the app and tap **Settings** to:
- Choose **Hindi** or **English** as your translation language
- Sanskrit is always displayed as the primary text

## Dataset

Includes 20 popular verses from the Bhagavad Gita covering chapters 2-18.

## Tech Stack

- **Language**: Kotlin
- **UI**: XML Layouts
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Dependencies**: AndroidX Core, AppCompat, Material

## License

MIT License - Feel free to use and modify.

## Contributing

Contributions welcome! To add more verses, edit `app/src/main/assets/verses.json`.

---

श्रीमद्भगवद्गीता 🙏
