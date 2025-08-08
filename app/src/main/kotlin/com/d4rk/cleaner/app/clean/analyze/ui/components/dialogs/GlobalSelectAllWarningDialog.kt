package com.d4rk.cleaner.app.clean.analyze.ui.components.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.d4rk.android.libs.apptoolkit.core.ui.components.dialogs.BasicAlertDialog
import com.d4rk.cleaner.R

@Composable
fun GlobalSelectAllWarningDialog(
    onConfirm: (dontShowAgain: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    BasicAlertDialog(
        title = stringResource(id = R.string.select_all_files_title),
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.select_all_files_message))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                    Text(text = stringResource(id = R.string.dont_show_warning_again))
                }
            }
        },
        confirmButtonText = stringResource(id = R.string.select_all),
        onConfirm = { onConfirm(dontShowAgain) },
        onDismiss = onDismiss
    )
}
