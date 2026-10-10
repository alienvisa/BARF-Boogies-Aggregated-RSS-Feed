# BARF v0.1.3 build report

October 10, 2026. Release of headline clipping and Undo timeout fixes, with Obtainium-compatible distribution.

| Check | Result |
|---|---|
| Optimized preview build | Passed |
| Automated debug tests | 24 passed, zero failures/errors |
| Preview lint | Zero errors |
| APK size | 1,756,122 bytes (1.67 MiB) |
| APK package | app.localbrief |
| APK label | BARF |
| versionName | 0.1.3, with no suffix |
| versionCode | 4, increased from 3 |
| APK signature | Verified; same certificate as preceding builds |
| GitHub distribution | Public repository; one installable APK; normal release tagged v0.1.3 |

APK SHA-256: 7F603FBE1265164FD2974A8998117BF4519B7D642BF9DB7957AE7C6005E5B5C9.

Signing certificate SHA-256: 74034762E2AE90BFCA3BBBDC68C98B4B141624D452EE365EB7430FB7CB41D03E.

The optimized build type remains named preview internally, but its versionName now matches the release version. It retains the private local development key for compatibility. No signing key is included in Git or the source archive.

The two fixes were verified on Pixel 9a / Android 17. See phone-testing-2026-10-10.md for the actual device coverage and limitations. Natural overnight refresh/battery, older devices, TalkBack, and full-screen video/audio remain pending.

Obtainium consumes the repository's public GitHub releases. A single APK avoids variant selection, ordinary tags/latest releases avoid prerelease-only discovery, and a plain matching versionName supports standard version detection. See releasing.md and obtainium.json for future-release conventions.
