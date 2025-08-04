# Cleanup Job System: Technical and UX Documentation

Smart Cleaner performs cleanup tasks only when the user explicitly initiates them. Each job runs in the foreground with determinate progress notifications, and its state is preserved so progress can resume after crashes or restarts.

## Overview
Smart Cleaner runs all file deletion and trash moves through WorkManager jobs. Every job begins with explicit user action and presents real-time progress in the UI and notification bar.

## Requirements for New Cleaning Features
* Start via explicit user action
* Persist WorkRequest ID immediately
* Observe WorkInfo for state
* Show determinate progress notification
* Clear ID and reset UI on completion

## Job Lifecycle
### Start
1. User taps a cleanup action (e.g., "Clean", "Delete large files").
2. App checks DataStore for an active job ID. If a job is running, the action is blocked and the user is notified.
3. If no job is active, a new WorkManager job is enqueued and its ID saved to DataStore (e.g., `scannerCleanWorkId`, `largeFilesCleanWorkId`).
4. A foreground notification immediately appears with determinate progress such as `0/184 files cleaned`.

### Progress
* The Worker updates the notification after each file is processed (`n/N files cleaned`).
* UI components observe WorkManager's `WorkInfo` for state, ensuring the UI stays in sync without polling or relying solely on in-process events.

### Completion & Feedback
* When the job finishes—success, failure, or cancellation—the notification updates with a localized result message and is dismissed after a short delay【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/work/FileCleanupWorker.kt†L147-L153】.
* UI state resets to "Ready" or "Error" depending on outcome.
* The stored job ID is cleared from DataStore, either when the Worker finishes or when no WorkInfo is found【F:app/src/main/kotlin/com/d4rk/cleaner/core/data/datastore/DataStore.kt†L88-L103】【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/ui/ScannerViewModel.kt†L142-L164】.

### Process Death & Recovery
* On restart, any persisted job IDs are used to reattach observers to the running WorkManager jobs so progress continues seamlessly.
* If the OS removes the progress notification, the app reattaches to the job on restart and shows the current state.
* If an ID does not match any existing `WorkInfo`, it is cleared automatically.
* Scan results and file selections exist only in RAM. If the process dies before a job starts, these lists reset and require a fresh scan and selection.

## UX & Notification Policy
* **Single job per feature:** The app never allows overlapping cleanup jobs for the same feature.
* **Visible progress:** Notifications always display determinate progress rather than a spinner.
* **Result shown:** Users always see a final notification summarizing success or failure, which persists briefly so they don't miss it.
* **Localization:** All user-facing strings are fully localized.

## Quota & Responsiveness Strategy
* Cleanup work requests are marked as **expedited** with `OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST` so they start promptly but gracefully fall back to regular background execution when quotas are exhausted.
* When more than 100 file paths are queued, the app splits them into sequential `FileCleanupWorker` requests. This keeps each input set below WorkManager's 10 KB limit and avoids hitting expedited quotas while still processing large batches quickly.

## Developer and Contributor Guidelines
* Persist job IDs to DataStore immediately after enqueuing.
* Drive all UI state from observed `WorkInfo` to handle backgrounding and process death.
* Clear stored IDs once work finishes or when `WorkInfo` disappears.
* Block new cleanups while a job for that feature is running and inform the user.
* Do not bypass notification progress logic.
* Localize every notification and error message.
* Update this documentation if the design changes.

## Testing Requirements
* Test success, failure, cancellation, and restart flows.
* Try datasets of varying sizes (1–10, hundreds, thousands of files).
* Verify notifications and UI remain consistent throughout.

## Why This Design?
* **Compliance:** Meets Google Play's requirement for user-initiated, foreground work.
* **Robustness:** Survives process death, backgrounding, and notification interruptions.
* **User trust:** Clearly communicates progress and results to avoid silent file loss or confusion.

## TODO / Future Work
* Support multiple concurrent jobs if features ever require it.
* Allow user-configurable notification duration.
* Add integration tests for process death, race conditions, and orphaned jobs.

For a detailed integration guide, refer to [Cleaning Screens and Flows](cleaning_screens.md).
