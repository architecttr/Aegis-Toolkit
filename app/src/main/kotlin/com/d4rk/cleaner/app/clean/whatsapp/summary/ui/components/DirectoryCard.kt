package com.d4rk.cleaner.app.clean.whatsapp.summary.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.DirectoryItem
import com.d4rk.cleaner.app.core.ui.components.GridCardItem

@Composable
fun DirectoryCard(
    item: DirectoryItem,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    GridCardItem(
        modifier = modifier,
        iconPainter = painterResource(id = item.icon),
        title = item.name,
        subtitle = item.size,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = { onOpenDetails(item.type) },
    )
}

