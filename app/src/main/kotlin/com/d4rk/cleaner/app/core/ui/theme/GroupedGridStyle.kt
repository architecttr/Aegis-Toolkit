package com.d4rk.cleaner.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants

object GroupedGridStyle {
    val cardShape = RoundedCornerShape(SizeConstants.ExtraTinySize)
    val gridClipShape = RoundedCornerShape(SizeConstants.LargeIncreasedSize)

    @Composable
    fun cardColors(containerColor: Color = MaterialTheme.colorScheme.surface): CardColors =
        CardDefaults.cardColors(containerColor = containerColor)

    val iconContainerColor: Color
        @Composable get() = MaterialTheme.colorScheme.primaryContainer
}

