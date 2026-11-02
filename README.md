# Olauncher Lite

A personal fork of [Olauncher](https://github.com/tanujnotes/Olauncher) that keeps the launcher and drops everything else: no daily wallpapers, no screen time, no promotions and no internet access.

It installs as a separate app (`app.olauncher.lite`), so it can sit alongside the original.

> This fork was made with an AI assistant. See [Made with AI](#made-with-ai) for what that means.

## What's different from Olauncher

**Removed**
- Daily wallpaper downloads, and the offer to change your wallpaper when you switch theme
- The screen time counter
- Rate, review and share prompts, the Pro upsell, New Year messages, and the links and ads in settings
- Double tap to lock, along with the accessibility service and device admin it needed
- Web search from the app drawer
- The internet, wallpaper and usage-access permissions

**Changed**
- Settings, menus and dialogs use Android's own Material 3 look, with Material You colours on Android 12 and later. They follow your phone's dark mode and always show the status bar: the launcher's text colour, bolder app names and status bar settings change the home screen and app drawer only. "Theme" is called "Text colour" here, with choices that say what they do (dark text for light wallpapers, light text for dark ones, or match dark mode)
- Separate sliders for the size of app names and of the clock and date, both on top of Android's own font size; settings and menus keep Android's size. The number of home apps is a slider too
- Long-pressing an app in the drawer opens a normal menu: Rename, Hide, App info, Uninstall. A new name shows everywhere the app is, home screen and swipe settings included; this replaces the Rename button in the home app picker
- Settings choose what tapping the time and the date opens. Each swipe has one row: tap it to choose the default app or one of yours, or flip its switch to turn the swipe on or off
- Long presses vibrate, the date follows the launcher's language (or your phone's, if none is set) in its usual short, full or numeric format, and TalkBack can read the search box
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

## Made with AI

This fork is vibe-coded. Its changes were written by Claude, Anthropic's AI assistant, in conversation with the repository's owner, who decided what to change and tried the builds on their own phone. Each release is built from this source by GitHub, but no person has reviewed the code line by line.

In practice:
- Olauncher's own code is left as it was wherever this fork didn't need to change it. Most of the fork's changes are marked with `Lite:` comments, and the commits Claude wrote carry a `Co-Authored-By: Claude` line.
- Expect rough edges. It's a personal project, offered as is under the GPL, without any warranty.

## Credits and licence

Based on [Olauncher](https://github.com/tanujnotes/Olauncher) by [@tanujnotes](https://github.com/tanujnotes) and its contributors. This fork isn't affiliated with or endorsed by them. Like Olauncher, it's licensed under the [GNU GPLv3](LICENSE).
