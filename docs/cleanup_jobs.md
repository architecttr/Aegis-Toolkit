# Cleanup job observation

File deletions and trash moves are executed with `WorkManager`.
UI components must observe the `WorkInfo` for the returned work ID and update
state based on `WorkInfo.state`. This ensures that the result is reflected even
if the process was killed or the work was deferred. An in-process
`CleaningEventBus` can dispatch fast updates, but `WorkInfo` remains the source
of truth and must always be observed when launching cleanup work.
