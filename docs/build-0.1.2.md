# BARF · Build 0.1.2

October 10, 2026 · In-app article viewer

## Behavior

Headlines open the publisher's original page inside the app. The viewer shows its current domain and title, loading progress, Close, Save/Saved, Share, Open externally, page Back/Forward, and Stop/Reload. Android Back traverses page history before returning to the underlying screen. Closing preserves the feed/topic/saved list position.

Following links updates the URL used for saving and sharing, including page history changes. Saving a new page creates a local bookmark; existing items retain their original source preview. App theme colors apply to the surrounding controls, while the publisher controls the page's appearance.

The viewer uses the device's WebView without adding a browser SDK. See the [Android WebView guide](https://developer.android.com/develop/ui/views/layout/webapps/webview). It is created only while reading and destroyed when closed. JavaScript and first-party cookies support ordinary websites; file/content access, insecure mixed content, third-party cookies, automatic playback, and automatic popup windows are disabled. There is no JavaScript bridge into app data. Certificate errors are cancelled.

Network/site errors provide retry and an external-open fallback. A stopped rendering process is handled without retaining its destroyed viewer. Downloads are handed to an external browser.

## Verification

| Check | Result |
|---|---|
| Debug and optimized preview builds | Passed |
| Automated tests | 24 passed, 0 failures |
| Lint, both variants | 0 errors, 11 advisory warnings |
| APK signatures, both variants | Verified |
| Packaged version | Code 3; preview 0.1.2-preview |
| Preview APK | 1,756,138 bytes (1.67 MiB) |
| Debug APK | 19,171,494 bytes |

Browser coverage checks HTTPS navigation boundaries, blocked local/executable schemes, opening and closing a saved article inside the app, configured WebView access restrictions, and saving a followed page without replacing the original bookmark. Existing feed, retention, sharing, and backup tests also pass. Activity ViewModels now receive an explicit factory bound to the current application.

The 11 lint warnings comprise nine dependency update advisories, one redundant resource-folder qualifier, and the reviewed JavaScript-enabled warning required for website compatibility.

Application ID app.localbrief and the development signing certificate are unchanged. Install over the prior APK to keep local data.

- Certificate SHA-256: 74034762E2AE90BFCA3BBBDC68C98B4B141624D452EE365EB7430FB7CB41D03E
- Preview SHA-256: 20B76605B918CFA02F81529FCF7BC74252CC15B7ECE63A3266EFE77120D12EDA
- Debug SHA-256: 8B2EB47A8541E94561899D6DF3046ED5604259BA03AC337DA0E4E9C9273701FE

## Remaining device checks

The viewer has not been launched on a physical phone or emulator. Robolectric tests check the app's behavior and callbacks; they do not render real publisher websites or validate actual video playback.

On a phone, check several chosen publishers, redirects and followed links, Back/Forward, Save/Share, both themes, offline/error recovery, rotation, and closing to the prior feed position. Test inline video and external downloads where needed.

Paywalls, ads, and sign-in requirements remain part of the original website. Browser-specific features, popup sign-in, PDF viewing, and full-screen playback may require Open externally. Rotation/process restoration reopens the current URL; browser history and scroll position may reset. No guaranteed offline full-article download is included. Website cookies/cache stay local, are excluded from exports/cloud backup, and are separate from the 14-day feed retention rule.
