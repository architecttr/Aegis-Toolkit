package com.d4rk.cleaner.app.clean.trash.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import com.d4rk.cleaner.core.ui.BaseCleanupActivity

class TrashActivity : BaseCleanupActivity() {

    @Composable
    override fun ScreenContent() {
        TrashScreen(activity = this@TrashActivity)
    }
}