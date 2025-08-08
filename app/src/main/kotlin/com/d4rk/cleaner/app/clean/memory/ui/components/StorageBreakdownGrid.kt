package com.d4rk.cleaner.app.clean.memory.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants

@Composable
fun StorageBreakdownGrid(
    storageBreakdown: Map<String, Long>,
    onItemClick: (String) -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = SizeConstants.MediumSize),
        shape = RoundedCornerShape(SizeConstants.MediumSize),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SizeConstants.ExtraTinySize)
        ) {
            storageBreakdown.entries.toList().chunked(size = 2)
                .forEach { chunk: List<Map.Entry<String, Long>> ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
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
