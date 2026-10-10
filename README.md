# BARF - Boogies Aggregated RSS Feed

Repository/project title: BARF - Boogies Aggregated RSS Feed. The installed app is labeled BARF. Native Android app; Kotlin, Jetpack Compose, Room/SQLite, and WorkManager. No backend, account, analytics, AI, or cloud backup. Android 8.0+ (API 26). Current version: 0.1.3.

## Download

Get **BARF-0.1.3.apk** from [the latest GitHub release](https://github.com/alienvisa/BARF-Boogies-Aggregated-RSS-Feed/releases/latest). It is the single optimized, universal APK for Android 8.0+. The existing signing identity is retained, so installing over a prior BARF build keeps local data. Android may require permission to install from your download app.


## Obtainium updates

[Add BARF to Obtainium](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22app.localbrief%22%2C%22name%22%3A%22BARF%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Falienvisa%2FBARF-Boogies-Aggregated-RSS-Feed%22%2C%22author%22%3A%22alienvisa%22%7D), or open Obtainium → Add app and paste:

https://github.com/alienvisa/BARF-Boogies-Aggregated-RSS-Feed

The repository is public; no GitHub token is required. Normal releases contain exactly one APK, with matching Android/release version names, increasing version codes, and the same package ID/signing certificate. Keep standard version detection enabled; no APK filter or prerelease option is needed for v0.1.3 onward. Initial installs may require Android's install permission. Automatic installation depends on Obtainium and Android settings and is not guaranteed.

See [Obtainium version tracking](https://wiki.obtainium.imranr.dev/app_tracking/), [configuration](docs/obtainium.json), and [release checklist](docs/releasing.md).

## Included

- Today, Topics, Saved, and Manage sources using the approved Light/Dark color schemes.
- System/Light/Dark appearance preference.
- Direct HTTPS RSS/Atom fetching, website feed discovery, YouTube channel feeds.
- Source management; topic assignment; comma-separated include/exclude phrases.
- Original headlines and source previews, without summaries.
- One card per canonical link, with topic matching across its source occurrences.
- Search saved items and sources; All/Articles/Videos filters.
- Save/unsave with Undo; Android share-out; receive HTTPS links from other apps.
- Refresh-on-open, pull-to-refresh, manual refresh, and battery-aware hourly background work.
- Wi-Fi-only option (Android unmetered-network constraint), optional thumbnails, 20 MB disk image cache.
- Local JSON backup/restore of sources, topics, bookmarks, and preferences.
- No sample subscriptions inserted automatically.

## Retention

Unsaved feed content expires **14 days after first arrival on this device**, not 14 days after the publisher's date. Saved links/previews stay until removed. Cleanup runs on startup and refresh; it is not an exact wall-clock deletion alarm.

The app keeps a small URL/arrival ledger for each source's current feed window (up to 250 entries) so expired content is not repeatedly reimported. This ledger contains no titles or previews and shrinks when entries leave the source feed. Saved content survives source removal. Unsaving an expired item makes it eligible for the next cleanup.

## First use

1. Install the development APK and open BARF.
2. Open Topics → Manage sources → Add source.
3. Paste an HTTPS website/feed URL or YouTube channel URL/ID. Names are optional for new sources.
4. Add topics, connect sources, and optionally define include/exclude phrases.
5. Browse Today; tap a headline to read the original page inside the app. Close it to return to the feed.
6. In a browser or YouTube, use Share → BARF to save a link locally.
7. Export a backup from Settings before uninstalling or changing phones.

## Source compatibility and boundaries

Website discovery selects the first advertised RSS/Atom alternate feed. A source with several feeds can be added using the specific feed URL instead. Sites without feeds are not monitored. YouTube channel ID URLs and direct channel feeds are the most dependable inputs; handle pages are supported when feed/channel metadata is present, but YouTube can restrict page access. No YouTube API key is embedded.

Feeds and links must use HTTPS; HTTP-only entries are skipped. Requests have timeouts, bounded response sizes, and limited redirects. Article markup is reduced to plain source text. Sources are retried independently and errors appear in Manage sources.

Incoming shared links save the supplied title when available, otherwise the URL. The app does not scrape full articles to invent missing previews. Original pages open in the in-app viewer and require connectivity. Open externally remains available for a full browser, downloads, and unsupported video/site features. Content already stored locally is available offline; automatic collection requires internet access. There is no cross-device sync.

Background refresh is approximate and can be delayed by Android battery restrictions or stop after force-stop until the app is reopened. There are no notifications in this first development build. Ordinary feed history and thumbnail caches are excluded from backups; saved content is included. Exported JSON is readable by whoever has the file. Restore validates before replacing data and uses a database transaction; the app asks before replacement.

## Design references

The [Design Bible](docs/design-bible.md) and [Mockups Tracker](docs/mockups-tracker.md) are the shared visual specifications. Add/Edit dialogs and Settings are functional extensions using those tokens; their detailed layouts have not had separate mockup approval.

## Build

Open this directory in Android Studio with JDK 17, Android SDK 35, and Build Tools 35.0.0. The pinned compatible build stack uses AGP 8.9.2, Gradle 8.11.1, Kotlin 2.1.20, and Compose BOM 2025.04.01. Dependencies are pinned rather than tracking latest releases.

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On macOS/Linux:

```sh
sh gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Set the local Android SDK path through Android Studio or an ignored local.properties file. The project's current local.properties is workspace-specific and is excluded from the packaged source archive. The Gradle wrapper downloads its pinned distribution on first use.

Debug APK output: app/build/outputs/apk/debug/app-debug.apk. GitHub distribution retains the existing development signing certificate for update compatibility. Keep this signing key private and backed up; builds signed with a different key cannot update existing installs.

For the smaller optimized development build, run `:app:assemblePreview :app:lintPreview`. The internally named preview variant shrinks unused code/resources and uses the existing signing key. Since v0.1.3, it has no version-name suffix: its Android version matches the GitHub tag after stripping the leading v. Its output is app/build/outputs/apk/preview/app-preview.apk; distribute it as BARF-<version>.apk. Both visual fixes were verified on a Pixel 9a; automated screen/data tests run against the debug variant.

When rebuilding on another computer, Android Studio normally generates a different development signing key. Export a backup before uninstalling if Android refuses an update because its signing certificate differs.

## Verification

Unit tests exercise feed parsing, keyword filtering, URL normalization, share reception, retention boundaries, bookmarked-content protection, source deletion, duplicate membership, expired-feed replay prevention, and transactional backup recovery. See the separate v1 build report for the actual results and device-testing status.

## Design references

See [Design Bible](docs/design-bible.md), [Mockups Tracker](docs/mockups-tracker.md), and [v1 Scope](docs/v1-scope.md). The approved card-stack app icon is recreated as an Android vector with adaptive masks and a monochrome themed variant. App ID and signing identity are retained so this update preserves existing local data.

## In-app article viewer

Headlines open a single-page viewer with the current site domain, loading progress, page Back/Forward, Stop/Reload, Close, Save, Share, and Open externally. Closing returns to the same feed/topic/saved screen. Android Back traverses page history before closing. Save and Share use the current page after following links; bookmarks retain the existing source preview when the page already exists in the library.

HTTPS pages render through the device's WebView, with JavaScript and first-party cookies for ordinary site compatibility. File/content navigation, insecure mixed content, third-party cookies, automatic media playback, and automatic popup windows are disabled. No native JavaScript bridge is exposed. Browsing state/cookies/cache stay on this device and are excluded from app exports and cloud backup. Publishers' own embedded services can still make network requests.

This is the publisher's normal page, including its ads, sign-in prompts, and paywalls. App theme colors apply to the viewer controls; pages keep their own appearance. Full-screen video, popup-based sign-in, browser-specific features, and PDF downloads may need Open externally. Downloads are handed to an external browser; the app does not request file storage permissions. On rotation or process restoration, the current URL reopens; page history and scroll position may reset. Page cache is not a guaranteed offline article download, and the 14-day feed retention rule does not manage website cookies/cache.
