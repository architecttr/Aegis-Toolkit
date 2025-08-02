# Cleanup job observation

File deletions and trash moves are executed with `WorkManager`.
UI components must observe the `WorkInfo` for the returned work ID and update
state based on `WorkInfo.state`. This ensures that the result is reflected even
if the process was killed or the work was deferred. An in-process
`CleaningEventBus` can dispatch fast updates, but `WorkInfo` remains the source
of truth and must always be observed when launching cleanup work.

## Job Lifecycle and UI Sync

Cleanup work IDs are persisted to `DataStore` immediately after enqueuing to
survive process death. A single-job policy is enforced per cleanup feature: if
an active ID is found in `DataStore` and the corresponding `WorkInfo` is still
running, the UI blocks additional launches and surfaces a "Cleaning in progress"
message. When the observed `WorkInfo` reaches a terminal state or disappears
entirely, the stored ID is cleared so `DataStore` never accumulates stale
entries.

On startup, any ID whose `WorkInfo` no longer exists is treated as orphaned and
removed. This best-effort approach covers most cases, though a crash between
enqueueing and ID persistence may still leave an untracked job until the next
launch cleans it up. Multi-job support remains undefined.
