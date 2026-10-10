# v1 implementation scope

October 10, 2026

## Accepted architecture

One native Android app, with everything kept locally on the device. Kotlin + Jetpack Compose, Room/SQLite for data, WorkManager for approximate hourly refresh. No server, account, browser/PWA dependency, telemetry, AI services, or cloud backup. Internet requests go directly to user-selected sources and their thumbnails. Reading an original page also contacts its publisher and embedded services.

## Accepted product scope

Today, Topics, Saved, and Manage sources; Light/Dark/System themes; source previews without generated summaries; source-first topics and keyword exclusions; direct feed collection; local bookmarks/search; Android sharing; and user-controlled export/import.

Unsaved feed content has **14-day retention after arrival**. Saved content is retained until removed. Lightweight cached reading is available offline; original pages are read in the in-app WebView, with an external-open fallback. Full-article downloads are not included.

The approved design bible and eight approved mockups remain the visual baseline. Settings and source/topic editing are necessary functional extensions; no new main navigation tabs are added.

## Delivery

The first milestone is a buildable native project plus an installable development APK, meaningful automated checks, and a clear report of remaining device validation. Notifications, full-article downloads, multi-device sync, web discovery beyond chosen sources, and public-store release are outside this initial development milestone.

The app is named BARF (Boogies Aggregated RSS Feed).
