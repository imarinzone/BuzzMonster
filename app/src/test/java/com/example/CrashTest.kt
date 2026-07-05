package com.example

import androidx.test.core.app.ActivityScenario
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CrashTest {
    @Test
    fun testCrash() {
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    println("Activity launched successfully without crash")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
