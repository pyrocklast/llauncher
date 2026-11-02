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

- **Native look for menus, dialogs and settings**: the app uses Android's Material 3 theme, with Material You colours from your wallpaper on Android 12 and later. Settings, menus and dialogs follow the phone's dark mode and font, and settings always shows the status bar. The launcher's text colour (upstream's theme), bolder app names (upstream's bold font) and status bar settings apply to the home screen and app drawer only, and their names say so.
  - Settings is a standard Android settings screen titled "Olauncher Lite settings", with a large title bar that shrinks as you scroll and a back arrow. Setting the default launcher comes first until Lite is your launcher (afterwards "Change default launcher" moves under About), then sections for each part of the launcher: Home screen, Clock and date, App drawer, Appearance, Gestures and About, with switches, sliders and choice dialogs. Hidden apps (with a count, under App drawer), drawer alignment and following the phone's dark mode have their own rows instead of hidden taps and long presses, and App info (under About) shows the version.
  - The number of apps on the home screen is a slider right in the settings list, applied as you move it.
  - App name size is a slider with a preview of your home apps. It applies only to app names (home screen, drawer and its search box), not to settings or menus.
  - Clock and date size is a separate slider, previewed with the live time and date.
  - Both sizes are relative to Android's own font size, so raising that for the whole phone enlarges the launcher too.
  - Date style: short ("lun. 2 nov."), full ("lundi 2 novembre") or numeric ("lun. 02/11"), each in the order your language uses.
  - Clock and date has rows for what tapping the time and the date opens: Android's default app, or one you choose. Long-pressing the clock or date on the home screen still chooses an app, but no longer forgets the current one if you back out.
  - Gestures has one row per swipe, like Android's Wi-Fi row: tap the text to choose the default app (camera or phone) or one of yours, flip the switch to turn the swipe on or off.
  - Text colour, bolder app names and sizes apply without reloading anything; the home screen and drawer use them when you go back.
  - Long-pressing an app in the drawer opens a normal pop-up menu (Rename, Hide, App info, Uninstall). Rename opens a dialog, with a button to restore the original name. The new name also shows on the home screen and in the swipe settings, so the app picker's separate Rename button is gone. Hiding an app the first time shows a short note on where to find it, instead of a dialog and a trip to settings.
  - Messages use standard Material dialogs.
- The home screen and the app drawer keep Olauncher's own minimal text style.
- **Small touches**: long presses give the usual short vibration; the date uses the language chosen for the launcher in Android's settings, or else the phone's language, in that language's usual format; swipe distances no longer depend on how sharp the screen is; the keyboard opens with Android's current method; TalkBack reads the drawer's search box as "Search apps".
- **Faster returns home**:
  - The app list is cached. Upstream reloaded and re-sorted every app each time you came back home, then again when you opened the drawer. Lite reloads when apps are installed, updated or removed, when a work profile or Private Space is switched on or off, when you hide or rename an app, and at most once an hour otherwise.
  - Checking that home apps are still installed happens in the background, and only after apps change, instead of on the main thread every time you return home.
  - Long press uses Android's own timing, which follows the "Touch & hold delay" accessibility setting. Upstream added half a second on top.
  - Release builds drop resources nothing uses any more.
  - AppCompat's emoji loader no longer starts with the launcher. Android already has current emoji.
  - No full reload every 4 hours. Upstream reloaded the launcher when you came home after 4 hours, which could show as a blink; Lite's app list and theme don't need it.
  - The deprecated lifecycle-extensions library is gone; nothing used it.
- **Fixes**: Persian, offered in Android's per-app language list, is now included in the build (upstream left it out, so it showed English). Long-pressing the clock or date lists hidden apps too, like the settings rows.

- **GitHub builds**: two workflows, **Release** (builds a signed APK and publishes it for Obtainium) and **Sync with Olauncher** (merges upstream's changes). See "Building on GitHub" below.

Everything that makes it a launcher still works: home apps, swipe gestures and swipe apps, clock and date taps, the app drawer and its search, hidden apps, renaming, pinned shortcuts and Private Space.

New text this fork adds lives in `strings_lite.xml`, in English and French. Other languages show those few lines in English.

## Building on GitHub

GitHub builds and signs the APK on its servers, and Obtainium on your phone installs each new build as an update. Your PC only needs Java once, to create the signing key.

### 1. The repository

The code lives at <https://github.com/pyrocklast/llauncher>. It's public, which is the simplest setup for Obtainium. You only need a copy on your PC to build or change things there:

```sh
git clone https://github.com/pyrocklast/llauncher ~/llauncher
```

### 2. Add your signing key (one time)

If you already made a key for building on your PC, use that one: an APK signed with a different key won't install over one that's already on your phone. Otherwise, create one (any Java works, Fedora's included):

```sh
sudo dnf install java-25-openjdk-headless
mkdir -p ~/keys
keytool -genkeypair -v -keystore ~/keys/olauncher-lite.jks \
  -alias lite -keyalg RSA -keysize 4096 -validity 36500
```

Turn the key file into text that GitHub can store:

```sh
base64 -w0 ~/keys/olauncher-lite.jks > ~/olauncher-lite-key.txt
```

In the repository, open **Settings > Secrets and variables > Actions** and add two repository secrets:

- `KEYSTORE_BASE64`: the whole contents of `olauncher-lite-key.txt` (open it in Text Editor, select all, copy)
- `KEYSTORE_PASSWORD`: the key's password

Only if you created the key differently from the command above, also add `KEY_ALIAS` (when it isn't `lite`) and `KEY_PASSWORD` (when the key has a password of its own).

Then delete the text file with `rm ~/olauncher-lite-key.txt`. **Back up the `.jks` file and its password somewhere safe.** GitHub keeps them encrypted and never shows them in logs, but you can't download them back.

### 3. Build a release

In the repository, open **Actions > Release > Run workflow**. This works from the GitHub website on your phone too. About five minutes later the APK is on the repository's **Releases** page, named like `OlauncherLite-v6.10.27-lite.1.apk`. The number at the end goes up with every build.

### 4. Install with Obtainium (one time)

Install Obtainium from F-Droid, then **Add app**, paste `https://github.com/pyrocklast/llauncher`, and install. From then on Obtainium checks for new releases and offers them as updates.

### Getting Olauncher's updates

Open **Actions > Sync with Olauncher > Run workflow**. It merges Olauncher's new changes into your repository and lists them in the run's summary. Then run **Release** to build them. Files Lite deleted on purpose stay deleted, and your README is kept. If Olauncher's changes clash with Lite's in any other way, it stops without changing anything, and the merge has to be done in git (see "Updating from upstream" below).

## Building on your PC (Fedora)

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
cd ~/llauncher
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

On GitHub, the **Sync with Olauncher** workflow does this for you. To do it by hand on your PC (for example when that workflow reports a clash), note that the original project is set up as the `upstream` remote:

```sh
git fetch upstream
git merge upstream/master
./gradlew assembleRelease   # or push, then run Release on GitHub
```

On a PC, run `git config merge.ours.driver true` once first, so the merge keeps this fork's README (see `.gitattributes`). Conflicts can only appear in files this fork changed. The fork's build settings are all in one block at the end of `app/build.gradle`, so upstream's version bumps don't clash with them. `git diff upstream/master --stat` lists them. Settings is a full rewrite, so if upstream adds a new setting you'd add it to `SettingsFragment.kt` by hand rather than merging it. The version code follows upstream, so the new APK installs over the old one.

## Changing the name or package

- App name: `app_name` in `app/src/main/res/values/strings.xml`
- Package: `applicationId` in `app/build.gradle`. Changing it makes a separate app with its own settings.
