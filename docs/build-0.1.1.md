# BARF · Build 0.1.1

October 10, 2026

The approved card-stack concept is now a native Android vector launcher icon, using exact forest (#172815), warm white (#FFF8F3), and mint (#A9FFCB). Adaptive icon foreground/background layers support Android launcher masks. A monochrome version supports themed icons on compatible launchers.

Version code is 2; preview version is 0.1.1-preview. Application ID app.localbrief and the development signing certificate are unchanged, allowing an in-place upgrade of the prior build. The installed app label remains Local Brief; BARF - Boogies Aggregated RSS Feed is the repository/project title.

## Verification

- Preview and debug APKs built successfully.
- Both lint checks passed with no errors. Ten advisory warnings: nine dependency updates and one redundant v26 resource-folder qualifier.
- Preview APK signature verified. Certificate SHA-256: 74034762E2AE90BFCA3BBBDC68C98B4B141624D452EE365EB7430FB7CB41D03E.
- Packaged metadata verified: version code 2, version 0.1.1-preview, minimum Android API 26.
- Preview APK SHA-256: E9AEE232FF665A289A6640AB9D0733F6910DCFBFD4B7B11C657103863DE90239.
- Debug APK SHA-256: 5703A3ED3232A4F765D9F2E18C960BA72AFAB74F840C60B823FB1DDBB65F4CC0.

The prior 19 automated tests passed for 0.1.0; they were not rerun for this icon/resource-only update. Physical-phone testing and launcher-mask visual testing are still pending. Signing keys, SDK paths, caches, and compiled APKs are excluded from the source repository.
