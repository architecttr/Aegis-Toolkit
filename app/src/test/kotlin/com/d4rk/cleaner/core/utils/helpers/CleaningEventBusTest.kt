package com.d4rk.cleaner.core.utils.helpers

import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.CleaningState
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiAnalyzeModel
import com.d4rk.cleaner.app.clean.scanner.domain.data.model.ui.UiScannerModel
import com.d4rk.android.libs.apptoolkit.core.domain.model.ui.UiStateScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CleaningEventBusTest {

    @Test
    fun `event resets cleaning state`() = runTest {
        val uiState = MutableStateFlow(UiStateScreen(data = UiScannerModel(analyzeState = UiAnalyzeModel(state = CleaningState.Cleaning))))

        val job = launch {
            CleaningEventBus.events.collect { success ->
                uiState.value = uiState.value.copy(
                    data = uiState.value.data?.copy(
                        analyzeState = uiState.value.data!!.analyzeState.copy(
                            state = if (success) CleaningState.Result else CleaningState.Error
                        )
                    )
                )
            }
        }

        CleaningEventBus.notifyCleaned(success = true)
        advanceUntilIdle()
        assertEquals(CleaningState.Result, uiState.value.data?.analyzeState?.state)

        CleaningEventBus.notifyCleaned(success = false)
        advanceUntilIdle()
        assertEquals(CleaningState.Error, uiState.value.data?.analyzeState?.state)

        job.cancel()
    }
}
