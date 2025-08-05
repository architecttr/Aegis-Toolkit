# Cleaning Screens and Flows

## 1. Dashboard Screen
**Trigger**
- User opens the app or returns from other screens.

**UI states**
- Shows storage stats and entry points for other cleaners.
- No background scanning; every action requires a tap.

**State persistence**
- Stats are loaded from repository each time the screen is shown.
- No disk state is persisted; values may be stale after process death.

**Error handling**
- IO failures surface as snackbars and the screen falls back to the last known values.

**System events**
- Process death simply reloads stats on next visit.
- Removing the app from memory discards any cached values.

## 2. Analyze Screen
**Trigger**
- User presses **Start scan** from the Analyze tab.

**UI states**
1. *Idle* – waiting for the user to initiate a scan.
2. *Analyzing* – shows determinate progress and disables input.
3. *ReadyToClean* – lists results, including empty folders and duplicates.
4. *Cleaning* – WorkManager job running; selections disabled.
5. *Complete/Error* – results updated, job ID cleared.

**State persistence**
- `UiScannerModel` holds results in memory only; a crash or process kill removes them.
- Active job IDs are persisted via `DataStore` for recovery.

**Error handling**
- Failures during analysis or cleaning post a snackbar and move the state to `Error`.

**System events**
- On restart, the ViewModel reattaches to any running cleanup job using the stored ID and clears stale IDs when no work exists【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/scanner/ui/ScannerViewModel.kt†L142-L164】.

## 3. WhatsApp Cleaner Screen
**Trigger**
- User navigates from Dashboard or Analyze to the WhatsApp section.

**UI states**
- Idle → Scanning individual WhatsApp directories → Displaying media categories → Cleaning.

**State persistence**
 - Lists and selections remain in memory; job IDs stored in `DataStore` under `whatsappCleanWorkId`.
 - `FileCleaner.enqueue` schedules work, stores `whatsappCleanWorkId`, and prevents duplicate jobs.
 - `observeFileCleanWork` reads that ID to report progress and surface per-file failures from worker output.

**Error handling**
- Scan or delete failures show snackbars; media lists fall back to empty results.

**System events**
 - Because selections and lists are memory-only, process death forces a rescan unless a job is active; when active, `observeFileCleanWork` reattaches using the stored ID.

## 4. Large Files Scanner
**Trigger**
- User taps **Large files** from Dashboard or Analyze.

**UI states**
1. Idle/Loading – fetching the largest files.
2. Ready – list of large files with selection checkboxes.
3. Cleaning – WorkManager job removes selected files.
4. Complete/Error – list refreshed and job ID cleared.

**State persistence**
- File list kept in memory; selections lost on process death.
- Job ID persisted as `largeFilesCleanWorkId` for recovery【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/largefiles/ui/LargeFilesViewModel.kt†L45-L58】.

**Error handling**
- Failures show snackbars and keep the last known list.

**System events**
- ViewModel reconnects to running jobs and clears stale IDs on start【F:app/src/main/kotlin/com/d4rk/cleaner/app/clean/largefiles/ui/LargeFilesViewModel.kt†L130-L146】.

## 5. Other Screens
- [Clipboard Cleaner](clipboard_cleaner.md)
- [Empty Folder Cleaner](empty_folder_cleaner.md)
- [Trash Recovery](trash_recovery.md)
- [App Manager](app_manager.md)

All cleanup operations share a common pattern: user action → handler → WorkManager job → notification → UI/ID reset.
