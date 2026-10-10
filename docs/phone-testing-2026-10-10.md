# BARF phone testing — October 10, 2026

First physical-device smoke test: Pixel 9a, Android 17, 1080 × 2424 display. WebView provider: app.vanadium.webview 154.0.8037.126.0. Tested the published v0.1.2-preview APK, then installed a local candidate containing the headline rendering fix, followed by a candidate that also fixes the Undo notice timeout. Both use application ID app.localbrief and version code 3.

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

The latest corrected APK, including both headline clipping and Undo timeout fixes, is installed on the phone and also saved as ../BARF-0.1.2-phone-test.apk. It is an unpublished test candidate, distinct from the existing GitHub release assets. Size: 1,756,134 bytes. SHA-256: E7FF6B3BD74118C9BA6C9EE8408C5A58D7CDC2B59436740050F636453406A3A3.

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

## Continued device pass — October 10, 2026

| Check | Result |
|---|---|
| Followed-page sharing | BBC Premier League link shares the followed URL, not the original article |
| Page Back/Forward and Android Back | Returned between the original article and followed page as expected |
| Save a followed page | Followed page appears in Saved; unsaving it works |
| Feed scroll restoration | Same article bounds before opening and after closing: [84,1490][922,1672] |
| YouTube handle discovery | https://www.youtube.com/@NASA resolves to its channel feed |
| YouTube video cards | VIDEO labels, titles, previews, and thumbnails render |
| Recorded video playback | NASA Crew-13 trailer showed advancing video frames inside WebView, muted |
| External YouTube handoff | Open externally launches the phone's installed YouTube app |
| Backup file picker | Export and import both work through Android's document picker |
| Backup round-trip | Restored 2 sources, 1 topic, and the original 2 bookmarks; temporary marker removed |
| Invalid backup | Unsupported format rejected before replacement; subsequent export retains both sources, topic, and all 4 bookmarks present at that stage |
| Offline error | With Wi-Fi and mobile data temporarily disabled, an uncached NASA page showed retry and external-open controls |
| Reconnection | Restored connections; Try again loaded the NASA page |
| Rotation | Viewer survives portrait/landscape/portrait; URL/domain and controls remain available |
| Text size | Today and Saved remain readable and navigable at 130% text size |
| Undo | Restores removed bookmark; ignored Undo notice expires rather than blocking later feedback |
| Scheduled refresh | WorkManager job registered; forced early dispatch is deferred by its own schedule check |

### Second issue and fix

Material's default snackbar duration becomes indefinite when it has an action label. This kept the Removed from Saved/Undo notice visible across screens and queued later confirmations/errors behind it. The app now explicitly uses SnackbarDuration.Long for Undo and Short for ordinary notices. On the phone, Undo restored the bookmark, and an ignored Undo notice disappeared within the 12-second verification window. Invalid-backup feedback subsequently appeared normally.

After this fix, the optimized preview build and lint passed (zero lint errors), all 24 automated tests passed, and the APK signature matched the existing development key. The current phone-test APK remains version 0.1.2-preview/code 3 and is unpublished; the existing GitHub release assets were not replaced.

### Cleanup and remaining scope

The final library was restored from the test snapshot: BBC and NASA sources, Phone test topic, and the original two saved links. Refresh was requested to replenish ordinary feed history, which backup restore intentionally excludes. The original exported backup is retained in Downloads as BARF-phone-test-backup.json. Invalid/verification files and temporary screen files were removed from the phone.

Wi-Fi and mobile data were restored enabled. Font scale returned to 1.0, rotation returned to portrait with auto-rotation disabled, and the keep-awake setting returned to its original value of 0. No passwords, account changes, or sent messages were involved.

Remaining: natural background refresh and battery behavior over a day, older Android/another WebView provider, TalkBack accessibility, full-screen video/audio, and additional publisher download/sign-in cases. Forced early execution does not establish natural hourly refresh or battery performance. Real 14-day elapsed-time retention remains unobserved. No new crash was observed in the interactive checks.
