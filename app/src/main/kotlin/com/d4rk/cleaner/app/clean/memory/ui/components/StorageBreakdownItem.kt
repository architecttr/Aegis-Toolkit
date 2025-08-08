package com.d4rk.cleaner.app.clean.memory.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SnippetFolder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.core.ui.components.GridCardItem
import com.d4rk.cleaner.core.utils.helpers.FileSizeFormatter.format as formatSize

@Composable
fun StorageBreakdownItem(
    icon: String,
    size: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val storageIcons: Map<String, ImageVector> = mapOf(
        stringResource(id = R.string.installed_apps) to Icons.Outlined.Apps,
        stringResource(id = R.string.system) to Icons.Outlined.Android,
        stringResource(id = R.string.music) to Icons.Outlined.MusicNote,
        stringResource(id = R.string.images) to Icons.Outlined.Image,
        stringResource(id = R.string.documents) to Icons.Outlined.FolderOpen,
        stringResource(id = R.string.downloads) to Icons.Outlined.Download,
        stringResource(id = R.string.other_files) to Icons.Outlined.FolderOpen,
    )

    GridCardItem(
        modifier = modifier,
        iconVector = storageIcons[icon] ?: Icons.Outlined.SnippetFolder,
        title = icon,
        subtitle = formatSize(size),
        containerColor = MaterialTheme.colorScheme.surface,
        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = onClick,
    )
}

