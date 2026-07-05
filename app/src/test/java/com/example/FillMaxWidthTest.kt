package com.example

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FillMaxWidthTest {
    @Test
    fun test() {
        Modifier.fillMaxWidth(0f)
    }
}
