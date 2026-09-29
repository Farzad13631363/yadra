package com.example.yadra

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class ActuatorCodeTestActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "yadra_actuator_code_test"
        private const val PREFS_CODES = "code_tests"

        private const val COMMAND_2101 = "2101"
        private const val DATA_LENGTH = 90

        private const val UNKNOWN = "UNKNOWN"
        private const val CONFIRMED = "CONFIRMED"
        private const val REJECTED = "REJECTED"

        private const val TEST_DELAY = 700L
    }

    data class CodeTest(
        var id: Long,
        var sensorName: String,
        var command: String,
        var service: String,
        var localId: String,
        var offset: Int,
        var length: Int,
        var rawValue: Int?,
        var calculatedValue: Double?,
        var unit: String,
        var formula: String,
        var response: String,
        var status: String,
        var note: String,
        var testCount: Int,
        var minValue: Int?,
        var maxValue: Int?
    )

    private lateinit var txtCodeConnection: TextView
    private lateinit var txtCodeResult: TextView
    private lateinit var txtSavedCount: TextView
    private lateinit var txtCodeTestStatus: TextView

    private lateinit var edtCodeName: EditText
    private lateinit var edtCodeCommand: EditText
    private lateinit var edtCodeService: EditText
    private lateinit var edtCodeLocalId: EditText
    private lateinit var edtCodeOffset: EditText
    private lateinit var edtCodeLength: EditText
    private lateinit var edtCodeUnit: EditText
    private lateinit var edtCodeFormula: EditText
    private lateinit var edtCodeExpected: EditText
    private lateinit var edtCodeNote: EditText

    private lateinit var btnRunCode: Button
    private lateinit var btnClearCode: Button
    private lateinit var btnSaveCode: Button
    private lateinit var btnTestNextCode: Button
    private lateinit var btnRetestRejectedCode: Button
    private lateinit var btnAnalyzeCodes: Button
    private lateinit var btnClearCodeDatabase: Button

    private lateinit var savedCodeContainer: LinearLayout

    private val handler = Handler(Looper.getMainLooper())

    private val codes = ArrayList<CodeTest>()

    private var testing = false
    private var last2101Data: ByteArray? = null
    private var last2101Hex = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_actuator_code_test)

        bindViews()
        loadCodes()
        setupButtons()

        updateConnectionStatus()
        refreshScreen()
    }

    override fun onDestroy() {
        testing = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun bindViews() {

        txtCodeConnection = findViewById(R.id.txtCodeConnection)
        txtCodeResult = findViewById(R.id.txtCodeResult)
        txtSavedCount = findViewById(R.id.txtSavedCount)
        txtCodeTestStatus = findViewById(R.id.txtCodeTestStatus)

        edtCodeName = findViewById(R.id.edtCodeName)
        edtCodeCommand = findViewById(R.id.edtCodeCommand)
        edtCodeService = findViewById(R.id.edtCodeService)
        edtCodeLocalId = findViewById(R.id.edtCodeLocalId)
        edtCodeOffset = findViewById(R.id.edtCodeOffset)
        edtCodeLength = findViewById(R.id.edtCodeLength)
        edtCodeUnit = findViewById(R.id.edtCodeUnit)
        edtCodeFormula = findViewById(R.id.edtCodeFormula)
        edtCodeExpected = findViewById(R.id.edtCodeExpected)
        edtCodeNote = findViewById(R.id.edtCodeNote)

        btnRunCode = findViewById(R.id.btnRunCode)
        btnClearCode = findViewById(R.id.btnClearCode)
        btnSaveCode = findViewById(R.id.btnSaveCode)
        btnTestNextCode = findViewById(R.id.btnTestNextCode)
        btnRetestRejectedCode = findViewById(R.id.btnRetestRejectedCode)
        btnAnalyzeCodes = findViewById(R.id.btnAnalyzeCodes)
        btnClearCodeDatabase = findViewById(R.id.btnClearCodeDatabase)

        savedCodeContainer =
            findViewById(R.id.savedCodeContainer)
    }

    private fun setupButtons() {

        btnRunCode.setOnClickListener {
            runSelectedCommand()
        }

        btnClearCode.setOnClickListener {
            clearForm()
        }

        btnSaveCode.setOnClickListener {
            saveManualSensor()
        }

        btnTestNextCode.setOnClickListener {
            testNextCandidate()
        }

        btnRetestRejectedCode.setOnClickListener {
            restoreRejectedCandidates()
        }

        btnAnalyzeCodes.setOnClickListener {
            analyzeCandidates()
        }

        btnClearCodeDatabase.setOnClickListener {
            clearDatabaseDialog()
        }
    }

    /*
     * =========================================================
     * CONNECTION
     * =========================================================
     */

    private fun updateConnectionStatus() {

        val connected = try {

            YadraConnectionManager.elmConnected ||
                    BluetoothConnectionManager.elmConnected

        } catch (_: Exception) {
            false
        }

        txtCodeConnection.text =
            if (connected) {
                "ECU CONNECTION: CONNECTED"
            } else {
                "ECU CONNECTION: DISCONNECTED"
            }
    }

    /*
     * =========================================================
     * COMMAND
     * =========================================================
     */

    private fun runSelectedCommand() {

        val command =
            edtCodeCommand.text
                .toString()
                .trim()
                .ifEmpty {
                    COMMAND_2101
                }

        sendCommandAndDisplay(command)
    }

    private fun sendCommandAndDisplay(command: String) {

        txtCodeTestStatus.text =
            "در حال ارسال $command ..."

        Thread {

            val response =
                try {
                    sendCommand(command)
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }

            runOnUiThread {

                txtCodeResult.text = response

                if (command.equals(
                        COMMAND_2101,
                        true
                    )
                ) {

                    val data =
                        parse2101(response)

                    if (data != null) {

                        last2101Data = data
                        last2101Hex = bytesToHex(data)

                        updateCandidates(data)

                        txtCodeTestStatus.text =
                            "2101 دریافت شد - ${data.size} بایت"

                        refreshScreen()

                    } else {

                        txtCodeTestStatus.text =
                            "داده 2101 پیدا نشد."
                    }

                } else {

                    txtCodeTestStatus.text =
                        "پاسخ دریافت شد."
                }
            }

        }.start()
    }

    private fun sendCommand(command: String): String {

        return try {

            when {
                YadraConnectionManager.elmConnected -> {
                    YadraConnectionManager.sendCommand(command)
                }

                BluetoothConnectionManager.elmConnected -> {
                    BluetoothConnectionManager
                        .sendActiveCommand(command)
                }

                else -> {
                    "ELM327 NOT CONNECTED"
                }
            }

        } catch (e: Exception) {

            "ERROR: ${e.message}"
        }
    }

    /*
     * =========================================================
     * 2101 PARSER
     * =========================================================
     */

    private fun parse2101(response: String): ByteArray? {

        val tokens =
            response
                .replace("\r", " ")
                .replace("\n", " ")
                .replace(">", " ")
                .replace(":", " ")
                .split(Regex("\\s+"))
                .mapNotNull { token ->

                    val value =
                        token
                            .trim()
                            .removePrefix("0x")
                            .removePrefix("0X")

                    if (
                        value.length == 2 &&
                        value.matches(
                            Regex("[0-9A-Fa-f]{2}")
                        )
                    ) {
                        value.toIntOrNull(16)
                    } else {
                        null
                    }
                }

        if (tokens.size < 3) {
            return null
        }

        /*
         * Search for:
         *
         * 61 01 + 90 bytes
         */

        for (i in 0 until tokens.size - 91) {

            if (
                tokens[i] == 0x61 &&
                tokens[i + 1] == 0x01
            ) {

                val data =
                    ByteArray(DATA_LENGTH)

                for (j in 0 until DATA_LENGTH) {
                    data[j] =
                        tokens[i + 2 + j].toByte()
                }

                return data
            }
        }

        /*
         * If exactly 90 bytes arrived,
         * use them directly.
         */

        if (tokens.size == DATA_LENGTH) {

            return ByteArray(DATA_LENGTH) { index ->
                tokens[index].toByte()
            }
        }

        /*
         * Try last 90 bytes.
         */

        if (tokens.size > DATA_LENGTH) {

            val start =
                tokens.size - DATA_LENGTH

            return ByteArray(DATA_LENGTH) { index ->
                tokens[start + index].toByte()
            }
        }

        return null
    }

    /*
     * =========================================================
     * IMPORT
     * =========================================================
     */

    private fun import2101() {

        txtCodeTestStatus.text =
            "در حال دریافت 2101 ..."

        Thread {

            val response =
                try {
                    sendCommand(COMMAND_2101)
                } catch (e: Exception) {
                    "ERROR: ${e.message}"
                }

            val data =
                parse2101(response)

            runOnUiThread {

                if (data == null) {

                    txtCodeResult.text = response

                    txtCodeTestStatus.text =
                        "پاسخ 2101 قابل شناسایی نیست."

                    return@runOnUiThread
                }

                last2101Data = data
                last2101Hex = bytesToHex(data)

                createCandidates()

                updateCandidates(data)

                txtCodeResult.text =
                    "2101 DATA\n$last2101Hex"

                txtCodeTestStatus.text =
                    "کاندیدها آماده شدند."

                refreshScreen()
            }

        }.start()
    }

    /*
     * =========================================================
     * CREATE 90 CANDIDATES
     * =========================================================
     */

    private fun createCandidates() {

        val existing =
            codes
                .filter {
                    it.command == COMMAND_2101
                }
                .map {
                    it.offset
                }
                .toHashSet()

        for (offset in 0 until DATA_LENGTH) {

            if (existing.contains(offset)) {
                continue
            }

            codes.add(
                CodeTest(
                    id =
                        System.currentTimeMillis() +
                                offset,

                    sensorName =
                        "Sensor candidate ${offset + 1}",

                    command =
                        COMMAND_2101,

                    service =
                        "21",

                    localId =
                        "01",

                    offset =
                        offset,

                    length =
                        1,

                    rawValue =
                        null,

                    calculatedValue =
                        null,

                    unit =
                        "",

                    formula =
                        "x",

                    response =
                        "",

                    status =
                        UNKNOWN,

                    note =
                        "2101 Offset $offset",

                    testCount =
                        0,

                    minValue =
                        null,

                    maxValue =
                        null
                )
            )
        }

        saveCodes()
    }

    /*
     * =========================================================
     * UPDATE VALUES
     * =========================================================
     */

    private fun updateCandidates(
        data: ByteArray
    ) {

        for (candidate in codes) {

            if (candidate.status != UNKNOWN) {
                continue
            }

            val offset =
                candidate.offset

            if (
                offset < 0 ||
                offset >= data.size
            ) {
                continue
            }

            val raw =
                readValue(
                    data,
                    offset,
                    candidate.length
                )

            candidate.rawValue = raw

            candidate.calculatedValue =
                calculate(
                    raw.toDouble(),
                    candidate.formula
                )

            candidate.response =
                bytesToHex(data)

            candidate.testCount++

            if (
                candidate.minValue == null ||
                raw < candidate.minValue!!
            ) {
                candidate.minValue = raw
            }

            if (
                candidate.maxValue == null ||
                raw > candidate.maxValue!!
            ) {
                candidate.maxValue = raw
            }
        }

        saveCodes()
    }

    private fun readValue(
        data: ByteArray,
        offset: Int,
        length: Int
    ): Int {

        if (offset !in data.indices) {
            return 0
        }

        if (length <= 1) {
            return data[offset].toInt() and 0xFF
        }

        if (
            offset + length >
            data.size
        ) {
            return data[offset].toInt() and 0xFF
        }

        var result = 0

        for (i in 0 until length) {

            result =
                (result shl 8) or
                        (
                                data[offset + i]
                                    .toInt() and 0xFF
                                )
        }

        return result
    }

    /*
     * =========================================================
     * FORMULA
     * =========================================================
     */

    private fun calculate(
        value: Double,
        formula: String
    ): Double {

        val f =
            formula
                .trim()
                .lowercase(Locale.US)

        if (
            f.isEmpty() ||
            f == "x"
        ) {
            return value
        }

        return try {

            when {

                f == "x*2" ->
                    value * 2.0

                f == "x/2" ->
                    value / 2.0

                f == "x/10" ->
                    value / 10.0

                f == "x/100" ->
                    value / 100.0

                f == "x*10" ->
                    value * 10.0

                f == "x+40" ->
                    value + 40.0

                f == "x-40" ->
                    value - 40.0

                else ->
                    value
            }

        } catch (_: Exception) {
            value
        }
    }

    /*
     * =========================================================
     * TEST NEXT
     * =========================================================
     */

    private fun testNextCandidate() {

        val candidate =
            codes.firstOrNull {
                it.status == UNKNOWN
            }

        if (candidate == null) {

            txtCodeTestStatus.text =
                "کاندید فعالی باقی نمانده است."

            return
        }

        fillForm(candidate)

        txtCodeTestStatus.text =
            "در حال تست Offset ${candidate.offset}"

        startAutomatic2101()
    }

    private fun fillForm(
        candidate: CodeTest
    ) {

        edtCodeName.setText(
            candidate.sensorName
        )

        edtCodeCommand.setText(
            candidate.command
        )

        edtCodeService.setText(
            candidate.service
        )

        edtCodeLocalId.setText(
            candidate.localId
        )

        edtCodeOffset.setText(
            candidate.offset.toString()
        )

        edtCodeLength.setText(
            candidate.length.toString()
        )

        edtCodeUnit.setText(
            candidate.unit
        )

        edtCodeFormula.setText(
            candidate.formula
        )

        edtCodeNote.setText(
            candidate.note
        )
    }

    /*
     * =========================================================
     * AUTOMATIC 2101 LOOP
     * =========================================================
     */

    private fun startAutomatic2101() {

        if (testing) {
            return
        }

        testing = true

        val runnable =
            object : Runnable {

                override fun run() {

                    if (!testing) {
                        return
                    }

                    Thread {

                        val response =
                            try {
                                sendCommand(COMMAND_2101)
                            } catch (e: Exception) {
                                "ERROR: ${e.message}"
                            }

                        val data =
                            parse2101(response)

                        runOnUiThread {

                            if (data != null) {

                                last2101Data = data
                                last2101Hex =
                                    bytesToHex(data)

                                updateCandidates(data)

                                txtCodeResult.text =
                                    last2101Hex

                                refreshScreen()
                            }

                            if (testing) {

                                handler.postDelayed(
                                    this,
                                    TEST_DELAY
                                )
                            }
                        }

                    }.start()
                }
            }

        handler.post(runnable)
    }

    /*
     * =========================================================
     * CONFIRM
     * =========================================================
     */

    private fun confirmCandidate(
        candidate: CodeTest
    ) {

        testing = false

        val input =
            EditText(this)

        input.hint =
            "نام سنسور"

        input.setText(
            candidate.sensorName
        )

        val layout =
            LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            40,
            10,
            40,
            10
        )

        layout.addView(
            input,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        AlertDialog.Builder(this)
            .setTitle("تأیید سنسور")
            .setMessage(
                "Offset ${candidate.offset}\n" +
                        "Raw: ${candidate.rawValue ?: 0}\n" +
                        "Value: ${formatValue(candidate.calculatedValue)}"
            )
            .setView(layout)
            .setNegativeButton(
                "انصراف",
                null
            )
            .setPositiveButton(
                "✓ تأیید"
            ) { _, _ ->

                val sensorName =
                    input.text
                        .toString()
                        .trim()

                if (sensorName.isNotEmpty()) {
                    candidate.sensorName =
                        sensorName
                }

                candidate.status =
                    CONFIRMED

                candidate.note =
                    candidate.note +
                            "\nCONFIRMED BY USER"

                saveCodes()
                refreshScreen()

                Toast.makeText(
                    this,
                    "سنسور ثبت شد",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    /*
     * =========================================================
     * REJECT
     * =========================================================
     */

    private fun rejectCandidate(
        candidate: CodeTest
    ) {

        candidate.status =
            REJECTED

        candidate.note =
            candidate.note +
                    "\nREJECTED"

        saveCodes()
        refreshScreen()
    }

    /*
     * =========================================================
     * RESTORE REJECTED
     * =========================================================
     */

    private fun restoreRejectedCandidates() {

        var count = 0

        for (candidate in codes) {

            if (
                candidate.status ==
                REJECTED
            ) {

                candidate.status =
                    UNKNOWN

                count++
            }
        }

        saveCodes()
        refreshScreen()

        txtCodeTestStatus.text =
            "$count کاندید به چرخه تست برگشت."
    }

    /*
     * =========================================================
     * ANALYSIS
     * =========================================================
     */

    private fun analyzeCandidates() {

        val active =
            codes.filter {
                it.status == UNKNOWN
            }

        if (active.isEmpty()) {

            txtCodeResult.text =
                "کاندید فعالی وجود ندارد."

            return
        }

        val changing =
            active
                .filter {
                    it.minValue != null &&
                            it.maxValue != null &&
                            it.minValue != it.maxValue
                }
                .sortedByDescending {
                    it.maxValue!! -
                            it.minValue!!
                }

        val result =
            StringBuilder()

        result.append(
            "YADRA 2101 ANALYSIS\n"
        )

        result.append(
            "============================\n"
        )

        result.append(
            "ACTIVE: ${active.size}\n"
        )

        result.append(
            "CONFIRMED: ${
                codes.count {
                    it.status == CONFIRMED
                }
            }\n"
        )

        result.append(
            "REJECTED: ${
                codes.count {
                    it.status == REJECTED
                }
            }\n\n"
        )

        result.append(
            "CHANGING OFFSETS\n"
        )

        result.append(
            "----------------------------\n"
        )

        for (candidate in changing) {

            val min =
                candidate.minValue ?: 0

            val max =
                candidate.maxValue ?: 0

            val range =
                max - min

            result.append(
                "Offset ${candidate.offset} | " +
                        "Raw ${candidate.rawValue ?: 0} | " +
                        "Range $range | " +
                        "Tests ${candidate.testCount}\n"
            )
        }

        txtCodeResult.text =
            result.toString()

        txtCodeTestStatus.text =
            "تحلیل انجام شد."
    }

    /*
     * =========================================================
     * SCREEN
     * =========================================================
     */

    private fun refreshScreen() {

        updateConnectionStatus()

        val active =
            codes.count {
                it.status == UNKNOWN
            }

        val confirmed =
            codes.count {
                it.status == CONFIRMED
            }

        val rejected =
            codes.count {
                it.status == REJECTED
            }

        txtSavedCount.text =
            "ACTIVE: $active | CONFIRMED: $confirmed | REJECTED: $rejected"

        renderCodes()
    }

    private fun renderCodes() {

        savedCodeContainer.removeAllViews()

        val importButton =
            Button(this)

        importButton.text =
            "دریافت کاندیدهای 2101"

        importButton.setOnClickListener {
            import2101()
        }

        savedCodeContainer.addView(
            importButton
        )

        addSectionTitle(
            "🔎 کاندیدهای فعال"
        )

        val active =
            codes.filter {
                it.status == UNKNOWN
            }

        if (active.isEmpty()) {

            addText(
                "✓ کاندید فعالی وجود ندارد."
            )

        } else {

            for (candidate in active) {

                savedCodeContainer.addView(
                    createActiveCard(
                        candidate
                    )
                )
            }
        }

        addSectionTitle(
            "✓ سنسورهای تأییدشده"
        )

        val confirmed =
            codes.filter {
                it.status == CONFIRMED
            }

        if (confirmed.isEmpty()) {

            addText(
                "هنوز سنسوری تأیید نشده است."
            )

        } else {

            for (candidate in confirmed) {

                savedCodeContainer.addView(
                    createConfirmedCard(
                        candidate
                    )
                )
            }
        }

        addSectionTitle(
            "✗ کاندیدهای ردشده"
        )

        val rejected =
            codes.filter {
                it.status == REJECTED
            }

        if (rejected.isEmpty()) {

            addText(
                "کاندید ردشده‌ای وجود ندارد."
            )

        } else {

            for (candidate in rejected) {

                savedCodeContainer.addView(
                    createRejectedCard(
                        candidate
                    )
                )
            }
        }
    }

    private fun addSectionTitle(
        titleText: String
    ) {

        val view =
            TextView(this)

        view.text =
            titleText

        view.textSize =
            18f

        view.setPadding(
            16,
            24,
            16,
            12
        )

        savedCodeContainer.addView(
            view
        )
    }

    private fun addText(
        value: String
    ) {

        val view =
            TextView(this)

        view.text =
            value

        view.textSize =
            14f

        view.setPadding(
            20,
            12,
            20,
            12
        )

        savedCodeContainer.addView(
            view
        )
    }

    /*
     * =========================================================
     * ACTIVE CARD
     * =========================================================
     */

    private fun createActiveCard(
        candidate: CodeTest
    ): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            18,
            14,
            18,
            14
        )

        val title =
            TextView(this)

        title.text =
            candidate.sensorName

        title.textSize =
            17f

        card.addView(
            title
        )

        val raw =
            candidate.rawValue
                ?.toString()
                ?: "--"

        val value =
            formatValue(
                candidate.calculatedValue
            )

        val min =
            candidate.minValue
                ?.toString()
                ?: "--"

        val max =
            candidate.maxValue
                ?.toString()
                ?: "--"

        val information =
            TextView(this)

        information.text =
            "Offset: ${candidate.offset}\n" +
                    "Raw: $raw\n" +
                    "Value: $value ${candidate.unit}\n" +
                    "Range: $min → $max\n" +
                    "Tests: ${candidate.testCount}"

        information.textSize =
            14f

        information.setPadding(
            0,
            10,
            0,
            10
        )

        card.addView(
            information
        )

        val buttons =
            LinearLayout(this)

        buttons.orientation =
            LinearLayout.HORIZONTAL

        buttons.gravity =
            Gravity.CENTER

        val confirm =
            Button(this)

        confirm.text =
            "✓ تأیید"

        confirm.setOnClickListener {
            confirmCandidate(candidate)
        }

        val reject =
            Button(this)

        reject.text =
            "✗ رد"

        reject.setOnClickListener {
            rejectCandidate(candidate)
        }

        buttons.addView(
            confirm,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        buttons.addView(
            reject,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        card.addView(
            buttons
        )

        card.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    8,
                    5,
                    8,
                    5
                )
            }

        return card
    }

    /*
     * =========================================================
     * CONFIRMED CARD
     * =========================================================
     */

    private fun createConfirmedCard(
        candidate: CodeTest
    ): LinearLayout {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            18,
            14,
            18,
            14
        )

        val title =
            TextView(this)

        title.text =
            "✓ ${candidate.sensorName}"

        title.textSize =
            18f

        card.addView(
            title
        )

        val value =
            TextView(this)

        value.text =
            "Command: ${candidate.command}\n" +
                    "Service: ${candidate.service}\n" +
                    "Local ID: ${candidate.localId}\n" +
                    "Offset: ${candidate.offset}\n" +
                    "Length: ${candidate.length}\n" +
                    "Raw: ${candidate.rawValue ?: "--"}\n" +
                    "Value: ${formatValue(candidate.calculatedValue)} ${candidate.unit}\n" +
                    "Formula: ${candidate.formula}"

        value.textSize =
            14f

        value.setPadding(
            0,
            10,
            0,
            10
        )

        card.addView(
            value
        )

        val delete =
            Button(this)

        delete.text =
            "حذف سنسور ثبت‌شده"

        delete.setOnClickListener {

            codes.remove(candidate)

            saveCodes()
            refreshScreen()
        }

        card.addView(
            delete
        )

        card.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    8,
                    5,
                    8,
                    5
                )
            }

        return card
    }

    /*
     * =========================================================
     * REJECTED CARD
     * =========================================================
     */

    private fun createRejectedCard(
        candidate: CodeTest
    ): LinearLayout {

        val row =
            LinearLayout(this)

        row.orientation =
            LinearLayout.HORIZONTAL

        row.gravity =
            Gravity.CENTER_VERTICAL

        val text =
            TextView(this)

        text.text =
            "Offset ${candidate.offset} | " +
                    "Raw ${candidate.rawValue ?: "--"}"

        text.textSize =
            14f

        val restore =
            Button(this)

        restore.text =
            "↻ تست دوباره"

        restore.setOnClickListener {

            candidate.status =
                UNKNOWN

            saveCodes()
            refreshScreen()
        }

        row.addView(
            text,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        row.addView(
            restore
        )

        return row
    }

    /*
     * =========================================================
     * MANUAL SAVE
     * =========================================================
     */

    private fun saveManualSensor() {

        val name =
            edtCodeName.text
                .toString()
                .trim()

        if (name.isEmpty()) {

            Toast.makeText(
                this,
                "نام سنسور را وارد کنید.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val command =
            edtCodeCommand.text
                .toString()
                .trim()
                .ifEmpty {
                    COMMAND_2101
                }

        val service =
            edtCodeService.text
                .toString()
                .trim()
                .ifEmpty {
                    "21"
                }

        val localId =
            edtCodeLocalId.text
                .toString()
                .trim()
                .ifEmpty {
                    "01"
                }

        val offset =
            edtCodeOffset.text
                .toString()
                .toIntOrNull()
                ?: 0

        val length =
            edtCodeLength.text
                .toString()
                .toIntOrNull()
                ?: 1

        val unit =
            edtCodeUnit.text
                .toString()
                .trim()

        val formula =
            edtCodeFormula.text
                .toString()
                .trim()
                .ifEmpty {
                    "x"
                }

        val note =
            edtCodeNote.text
                .toString()
                .trim()

        val existing =
            codes.firstOrNull {
                it.command.equals(
                    command,
                    true
                ) &&
                        it.offset == offset
            }

        if (existing != null) {

            existing.sensorName =
                name

            existing.service =
                service

            existing.localId =
                localId

            existing.length =
                length

            existing.unit =
                unit

            existing.formula =
                formula

            existing.note =
                note

            existing.status =
                CONFIRMED

        } else {

            codes.add(
                CodeTest(
                    id =
                        System.currentTimeMillis(),

                    sensorName =
                        name,

                    command =
                        command,

                    service =
                        service,

                    localId =
                        localId,

                    offset =
                        offset,

                    length =
                        length,

                    rawValue =
                        null,

                    calculatedValue =
                        null,

                    unit =
                        unit,

                    formula =
                        formula,

                    response =
                        "",

                    status =
                        CONFIRMED,

                    note =
                        note,

                    testCount =
                        0,

                    minValue =
                        null,

                    maxValue =
                        null
                )
            )
        }

        saveCodes()
        refreshScreen()

        Toast.makeText(
            this,
            "سنسور ذخیره شد.",
            Toast.LENGTH_SHORT
        ).show()
    }

    /*
     * =========================================================
     * CLEAR FORM
     * =========================================================
     */

    private fun clearForm() {

        edtCodeName.setText("")
        edtCodeCommand.setText(COMMAND_2101)
        edtCodeService.setText("21")
        edtCodeLocalId.setText("01")
        edtCodeOffset.setText("0")
        edtCodeLength.setText("1")
        edtCodeUnit.setText("")
        edtCodeFormula.setText("x")
        edtCodeExpected.setText("")
        edtCodeNote.setText("")

        txtCodeResult.text = ""
        txtCodeTestStatus.text =
            "فرم پاک شد."
    }

    /*
     * =========================================================
     * DATABASE SAVE
     * =========================================================
     */

    private fun saveCodes() {

        try {

            val array =
                JSONArray()

            for (candidate in codes) {

                val obj =
                    JSONObject()

                obj.put(
                    "id",
                    candidate.id
                )

                obj.put(
                    "sensorName",
                    candidate.sensorName
                )

                obj.put(
                    "command",
                    candidate.command
                )

                obj.put(
                    "service",
                    candidate.service
                )

                obj.put(
                    "localId",
                    candidate.localId
                )

                obj.put(
                    "offset",
                    candidate.offset
                )

                obj.put(
                    "length",
                    candidate.length
                )

                obj.put(
                    "rawValue",
                    candidate.rawValue
                        ?: JSONObject.NULL
                )

                obj.put(
                    "calculatedValue",
                    candidate.calculatedValue
                        ?: JSONObject.NULL
                )

                obj.put(
                    "unit",
                    candidate.unit
                )

                obj.put(
                    "formula",
                    candidate.formula
                )

                obj.put(
                    "response",
                    candidate.response
                )

                obj.put(
                    "status",
                    candidate.status
                )

                obj.put(
                    "note",
                    candidate.note
                )

                obj.put(
                    "testCount",
                    candidate.testCount
                )

                obj.put(
                    "minValue",
                    candidate.minValue
                        ?: JSONObject.NULL
                )

                obj.put(
                    "maxValue",
                    candidate.maxValue
                        ?: JSONObject.NULL
                )

                array.put(obj)
            }

            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )
                .edit()
                .putString(
                    PREFS_CODES,
                    array.toString()
                )
                .apply()

        } catch (_: Exception) {
        }
    }

    /*
     * =========================================================
     * DATABASE LOAD
     * =========================================================
     */

    private fun loadCodes() {

        codes.clear()

        try {

            val json =
                getSharedPreferences(
                    PREFS_NAME,
                    MODE_PRIVATE
                )
                    .getString(
                        PREFS_CODES,
                        null
                    )
                    ?: return

            val array =
                JSONArray(json)

            for (i in 0 until array.length()) {

                val obj =
                    array.getJSONObject(i)

                val rawValue =
                    if (
                        obj.isNull(
                            "rawValue"
                        )
                    ) {
                        null
                    } else {
                        obj.optInt(
                            "rawValue"
                        )
                    }

                val calculated =
                    if (
                        obj.isNull(
                            "calculatedValue"
                        )
                    ) {
                        null
                    } else {
                        obj.optDouble(
                            "calculatedValue"
                        )
                    }

                val min =
                    if (
                        obj.isNull(
                            "minValue"
                        )
                    ) {
                        null
                    } else {
                        obj.optInt(
                            "minValue"
                        )
                    }

                val max =
                    if (
                        obj.isNull(
                            "maxValue"
                        )
                    ) {
                        null
                    } else {
                        obj.optInt(
                            "maxValue"
                        )
                    }

                codes.add(
                    CodeTest(
                        id =
                            obj.optLong(
                                "id"
                            ),

                        sensorName =
                            obj.optString(
                                "sensorName",
                                "Sensor candidate"
                            ),

                        command =
                            obj.optString(
                                "command",
                                COMMAND_2101
                            ),

                        service =
                            obj.optString(
                                "service",
                                "21"
                            ),

                        localId =
                            obj.optString(
                                "localId",
                                "01"
                            ),

                        offset =
                            obj.optInt(
                                "offset",
                                0
                            ),

                        length =
                            obj.optInt(
                                "length",
                                1
                            ),

                        rawValue =
                            rawValue,

                        calculatedValue =
                            calculated,

                        unit =
                            obj.optString(
                                "unit",
                                ""
                            ),

                        formula =
                            obj.optString(
                                "formula",
                                "x"
                            ),

                        response =
                            obj.optString(
                                "response",
                                ""
                            ),

                        status =
                            obj.optString(
                                "status",
                                UNKNOWN
                            ),

                        note =
                            obj.optString(
                                "note",
                                ""
                            ),

                        testCount =
                            obj.optInt(
                                "testCount",
                                0
                            ),

                        minValue =
                            min,

                        maxValue =
                            max
                    )
                )
            }

        } catch (_: Exception) {
        }
    }

    /*
     * =========================================================
     * CLEAR DATABASE
     * =========================================================
     */

    private fun clearDatabaseDialog() {

        AlertDialog.Builder(this)
            .setTitle("پاک کردن بانک کدها")
            .setMessage(
                "همه کاندیدها و سنسورهای ثبت‌شده حذف شوند؟"
            )
            .setNegativeButton(
                "انصراف",
                null
            )
            .setPositiveButton(
                "حذف"
            ) { _, _ ->

                testing = false

                codes.clear()

                getSharedPreferences(
                    PREFS_NAME,
                    MODE_PRIVATE
                )
                    .edit()
                    .remove(PREFS_CODES)
                    .apply()

                last2101Data = null
                last2101Hex = ""

                txtCodeResult.text = ""

                txtCodeTestStatus.text =
                    "بانک کدها پاک شد."

                refreshScreen()
            }
            .show()
    }

    /*
     * =========================================================
     * HELPERS
     * =========================================================
     */

    private fun formatValue(
        value: Double?
    ): String {

        if (value == null) {
            return "--"
        }

        return if (
            abs(
                value -
                        value.roundToInt()
            ) < 0.000001
        ) {

            value
                .roundToInt()
                .toString()

        } else {

            String.format(
                Locale.US,
                "%.2f",
                value
            )
        }
    }

    private fun bytesToHex(
        data: ByteArray
    ): String {

        return data.joinToString(" ") { byte ->

            String.format(
                Locale.US,
                "%02X",
                byte.toInt() and 0xFF
            )
        }
    }
}