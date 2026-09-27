package com.example.yadra

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UnknownEcuScanService : Service() {

    companion object {
        const val ACTION_START = "com.example.yadra.UNKNOWN_ECU_SCAN_START"
        const val ACTION_STOP = "com.example.yadra.UNKNOWN_ECU_SCAN_STOP"

        const val ACTION_PROGRESS = "com.example.yadra.UNKNOWN_ECU_SCAN_PROGRESS"
        const val ACTION_RESULT = "com.example.yadra.UNKNOWN_ECU_SCAN_RESULT"
        const val ACTION_ERROR = "com.example.yadra.UNKNOWN_ECU_SCAN_ERROR"

        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_STATUS = "status"
        const val EXTRA_RESULT = "result"
        const val EXTRA_ERROR = "error"

        private const val CHANNEL_ID = "unknown_ecu_scanner"
        private const val CHANNEL_NAME = "Unknown ECU Scanner"
        private const val NOTIFICATION_ID = 4101

        @Volatile
        var running: Boolean = false
            private set
    }

    private val serviceJob = SupervisorJob()

    private val serviceScope =
        CoroutineScope(
            Dispatchers.IO + serviceJob
        )

    private var scanJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        running = true

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                title = "Unknown ECU",
                text = "در حال آماده‌سازی اسکن..."
            )
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_START -> {
                startScan()
            }

            ACTION_STOP -> {
                stopScan()
            }
        }

        return START_NOT_STICKY
    }

    private fun startScan() {

        if (scanJob?.isActive == true) {
            return
        }

        scanJob = serviceScope.launch {

            try {

                sendProgress(
                    progress = 0,
                    status = "شروع شناسایی ECU ناشناخته..."
                )

                updateNotification(
                    "Unknown ECU",
                    "در حال شروع Discovery..."
                )

                /*
                 * بسیار مهم:
                 *
                 * Scanner خودش ConnectionSource.activeSource
                 * را بررسی می‌کند.
                 *
                 * بنابراین این Service بین Wi-Fi و Bluetooth
                 * دخالت نمی‌کند.
                 */

                val source = ConnectionSource.activeSource

                if (source == ConnectionSource.Source.NONE) {

                    sendError(
                        "هیچ مسیر ارتباطی فعالی برای ECU وجود ندارد."
                    )

                    stopSelfSafely()
                    return@launch
                }

                sendProgress(
                    progress = 5,
                    status = when (source) {
                        ConnectionSource.Source.WIFI ->
                            "مسیر ارتباطی: Wi-Fi"

                        ConnectionSource.Source.BLUETOOTH ->
                            "مسیر ارتباطی: Bluetooth"

                        ConnectionSource.Source.NONE ->
                            "بدون اتصال"
                    }
                )

                updateNotification(
                    "Unknown ECU",
                    "مسیر: ${source.name}"
                )

                /*
                 * ---------------------------------------------------------
                 * مرحله اول:
                 * Fast Reconnect / Saved Profile
                 * ---------------------------------------------------------
                 *
                 * منطق واقعی انتخاب پروفایل در Scanner قرار می‌گیرد.
                 * Service فقط اجرای فرآیند را مدیریت می‌کند.
                 */

                sendProgress(
                    progress = 10,
                    status = "بررسی پروفایل ذخیره‌شده..."
                )

                updateNotification(
                    "Unknown ECU",
                    "بررسی آخرین پروتکل موفق..."
                )

                /*
                 * scanUnknownEcu():
                 *
                 * 1. پروفایل یادگرفته‌شده قبلی
                 * 2. 46 پروفایل فعلی
                 * 3. Discovery probes
                 * 4. Fingerprint
                 * 5. ذخیره پروفایل جدید
                 */
                val result = withContext(Dispatchers.IO) {

                    UnknownEcuScanner.scanUnknownEcu(
                        maxCommands = 500
                    )
                }

                sendProgress(
                    progress = 100,
                    status = "شناسایی ECU کامل شد."
                )

                updateNotification(
                    "Unknown ECU",
                    "شناسایی ECU با موفقیت انجام شد."
                )

                /*
                 * نتیجه را به Activityها/Receiverهای احتمالی
                 * اطلاع می‌دهیم.
                 *
                 * خود Result ممکن است data class باشد؛
                 * برای جلوگیری از وابستگی به Serializable،
                 * فعلاً اطلاعات اصلی را از RuntimeState می‌گیریم.
                 */

                val protocol =
                    EcuRuntimeState.detectedProtocol

                val protocolName =
                    EcuRuntimeState.detectedProtocolName

                val resultText =
                    buildResultText(
                        protocol = protocol,
                        protocolName = protocolName,
                        source = source
                    )

                sendBroadcast(
                    Intent(ACTION_RESULT).apply {
                        setPackage(packageName)

                        putExtra(
                            EXTRA_RESULT,
                            resultText
                        )
                    }
                )

                /*
                 * سرویس پس از اتمام کار متوقف می‌شود.
                 */
                stopSelfSafely()

            } catch (e: CancellationException) {

                /*
                 * توقف عادی سرویس.
                 */
                throw e

            } catch (e: Exception) {

                sendError(
                    e.message ?: "خطای ناشناخته در Unknown ECU Scanner"
                )

                stopSelfSafely()
            }
        }
    }

    private fun stopScan() {

        scanJob?.cancel()
        scanJob = null

        try {
            EcuRuntimeState.clear()
        } catch (_: Exception) {
        }
        sendProgress(
            progress = 0,
            status = "اسکن متوقف شد."
        )

        stopSelfSafely()
    }

    private fun sendProgress(
        progress: Int,
        status: String
    ) {

        sendBroadcast(
            Intent(ACTION_PROGRESS).apply {
                setPackage(packageName)

                putExtra(
                    EXTRA_PROGRESS,
                    progress.coerceIn(0, 100)
                )

                putExtra(
                    EXTRA_STATUS,
                    status
                )
            }
        )
    }

    private fun sendError(
        message: String
    ) {

        sendBroadcast(
            Intent(ACTION_ERROR).apply {
                setPackage(packageName)

                putExtra(
                    EXTRA_ERROR,
                    message
                )
            }
        )

        updateNotification(
            "Unknown ECU",
            "خطا: $message"
        )
    }

    private fun buildResultText(
        protocol: String,
        protocolName: String,
        source: ConnectionSource.Source
    ): String {

        return buildString {

            append("Source: ")
            append(source.name)

            append("\nProtocol: ")
            append(
                protocol.ifBlank {
                    "Unknown"
                }
            )

            if (protocolName.isNotBlank()) {
                append("\nProtocol Name: ")
                append(protocolName)
            }
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {

                    description =
                        "ECU discovery and protocol learning"

                    setShowBadge(false)
                }

            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        title: String,
        text: String
    ): Notification {

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.stat_sys_data_bluetooth
            )
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(
                NotificationCompat.CATEGORY_SERVICE
            )
            .build()
    }

    private fun updateNotification(
        title: String,
        text: String
    ) {

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.notify(
            NOTIFICATION_ID,
            buildNotification(
                title = title,
                text = text
            )
        )
    }

    private fun stopSelfSafely() {

        running = false

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }

        stopSelf()
    }

    override fun onDestroy() {

        running = false

        scanJob?.cancel()
        scanJob = null

        serviceScope.cancel()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}