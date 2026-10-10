# Obtainium verification — BARF v0.1.3

October 10, 2026. Verified live on the same Pixel 9a / Android 17 used for phone testing.

- Public unauthenticated GitHub latest-release endpoint returned v0.1.3, draft=false, prerelease=false, and exactly one APK: BARF-0.1.3.apk.
- Uploaded asset digests were compared with local SHA-256 values before publication. APK SHA-256: 7F603FBE1265164FD2974A8998117BF4519B7D642BF9DB7957AE7C6005E5B5C9.
- Added the public repository URL to the phone's existing Obtainium app. It identified BARF, app.localbrief, the current 0.1.2-preview install, latest v0.1.3, and the single APK without an APK selection filter.
- Initiated the download/update in Obtainium. Android reported installed versionName=0.1.3, versionCode=4, and installerPackageName=dev.imranr.obtainium.fdroid.
- Reopening the tracked app in Obtainium showed v0.1.3 Installed / Latest with the retained certificate hash.
- BARF retained both test sources (BBC and NASA), the Phone test topic, and saved links after updating.
- Temporary keep-awake was restored to its original value of 0. No GitHub credentials were transferred to Obtainium; the repository is public. Existing unrelated Obtainium entries/settings were not edited.

Release: https://github.com/alienvisa/BARF-Boogies-Aggregated-RSS-Feed/releases/tag/v0.1.3

This verifies discovery, selection, version reporting, and an interactive installation/update. It does not establish silent/background installs, which depend on Android and Obtainium settings. Overnight BARF feed refresh/battery and broader device coverage remain pending as documented in phone-testing-2026-10-10.md.

The release tag/source archive remain the original v0.1.3 source snapshot; this verification record was added after publication. Release APK assets were not overwritten.
