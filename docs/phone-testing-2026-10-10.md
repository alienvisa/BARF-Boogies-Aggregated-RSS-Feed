# BARF phone testing — October 10, 2026

First physical-device smoke test: Pixel 9a, Android 17, 1080 × 2424 display. WebView provider: app.vanadium.webview 154.0.8037.126.0. Tested the published v0.1.2-preview APK, then installed a local candidate containing the headline rendering fix. Both use application ID app.localbrief and version code 3.

## Results

| Check | Result |
|---|---|
| Fresh install and launch | Passed; no prior BARF installation on this phone |
| Today, Topics, Saved, Manage sources, Settings | Opened successfully |
| Add real HTTPS RSS source | BBC feed accepted, headlines/previews/thumbnails loaded |
| Create topic and assign source | Phone test topic shows BBC items |
| Publisher article and redirect | BBC article rendered in WebView; domain/title updated |
| Save from article browser | Current redirected BBC URL appears in Saved |
| Share out | Android share sheet opens with the current URL; nothing sent |
| Receive shared link | ACTION_SEND example.org link appears in Saved |
| Restart persistence | Source and saved links remain after force-stop/relaunch |
| Install candidate over published build | Passed; local data preserved |
| Light/dark themes | Checked visually on the phone |
| Browser reload and system Back | Reload works; Back returns to Saved on initial page |
| Headline clipping | Found in published APK; fixed and visually verified in both themes |

The headline TextButton inherited a rounded clip with zero padding, which removed parts of the first letters on multiline titles. Giving this text button a rectangular shape preserves its full text while keeping the surrounding card design unchanged. Preview build and lint passed after the fix. No new automated tests were added for this visual-only change; the preceding published build passed 24 automated tests.

No app crash was observed during this pass. App-process error logs include platform/WebView initialization diagnostics; these did not prevent the tested flows from working.

## Candidate

The corrected APK is installed on the phone and also saved as ../BARF-0.1.2-phone-test.apk. It is an unpublished test candidate, distinct from the existing GitHub release assets. Size: 1,756,134 bytes. SHA-256: FCE9AE7176E0AAF088E2BC99B4A30CDF7746243816E62878D74460D4B1BF60A2.

The phone retains one BBC source, a Phone test topic, and the two saved test links. Appearance was returned to Light. No app data was cleared, no messages were sent, and phone connectivity settings were not changed.

## Next device checks

- Several user-selected publishers and a YouTube channel/video, including external-open fallback.
- Follow a page link and verify Back/Forward, current-page Save/Share, and feed scroll restoration.
- Offline/reconnection behavior, slow pages, and failed loads.
- Rotation, larger text sizes, and accessibility navigation.
- Export/import using Android's file picker, without replacing the user's real library.
- Hourly background refresh and battery behavior over a day of normal use.
- Older Android version and a second WebView provider.

The 14-day retention boundary remains covered by automated data tests; real elapsed-time retention has not been observed on this newly installed phone.
