package com.d4rk.cleaner.app.clean.analyze.ui.components

import android.content.Context
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4rk.android.libs.apptoolkit.core.ui.components.modifiers.bounceClick
import com.d4rk.android.libs.apptoolkit.core.utils.constants.ui.SizeConstants
import com.d4rk.cleaner.R
import com.d4rk.cleaner.app.clean.scanner.utils.helpers.FilePreviewHelper
import com.d4rk.cleaner.core.utils.helpers.FileManagerHelper
import java.io.File

@Composable
fun FileCard(
    modifier: Modifier = Modifier,
    file: File, onCheckedChange: (Boolean) -> Unit,
    isChecked: Boolean,
    isOriginal: Boolean = false,
    view: View,
) {
    val context: Context = LocalContext.current

    Card(
        modifier = modifier
            .aspectRatio(ratio = 1f)
            .bounceClick()
            .clickable {
                if (!file.isDirectory) {
                    FileManagerHelper.openFile(context, file)
                }
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            FilePreviewHelper.Preview(file = file, modifier = Modifier.align(Alignment.Center))

            Checkbox(checked = isChecked, onCheckedChange = { checked ->
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onCheckedChange(checked)
            }, modifier = Modifier.align(alignment = Alignment.TopEnd))

            if (isOriginal) {
                Text(
                    text = stringResource(id = R.string.original),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(Color.Red.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.Black.copy(alpha = 0.4f)
                    )
                    .align(alignment = Alignment.BottomCenter)
            ) {
                Text(
                    text = file.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .basicMarquee()
                        .padding(all = SizeConstants.SmallSize)
                )
            }
        }
    }
}