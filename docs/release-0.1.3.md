# BARF v0.1.3

Phone-tested fixes and straightforward updates through Obtainium. Android 8.0+.

## Changes

- Fix clipped first letters on multiline article/video headlines.
- Fix the Undo notice staying visible indefinitely and blocking later confirmations/errors.
- Publish one optimized universal APK with Android version 0.1.3 and version code 4.
- Keep the existing package ID and signing certificate so updates preserve local data.
- Make the repository public and add an Obtainium setup link and release checklist.

## Install or update

Download **BARF-0.1.3.apk** (1.67 MiB), or add this repository URL in Obtainium:

https://github.com/alienvisa/BARF-Boogies-Aggregated-RSS-Feed

No GitHub token, APK filter, or prerelease option is needed for this release. Standard version detection uses the matching Android and release version. Automatic/silent installs still depend on Android and Obtainium settings. Install over the existing app; do not uninstall first.

## Verification

24 automated tests passed, preview build/lint passed with zero lint errors, and APK signature/package/version verified. The fixes were checked on a Pixel 9a running Android 17 with Vanadium WebView. Device testing covered feeds/topics, saving/sharing, browser navigation, YouTube recorded playback (muted), backup/restore and malformed imports, offline/reconnection, rotation, and 130% text sizing.

Natural overnight refresh/battery behavior, older Android devices, TalkBack, and full-screen video/audio remain pending. The scheduled WorkManager job was observed; forced early dispatch was deferred by its own schedule. This is an early v1 release, not a claim of exhaustive device coverage.

Source archive, build report, checksums, and Obtainium configuration are attached. The only APK asset is the app intended for installation.
