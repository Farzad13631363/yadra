package com.example.yadra

import android.content.Context

object EcuRuntimeState {

    // =====================================================
    // PERSISTENT STORAGE
    // =====================================================

    private const val PREFS_NAME =
        "yadra_ecu_runtime"

    private const val KEY_LAST_UNKNOWN_PROTOCOL =
        "last_unknown_protocol"

    private const val KEY_LAST_UNKNOWN_PROTOCOL_NAME =
        "last_unknown_protocol_name"

    private var appContext: Context? = null

    // =====================================================
    // CURRENT ECU STATE
    // =====================================================

    @Volatile
    var mode: EcuMode = EcuMode.STANDARD
        private set

    @Volatile
    var detectedProtocol: String = ""
        private set

    @Volatile
    var detectedProtocolName: String = ""
        private set

    @Volatile
    var connected: Boolean = false
        private set

    // =====================================================
    // LAST UNKNOWN ECU PROTOCOL
    //
    // این اطلاعات با خروج از Unknown ECU پاک نمی‌شوند.
    // =====================================================

    @Volatile
    var lastUnknownProtocol: String = ""
        private set

    @Volatile
    var lastUnknownProtocolName: String = ""
        private set

    // =====================================================
    // UNKNOWN ECU DETAILS
    // =====================================================

    @Volatile
    var ecuAddress: String = ""
        private set

    @Volatile
    var ecuIdentifier: String = ""
        private set

    @Volatile
    var vin: String = ""
        private set

    @Volatile
    var softwareVersion: String = ""
        private set

    @Volatile
    var hardwareVersion: String = ""
        private set

    @Volatile
    var confidence: Int = 0
        private set

    @Volatile
    var responseIds: List<String> = emptyList()
        private set

    @Volatile
    var successfulCommands: List<String> = emptyList()
        private set

    @Volatile
    var rawResponse: String = ""
        private set

    // =====================================================
    // INITIALIZE
    //
    // یک بار از MainActivity صدا زده شود.
    // =====================================================

    @Synchronized
    fun initialize(
        context: Context
    ) {

        if (appContext != null) {
            return
        }

        appContext =
            context.applicationContext

        val prefs =
            appContext!!.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        lastUnknownProtocol =
            prefs.getString(
                KEY_LAST_UNKNOWN_PROTOCOL,
                ""
            ).orEmpty()

        lastUnknownProtocolName =
            prefs.getString(
                KEY_LAST_UNKNOWN_PROTOCOL_NAME,
                ""
            ).orEmpty()
    }

    // =====================================================
    // STANDARD
    // =====================================================

    @Synchronized
    fun setStandard(
        protocol: String = ""
    ) {

        mode =
            EcuMode.STANDARD

        detectedProtocol =
            protocol.trim()

        detectedProtocolName =
            ""

        connected =
            false

        /*
         * مهم:
         *
         * آخرین Unknown ECU را پاک نمی‌کنیم.
         */
        clearUnknownDetails()
    }

    // =====================================================
    // UNKNOWN
    // =====================================================

    @Synchronized
    fun setUnknown(
        protocol: String,
        protocolName: String = ""
    ) {

        mode =
            EcuMode.UNKNOWN

        detectedProtocol =
            protocol.trim()

        detectedProtocolName =
            protocolName.trim()

        connected =
            false

        /*
         * پروتکل Unknown ECU را ذخیره کن.
         */
        saveLastUnknownProtocol(
            protocol = detectedProtocol,
            protocolName = detectedProtocolName
        )

        clearUnknownDetails()
    }

    // =====================================================
    // UNKNOWN ECU FULL RESULT
    // =====================================================

    @Synchronized
    fun setUnknownDetails(
        ecuAddress: String? = null,
        ecuIdentifier: String? = null,
        vin: String? = null,
        softwareVersion: String? = null,
        hardwareVersion: String? = null,
        confidence: Int = 0,
        responseIds: List<String> = emptyList(),
        successfulCommands: List<String> = emptyList(),
        rawResponse: String = ""
    ) {

        this.ecuAddress =
            ecuAddress.orEmpty().trim()

        this.ecuIdentifier =
            ecuIdentifier.orEmpty().trim()

        this.vin =
            vin.orEmpty().trim()

        this.softwareVersion =
            softwareVersion.orEmpty().trim()

        this.hardwareVersion =
            hardwareVersion.orEmpty().trim()

        this.confidence =
            confidence

        this.responseIds =
            responseIds.toList()

        this.successfulCommands =
            successfulCommands.toList()

        this.rawResponse =
            rawResponse.trim()
    }

    // =====================================================
    // CONNECTION STATE
    // =====================================================

    @Synchronized
    fun setConnected(
        value: Boolean
    ) {

        connected =
            value
    }

    // =====================================================
    // DISCONNECT
    //
    // فقط اتصال قطع می‌شود.
    //
    // هیچ اطلاعاتی درباره ECU پاک نمی‌شود.
    // =====================================================

    @Synchronized
    fun setDisconnected() {

        connected =
            false
    }

    // =====================================================
    // FULL CLEAR
    //
    // وضعیت فعلی پاک می‌شود.
    //
    // ولی آخرین Unknown Protocol باقی می‌ماند.
    // =====================================================

    @Synchronized
    fun clear() {

        mode =
            EcuMode.STANDARD

        detectedProtocol =
            ""

        detectedProtocolName =
            ""

        connected =
            false

        /*
         * عمداً این‌ها را پاک نمی‌کنیم:
         *
         * lastUnknownProtocol
         * lastUnknownProtocolName
         */

        clearUnknownDetails()
    }

    // =====================================================
    // CLEAR UNKNOWN DETAILS
    // =====================================================

    private fun clearUnknownDetails() {

        ecuAddress =
            ""

        ecuIdentifier =
            ""

        vin =
            ""

        softwareVersion =
            ""

        hardwareVersion =
            ""

        confidence =
            0

        responseIds =
            emptyList()

        successfulCommands =
            emptyList()

        rawResponse =
            ""
    }

    // =====================================================
    // SAVE LAST UNKNOWN PROTOCOL
    // =====================================================

    @Synchronized
    private fun saveLastUnknownProtocol(
        protocol: String,
        protocolName: String
    ) {

        if (protocol.isBlank()) {
            return
        }

        lastUnknownProtocol =
            protocol.trim()

        lastUnknownProtocolName =
            protocolName.trim()

        val context =
            appContext
                ?: return

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_LAST_UNKNOWN_PROTOCOL,
                lastUnknownProtocol
            )
            .putString(
                KEY_LAST_UNKNOWN_PROTOCOL_NAME,
                lastUnknownProtocolName
            )
            .apply()
    }

    // =====================================================
    // MANUAL CLEAR
    //
    // فقط اگر خودت بخواهی پروتکل ذخیره‌شده را حذف کنی.
    // =====================================================

    @Synchronized
    fun clearSavedUnknownProtocol() {

        lastUnknownProtocol =
            ""

        lastUnknownProtocolName =
            ""

        val context =
            appContext
                ?: return

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(
                KEY_LAST_UNKNOWN_PROTOCOL
            )
            .remove(
                KEY_LAST_UNKNOWN_PROTOCOL_NAME
            )
            .apply()
    }

    // =====================================================
    // HELPERS
    // =====================================================

    fun isStandard(): Boolean {

        return mode ==
                EcuMode.STANDARD
    }

    fun isUnknown(): Boolean {

        return mode ==
                EcuMode.UNKNOWN
    }
}