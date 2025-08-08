package com.d4rk.cleaner.app.clean.memory.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.d4rk.cleaner.app.core.ui.components.GroupedGridLayout

@Composable
fun StorageBreakdownGrid(
    storageBreakdown: Map<String, Long>,
    onItemClick: (String) -> Unit = {},
) {
    GroupedGridLayout(
        items = storageBreakdown.entries.toList(),
    ) { (icon, size) ->
        StorageBreakdownItem(
            icon = icon,
            size = size,
            modifier = Modifier.weight(1f),
            onClick = { onItemClick(icon) },
        )
    }
}

