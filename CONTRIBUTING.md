# Contributing to Smart Cleaner

We welcome pull requests!

* Follow the coding style of existing files.
* Run `./gradlew ktlintCheck` and `./gradlew test` before submitting.
* When adding file previews or icons, ensure a placeholder is visible while the actual image loads. Icons loaded via `FilePreviewHelper` must show a visually appropriate placeholder until ready.
* Foreground service usage must comply with the [Google Play policy](https://developer.android.com/guide/components/foreground-services#types).
  `FileOperationService` uses the `fileManagement` type, calls
  `ServiceCompat.startForeground()` with
  `ServiceInfo.FOREGROUND_SERVICE_TYPE_FILE_MANAGEMENT`, and shows a
  persistent notification.


