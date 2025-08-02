# Cleaning Screens and Flows

## 1. Dashboard Screen
**What happens here**
- Displays overall device stats such as used/free space and file type counts.
- Provides buttons for actions like "Scan for junk", "Large files", and "WhatsApp cleaner".
- No automatic scans: every action requires a user tap.

**Effect on state**
- Performs lightweight disk queries; heavy analysis is deferred to later screens.
- When the user leaves, the system may clear the Dashboard from memory.
- Shows the latest known summary, which may be stale if no recent scan ran.

## 2. Analyze Screen
**UI state flow**
- Initial: offers a "Start scan" action.
- Scanning: shows a progress indicator and blocks other input except cancel.
- Results: after scanning, categories (images, videos, etc.) appear with counts and sizes.
- Ready to clean: user can select files or categories for cleanup.
- Cleaning: disables selection and shows cleanup progress until completion.

**Internal details**
- ViewModel launches file analysis on scan.
- Results streamed via `MutableStateFlow` to the UI.
- File metadata is cached in RAM for the session only.
- File selections are transient and not persisted.
- Both scan results and selections live only in memory; if the process dies they are cleared and require a new scan.

## 3. File Discovery, Storage, and Cleaning Flow
**File discovery**
- The analyzer walks storage to collect files by type or category.
- Optionally groups duplicates via hashes or names.
- Results stored in a `UiScannerModel`.

**UI presentation**
- Each tab lists matching files using a `LazyColumn`.
- Users can filter, select all, or pick individual files.

**Move to trash / Delete**
- User action triggers `CleanOperationHandler`.
- Existing jobs are checked; if one is active, the new request is blocked.
- Otherwise a WorkManager job is enqueued and its ID persisted.

**Worker jobs**
- Run in the foreground and process each file sequentially.
- Notifications update with exact progress and final status.
- UI state resets to ready or error and the job ID is cleared.

## 4. WhatsApp Cleaner Screen
- Mirrors the Analyze screen but targets WhatsApp media folders (images, videos, documents, etc.).
- Presents category tabs specific to WhatsApp (stickers, voice notes, and more).
- Uses the same WorkManager cleanup flow and notification handling.
- Job IDs may be tracked separately, but core logic is shared.

## 5. Large Files Scanner
- Starts from Dashboard or Analyze depending on navigation.
- Scans storage for files above a threshold (e.g., >100 MB).
- Results shown in a list with optional filtering.
- User selects files to delete, triggering the standard WorkManager cleanup job.

## Meta-Observations
- All cleanup operations follow a unified pattern: UI event → handler → WorkManager → notification → state sync.
- No background or scheduled cleanups; every operation is user initiated and foregrounded.
- State is resilient: after crashes or restarts, UI and notifications reflect the true job state.
- Users always know what is happening, what succeeded or failed, and when they can retry.

## Documenting New Screens
When adding or documenting a new screen, capture:
- What triggers a scan or cleanup.
- What the user sees during each state.
- How file data is stored or cached.
- What happens on navigation away, crash, or restart.
- Any unique behavior relative to existing screens.
