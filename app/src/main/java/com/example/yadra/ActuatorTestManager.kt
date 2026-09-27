package com.example.yadra

import android.os.Handler
import android.os.Looper

object ActuatorTestManager {

    private const val TEST_DURATION_MS = 3000L

    private val handler =
        Handler(Looper.getMainLooper())

    @Volatile
    private var testRunning = false

    fun isRunning(): Boolean {
        return testRunning
    }

    fun startTest(
        startCommand: String,
        stopCommand: String,
        onStarted: () -> Unit = {},
        onStopped: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean {

        if (testRunning) {
            return false
        }

        if (startCommand.isBlank()) {
            onError("فرمان شروع تست مشخص نشده است")
            return false
        }

        if (stopCommand.isBlank()) {
            onError("فرمان توقف تست مشخص نشده است")
            return false
        }

        testRunning = true

        Thread {

            try {

                val startResponse =
                    sendCommand(startCommand)

                if (startResponse.isBlank()) {

                    testRunning = false

                    runOnMain {
                        onError(
                            "ECU پاسخی برای شروع تست نداد"
                        )
                    }

                    return@Thread
                }

                runOnMain {
                    onStarted()
                }

                Thread.sleep(
                    TEST_DURATION_MS
                )

                sendCommand(
                    stopCommand
                )

                testRunning = false

                runOnMain {
                    onStopped()
                }

            } catch (e: Exception) {

                testRunning = false

                /*
                 * تلاش برای ارسال STOP حتی
                 * در صورت بروز خطا
                 */
                try {
                    sendCommand(
                        stopCommand
                    )
                } catch (_: Exception) {
                }

                runOnMain {
                    onError(
                        e.message
                            ?: "خطای نامشخص در تست عملگر"
                    )
                }
            }
        }.start()

        return true
    }

    fun stopImmediately(
        stopCommand: String
    ) {

        if (stopCommand.isBlank()) {
            testRunning = false
            return
        }

        Thread {

            try {

                sendCommand(
                    stopCommand
                )

            } catch (_: Exception) {
            }

            testRunning = false

        }.start()
    }

    private fun sendCommand(
        command: String
    ): String {

        return when (
            ConnectionSource.activeSource
        ) {

            ConnectionSource.Source.WIFI -> {

                if (
                    !YadraConnectionManager.elmConnected
                ) {
                    ""
                } else {

                    YadraConnectionManager
                        .sendCommand(command)
                }
            }

            ConnectionSource.Source.BLUETOOTH -> {

                if (
                    !BluetoothConnectionManager.elmConnected
                ) {
                    ""
                } else {

                    BluetoothConnectionManager
                        .sendActiveCommand(command)
                }
            }

            ConnectionSource.Source.NONE -> {
                ""
            }
        }
    }

    private fun runOnMain(
        action: () -> Unit
    ) {

        handler.post {
            action()
        }
    }
}