# BARF - Boogies Aggregated RSS Feed

Repository/project title: BARF - Boogies Aggregated RSS Feed. The installed app is currently labeled Local Brief. Native Android app; Kotlin, Jetpack Compose, Room/SQLite, and WorkManager. No backend, account, analytics, AI, or cloud backup. Android 8.0+ (API 26). This is the first development build, version 0.1.1.

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

1. Install the development APK and open Local Brief.
2. Open Topics → Manage sources → Add source.
3. Paste an HTTPS website/feed URL or YouTube channel URL/ID. Names are optional for new sources.
4. Add topics, connect sources, and optionally define include/exclude phrases.
5. Browse Today; tap headlines to open originals, or Save/Share.
6. In a browser or YouTube, use Share → Local Brief to save a link locally.
7. Export a backup from Settings before uninstalling or changing phones.

## Source compatibility and boundaries

Website discovery selects the first advertised RSS/Atom alternate feed. A source with several feeds can be added using the specific feed URL instead. Sites without feeds are not monitored. YouTube channel ID URLs and direct channel feeds are the most dependable inputs; handle pages are supported when feed/channel metadata is present, but YouTube can restrict page access. No YouTube API key is embedded.

Feeds and links must use HTTPS; HTTP-only entries are skipped. Requests have timeouts, bounded response sizes, and limited redirects. Article markup is reduced to plain source text. Sources are retried independently and errors appear in Manage sources.

Incoming shared links save the supplied title when available, otherwise the URL. The app does not scrape full articles to invent missing previews. Original articles/videos open in other applications and require connectivity. Content already stored locally is available offline; automatic collection requires internet access. There is no cross-device sync.

Background refresh is approximate and can be delayed by Android battery restrictions or stop after force-stop until the app is reopened. There are no notifications in this first development build. Ordinary feed history and thumbnail caches are excluded from backups; saved content is included. Exported JSON is readable by whoever has the file. Restore validates before replacing data and uses a database transaction; the app asks before replacement.

## Design references

The [Design Bible](../design-bible.md) and [Mockups Tracker](../mockups-tracker.md) are the shared visual specifications. Add/Edit dialogs and Settings are functional extensions using those tokens; their detailed layouts have not had separate mockup approval.

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

Debug APK output: app/build/outputs/apk/debug/app-debug.apk. Debug signing is for local development; a separately managed release signing key is needed before public distribution.

For the smaller optimized development build, run `:app:assemblePreview :app:lintPreview`. The preview variant shrinks unused code/resources and uses the same development signing key. It is not a public release. Its output is app/build/outputs/apk/preview/app-preview.apk. The distributed preview APK has not been installed on a physical phone here; the automated screen/data tests run against the debug variant.

When rebuilding on another computer, Android Studio normally generates a different development signing key. Export a backup before uninstalling if Android refuses an update because its signing certificate differs.

## Verification

Unit tests exercise feed parsing, keyword filtering, URL normalization, share reception, retention boundaries, bookmarked-content protection, source deletion, duplicate membership, expired-feed replay prevention, and transactional backup recovery. See the separate v1 build report for the actual results and device-testing status.

## Design references

See [Design Bible](docs/design-bible.md), [Mockups Tracker](docs/mockups-tracker.md), and [v1 Scope](docs/v1-scope.md). The approved card-stack app icon is recreated as an Android vector with adaptive masks and a monochrome themed variant. App ID and signing identity are retained so this update preserves existing local data.
