# Contributing to Smart Cleaner

We welcome pull requests!

* Follow the coding style of existing files.
* Run `./gradlew ktlintCheck` and `./gradlew test` before submitting.
* When adding file previews or icons, ensure a placeholder is visible while the actual image loads. Icons loaded via `FilePreviewHelper` must show a visually appropriate placeholder until ready.
* File cleanup no longer uses a foreground service. Instead, `FileCleanupWorker`
  runs deletion and trash operations using WorkManager. If Android introduces a
  dedicated file-management foreground service, update the worker accordingly.


