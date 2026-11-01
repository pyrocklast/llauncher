# Olauncher Lite

A personal fork of [Olauncher](https://github.com/tanujnotes/Olauncher) with everything that isn't the launcher taken out. It installs as **Olauncher Lite** (`app.olauncher.lite`), so it sits alongside the original rather than replacing it.

## What's removed

- **Daily wallpaper**: the background downloader, its settings row, and the "did you know" prompt. The WorkManager library is gone too.
- **Plain wallpaper on theme change**: switching theme no longer offers to change your wallpaper. Set your wallpaper in Android's own settings.
- **Screen time**: the usage counter on the home screen, its settings row and the usage-access prompt.
- **Promotions and nags**: rate, review and share prompts, the Olauncher Pro dialog, the "More features" and "About" rows, the footer links (privacy, share, rate, GitHub, X) and the footer ads for the developer's other apps, the New Year messages, the welcome dialog and the "keep the keyboard on" nudge.
- **Double tap to lock**: the accessibility service and device-admin receiver it relied on are gone too. Lock with the power button.
- **Web search from the drawer**: typing `!something` no longer opens DuckDuckGo, and a search with no matching app no longer opens a web search. Enter launches the first match.
- **Permissions**: `INTERNET`, `SET_WALLPAPER` and `PACKAGE_USAGE_STATS`. The launcher can't go online, change your wallpaper or read app usage.

## What's changed

- **Native look for menus, dialogs and settings**: the app uses Android's Material 3 theme, with Material You colours from your wallpaper on Android 12 and later.
  - Settings is a standard Android settings screen, with sections, switches and choice dialogs. Hidden apps and app drawer alignment have their own rows, and the theme choice includes System, instead of hidden taps and long presses.
  - Long-pressing an app in the drawer opens a normal pop-up menu (Rename, Hide, App info, Uninstall). Rename opens a dialog, with a button to restore the original name.
  - Messages use standard Material dialogs.
- The home screen and the app drawer keep Olauncher's own minimal text style.
- **Faster returns home**:
  - The app list is cached. Upstream reloaded and re-sorted every app each time you came back home, then again when you opened the drawer. Lite reloads when apps are installed, updated or removed, when a work profile or Private Space is switched on or off, when you hide or rename an app, and at most once an hour otherwise.
  - Checking that home apps are still installed happens in the background, and only after apps change, instead of on the main thread every time you return home.
  - Long press uses Android's own timing, which follows the "Touch & hold delay" accessibility setting. Upstream added half a second on top.
  - Release builds drop resources nothing uses any more.

Everything that makes it a launcher still works: home apps, swipe gestures and swipe apps, clock and date taps, the app drawer and its search, hidden apps, renaming, pinned shortcuts and Private Space.

New text this fork adds lives in `strings_lite.xml`, in English and French. Other languages show those few lines in English.

## Building on Fedora

### 1. Java 21 (one time)

Fedora 44's default is Java 25, which the Gradle version this project uses can't run on. Put a Java 21 alongside it for building only; your system Java isn't touched.

```sh
mkdir -p ~/opt && cd ~/opt
curl -L -o jdk21.tar.gz "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar xf jdk21.tar.gz && rm jdk21.tar.gz
mv jdk-21* jdk-21
```

### 2. Android SDK (one time)

Download **Command line tools only** for Linux from <https://developer.android.com/studio#command-line-tools-only>, then:

```sh
mkdir -p ~/Android/Sdk/cmdline-tools
unzip ~/Downloads/commandlinetools-linux-*_latest.zip -d ~/Android/Sdk/cmdline-tools
mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest

export JAVA_HOME=~/opt/jdk-21 ANDROID_HOME=~/Android/Sdk
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --licenses
~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-36" "build-tools;35.0.0"
```

The `latest` folder name matters: sdkmanager refuses to run from anywhere else.

### 3. Signing key (one time)

```sh
mkdir -p ~/keys
~/opt/jdk-21/bin/keytool -genkeypair -v -keystore ~/keys/olauncher-lite.jks \
  -alias lite -keyalg RSA -keysize 4096 -validity 36500
```

Then create `keystore.properties` in the project root. It's in `.gitignore`, so it never gets committed. Use the same password for both entries:

```properties
storeFile=/home/YOUR_USER/keys/olauncher-lite.jks
storePassword=your-password
keyAlias=lite
keyPassword=your-password
```

**Back up the `.jks` file and its password.** Every update must be signed with this key. If you lose it, you'll have to uninstall and reinstall, and you'll lose your home screen setup.

### 4. Build

```sh
cd olauncher-lite
export JAVA_HOME=~/opt/jdk-21 ANDROID_HOME=~/Android/Sdk
./gradlew assembleRelease
```

The APK is `app/build/outputs/apk/release/app-release.apk`. The first build downloads Gradle and the libraries, so it takes a few minutes.

For a quick test without a key, run `./gradlew assembleDebug` instead. That gives `app/build/outputs/apk/debug/app-debug.apk`, which installs as a separate debug copy.

### 5. Install

Copy the APK to the phone and open it from the Files app, or with USB debugging on:

```sh
~/Android/Sdk/platform-tools/adb install app/build/outputs/apk/release/app-release.apk
```

Then make it your Home app, either from its own settings ("Set as default launcher") or in Android's Settings > Apps > Default apps.

## Updating from upstream

The original project is set up as the `upstream` remote.

```sh
git fetch upstream
git merge upstream/master
./gradlew assembleRelease
```

Conflicts can only appear in files this fork changed. `git diff upstream/master --stat` lists them. Settings is a full rewrite, so if upstream adds a new setting you'd add it to `SettingsFragment.kt` by hand rather than merging it. The version code follows upstream, so the new APK installs over the old one.

## Changing the name or package

- App name: `app_name` in `app/src/main/res/values/strings.xml`
- Package: `applicationId` in `app/build.gradle`. Changing it makes a separate app with its own settings.
