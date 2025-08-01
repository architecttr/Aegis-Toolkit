# File Preview Helper

`FilePreviewHelper` centralizes logic for rendering previews of files throughout the app. Use the `preview` function whenever a thumbnail or icon is needed.

```
@Composable
fun FilePreviewHelper.Preview(file: File, modifier: Modifier = Modifier)
```

The helper determines the file `PreviewType` from the extension and provides an appropriate thumbnail when possible (images, videos, PDFs, APKs) or falls back to an icon. Extend the `PreviewType` sealed class and update `getPreviewType` to support new file formats.
