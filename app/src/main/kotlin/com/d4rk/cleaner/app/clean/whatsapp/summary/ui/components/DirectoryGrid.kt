package com.d4rk.cleaner.app.clean.whatsapp.summary.ui.components

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
import com.d4rk.cleaner.app.clean.whatsapp.summary.domain.model.DirectoryItem

@Composable
fun DirectoryGrid(items: List<DirectoryItem>, onOpenDetails: (String) -> Unit) {
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
                items.chunked(2).forEach { chunk ->
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
                        for (item in chunk) {
                            DirectoryCard(
                                item = item,
                                onOpenDetails = onOpenDetails,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (chunk.size == 1) {
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

