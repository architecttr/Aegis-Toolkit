package com.d4rk.cleaner.app.clean.scanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ImageSearch
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.dashboard.ui.components.DashboardActionCard

@Composable
fun ImageOptimizerCard(
    modifier: Modifier = Modifier,
    lastOptimized: String? = null,
    onOptimizeClick: () -> Unit,
) {
    DashboardActionCard(
        modifier = modifier,
        icon = Icons.Outlined.Image,
        title = stringResource(id = R.string.image_optimizer_card_title),
        subtitle = stringResource(id = R.string.image_optimizer_card_subtitle),
        actionLabel = stringResource(id = R.string.optimize_image),
        actionIcon = Icons.Outlined.ImageSearch,
        onActionClick = onOptimizeClick
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoSizeSelectLarge,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        AnimatedVisibility(visible = lastOptimized != null) {
            lastOptimized?.let { size ->
                Text(
                    text = stringResource(id = R.string.image_optimizer_last_format, size),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.animateContentSize()
                )
            }
        }
    }
}
