package com.d4rk.cleaner.app.clean.memory.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants

@Composable
fun StorageBreakdownGrid(
    storageBreakdown: Map<String, Long>,
    onItemClick: (String) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SizeConstants.MediumSize)
            .animateContentSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SizeConstants.LargeIncreasedSize))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SizeConstants.ExtraTinySize)) {
                storageBreakdown.entries.toList().chunked(size = 2)
                    .forEach { chunk: List<Map.Entry<String, Long>> ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            horizontalArrangement = Arrangement.spacedBy(
                                space = SizeConstants.ExtraTinySize,
                                alignment = Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (item: Map.Entry<String, Long> in chunk) {
                                val (icon: String, size: Long) = item
                                StorageBreakdownItem(
                                    icon = icon,
                                    size = size,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onItemClick(icon) }
                                )
                            }
                        }
                    }
            }
        }
    }
}

