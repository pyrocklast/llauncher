# Olauncher Lite

A personal fork of [Olauncher](https://github.com/tanujnotes/Olauncher) that keeps the launcher and drops everything else: no daily wallpapers, no screen time, no promotions and no internet access.

It installs as a separate app (`app.olauncher.lite`), so it can sit alongside the original.

## What's different from Olauncher

**Removed**
- Daily wallpaper downloads, and the offer to change your wallpaper when you switch theme
- The screen time counter
- Rate, review and share prompts, the Pro upsell, New Year messages, and the links and ads in settings
- Double tap to lock, along with the accessibility service and device admin it needed
- Web search from the app drawer
- The internet, wallpaper and usage-access permissions

**Changed**
- Settings, menus and dialogs use Android's own Material 3 look, with Material You colours on Android 12 and later
- Text size and the number of home apps are set with sliders, and text size follows Android's own font size
- Long-pressing an app in the drawer opens a normal menu: Rename, Hide, App info, Uninstall
- Long presses vibrate, the date follows the launcher's language (or your phone's, if none is set) in its usual format, and TalkBack can read the search box
- Returning home does less work: the app list is cached and refreshed only when apps change, home apps are checked in the background, and long press uses Android's own timing

The home screen and app drawer keep Olauncher's minimal, text-only look.

## Install

Add `https://github.com/pyrocklast/llauncher` to [Obtainium](https://github.com/ImranR98/Obtainium) (available on F-Droid), and it will keep the app up to date. Or download the APK from [Releases](https://github.com/pyrocklast/llauncher/releases).

Requires Android 7.0 or later.

## Permissions

| Permission | Used for |
|---|---|
| Query all packages | Listing your apps |
| Expand status bar | Swiping down to open notifications |
| Request delete packages | Uninstalling apps from the app drawer |
| Set alarm | Opening your clock app when you tap the time |
| Access hidden profiles | Showing Private Space apps on Android 15 and later |

## Building

See [BUILD.md](BUILD.md) for building on GitHub or on a PC, and for pulling in Olauncher's updates.

## Credits and licence

Based on [Olauncher](https://github.com/tanujnotes/Olauncher) by [@tanujnotes](https://github.com/tanujnotes) and its contributors. This fork isn't affiliated with or endorsed by them. Like Olauncher, it's licensed under the [GNU GPLv3](LICENSE).
