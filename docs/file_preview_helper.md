# File Preview Helper

`FilePreviewHelper` centralizes logic for rendering previews of files throughout the app. Use the `preview` function whenever a thumbnail or icon is needed.

```
@Composable
fun FilePreviewHelper.Preview(file: File, modifier: Modifier = Modifier)
```

The helper determines the file `PreviewType` from the extension and provides an appropriate thumbnail when possible (images, videos, PDFs, APKs) or falls back to an icon. Extend the `PreviewType` sealed class and update `getPreviewType` to support new file formats.

Plain text files (`.txt`, `.log`, `.csv`, etc.) display the first three lines of content when readable. Large or binary files automatically fall back to the default icon.

Archive files (`.zip`, `.rar`, `.7z`) show the archive icon with a summary of the number of items contained inside, e.g. `ZIP (12 files)`. Entry counts are calculated using `commons-compress` and capped at 10k entries to avoid UI slowdowns on huge or corrupted archives.
