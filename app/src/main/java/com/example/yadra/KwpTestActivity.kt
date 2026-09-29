package com.example.yadra

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.graphics.Color
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

class KwpTestActivity : AppCompatActivity() {

    companion object {

        private const val PREFS_NAME =
            "yadra_kwp_test_2101"

        private const val KEY_SAMPLES =
            "samples"

        private const val KEY_RPM_SAMPLES =
            "rpm_samples"

        private const val KWP_POST_PROMPT_QUIET_MS =
            500L

        private const val SAVED_PROTOCOL =
            "A5"

        private const val ECU_ADDRESS =
            "11"
    }

    /*
     * ============================================================
     * DATA CLASSES
     * ============================================================
     */

    data class Kwp2101Sample(
        val label: String,
        val data: ByteArray
    )

    private data class SavedRpmSample(
        val rpm: Int,
        val sample: Kwp2101Sample
    )

    data class Kwp2101Field(
        val offset: Int,
        val hex: String,
        val decimal: Int,
        val u16Be: Int?,
        val u16Le: Int?
    )

    /*
     * ============================================================
     * SIMPLE 2101 ANALYZER
     * ============================================================
     */

    object Kwp2101Analyzer {

        fun buildFields(
            data: ByteArray
        ): List<Kwp2101Field> {

            val result =
                mutableListOf<Kwp2101Field>()

            for (i in data.indices) {

                val value =
                    data[i].toInt() and 0xFF

                val be =
                    if (i + 1 < data.size) {
                        (value shl 8) or
                                (data[i + 1].toInt() and 0xFF)
                    } else {
                        null
                    }

                val le =
                    if (i + 1 < data.size) {
                        ((data[i + 1].toInt() and 0xFF) shl 8) or
                                value
                    } else {
                        null
                    }

                result.add(
                    Kwp2101Field(
                        offset = i,
                        hex = String.format(
                            Locale.US,
                            "%02X",
                            value
                        ),
                        decimal = value,
                        u16Be = be,
                        u16Le = le
                    )
                )
            }

            return result
        }

        fun createTable(
            sample: Kwp2101Sample
        ): String {

            val sb =
                StringBuilder()

            sb.appendLine()
            sb.appendLine(
                "=============================="
            )
            sb.appendLine(
                "2101 SAMPLE: ${sample.label}"
            )
            sb.appendLine(
                "SIZE: ${sample.data.size} bytes"
            )
            sb.appendLine(
                "=============================="
            )

            for (field in buildFields(sample.data)) {

                sb.append(
                    String.format(
                        Locale.US,
                        "%02d  HEX=%s  U8=%3d",
                        field.offset,
                        field.hex,
                        field.decimal
                    )
                )

                if (field.u16Be != null) {

                    sb.append(
                        String.format(
                            Locale.US,
                            "  BE=%5d",
                            field.u16Be
                        )
                    )
                }

                if (field.u16Le != null) {

                    sb.append(
                        String.format(
                            Locale.US,
                            "  LE=%5d",
                            field.u16Le
                        )
                    )
                }

                sb.appendLine()
            }

            return sb.toString()
        }

        fun findChangingOffsets(
            samples: List<Kwp2101Sample>
        ): List<Int> {

            if (samples.size < 2) {
                return emptyList()
            }

            val maxSize =
                samples.minOfOrNull {
                    it.data.size
                } ?: return emptyList()

            val result =
                mutableListOf<Int>()

            for (offset in 0 until maxSize) {

                val first =
                    samples[0].data[offset].toInt() and 0xFF

                var changed =
                    false

                for (i in 1 until samples.size) {

                    val value =
                        samples[i].data[offset].toInt() and 0xFF

                    if (value != first) {
                        changed = true
                        break
                    }
                }

                if (changed) {
                    result.add(offset)
                }
            }

            return result
        }

        fun findStableOffsets(
            samples: List<Kwp2101Sample>
        ): List<Int> {

            if (samples.isEmpty()) {
                return emptyList()
            }

            val maxSize =
                samples.minOfOrNull {
                    it.data.size
                } ?: return emptyList()

            val result =
                mutableListOf<Int>()

            for (offset in 0 until maxSize) {

                val first =
                    samples[0].data[offset].toInt() and 0xFF

                var stable =
                    true

                for (i in 1 until samples.size) {

                    val value =
                        samples[i].data[offset].toInt() and 0xFF

                    if (value != first) {
                        stable = false
                        break
                    }
                }

                if (stable) {
                    result.add(offset)
                }
            }

            return result
        }

        private fun monotonicType(
            values: List<Int>
        ): String? {

            if (values.size < 3) {
                return null
            }

            var increasing = true
            var decreasing = true

            for (i in 1 until values.size) {

                if (values[i] < values[i - 1]) {
                    increasing = false
                }

                if (values[i] > values[i - 1]) {
                    decreasing = false
                }
            }

            return when {
                increasing -> "INCREASING"
                decreasing -> "DECREASING"
                else -> null
            }
        }

        fun findRpmCorrelations(
            samples: List<Pair<Int, Kwp2101Sample>>
        ): String {

            if (samples.size < 2) {
                return "Not enough RPM samples."
            }

            val sb =
                StringBuilder()

            sb.appendLine()
            sb.appendLine(
                "========== RPM U8 ANALYSIS =========="
            )

            val maxSize =
                samples.minOfOrNull {
                    it.second.data.size
                } ?: 0

            for (offset in 0 until maxSize) {

                val values =
                    samples.map {
                        it.second.data[offset]
                            .toInt() and 0xFF
                    }

                val rpms =
                    samples.map {
                        it.first
                    }

                val type =
                    monotonicType(values)

                if (type != null) {

                    val min =
                        values.minOrNull() ?: 0

                    val max =
                        values.maxOrNull() ?: 0

                    if (max != min) {

                        sb.appendLine(
                            String.format(
                                Locale.US,
                                "OFFSET %02d  %s  %s  values=%s  rpm=%s",
                                offset,
                                type,
                                if (type == "INCREASING")
                                    "RPM-LIKE"
                                else
                                    "INVERSE-RPM-LIKE",
                                values,
                                rpms
                            )
                        )
                    }
                }
            }

            return sb.toString()
        }

        fun findRpmU16Correlations(
            samples: List<Pair<Int, Kwp2101Sample>>
        ): String {

            if (samples.size < 2) {
                return "Not enough RPM samples."
            }

            val sb =
                StringBuilder()

            sb.appendLine()
            sb.appendLine(
                "========== RPM U16 ANALYSIS =========="
            )

            val maxSize =
                samples.minOfOrNull {
                    it.second.data.size
                } ?: 0

            if (maxSize < 2) {
                return sb.toString()
            }

            for (offset in 0 until maxSize - 1) {

                val beValues =
                    samples.map {
                        val hi =
                            it.second.data[offset]
                                .toInt() and 0xFF

                        val lo =
                            it.second.data[offset + 1]
                                .toInt() and 0xFF

                        (hi shl 8) or lo
                    }

                val leValues =
                    samples.map {
                        val lo =
                            it.second.data[offset]
                                .toInt() and 0xFF

                        val hi =
                            it.second.data[offset + 1]
                                .toInt() and 0xFF

                        (hi shl 8) or lo
                    }

                val beType =
                    monotonicType(beValues)

                val leType =
                    monotonicType(leValues)

                if (
                    beType != null &&
                    beValues.distinct().size > 1
                ) {

                    sb.appendLine(
                        String.format(
                            Locale.US,
                            "OFFSET %02d-%02d BE %s values=%s",
                            offset,
                            offset + 1,
                            beType,
                            beValues
                        )
                    )
                }

                if (
                    leType != null &&
                    leValues.distinct().size > 1
                ) {

                    sb.appendLine(
                        String.format(
                            Locale.US,
                            "OFFSET %02d-%02d LE %s values=%s",
                            offset,
                            offset + 1,
                            leType,
                            leValues
                        )
                    )
                }
            }

            return sb.toString()
        }

        fun analyze(
            samples: List<Kwp2101Sample>,
            rpmSamples: List<Pair<Int, Kwp2101Sample>>
        ): String {

            val sb =
                StringBuilder()

            sb.appendLine()
            sb.appendLine(
                "########################################"
            )
            sb.appendLine(
                "KWP 2101 FULL ANALYSIS"
            )
            sb.appendLine(
                "SAMPLES = ${samples.size}"
            )
            sb.appendLine(
                "RPM SAMPLES = ${rpmSamples.size}"
            )
            sb.appendLine(
                "########################################"
            )

            if (samples.isEmpty()) {

                sb.appendLine(
                    "NO SAMPLES."
                )

                return sb.toString()
            }

            val changing =
                findChangingOffsets(samples)

            val stable =
                findStableOffsets(samples)

            sb.appendLine()
            sb.appendLine(
                "CHANGING OFFSETS:"
            )

            sb.appendLine(
                changing.joinToString(", ")
            )

            sb.appendLine()
            sb.appendLine(
                "STABLE OFFSETS:"
            )

            sb.appendLine(
                stable.joinToString(", ")
            )

            sb.appendLine(
                findRpmCorrelations(rpmSamples)
            )

            sb.appendLine(
                findRpmU16Correlations(rpmSamples)
            )

            return sb.toString()
        }
    }

    /*
     * ============================================================
     * UI
     * ============================================================
     */

    private lateinit var outputText: TextView

    private lateinit var btnRead2101: Button
    private lateinit var btnAnalyze: Button
    private lateinit var btnClear: Button
    private lateinit var btnCopy: Button

    private val rpmButtons =
        mutableListOf<Button>()

    /*
     * ============================================================
     * STATE
     * ============================================================
     */

    private var commandCounter =
        0

    private var validKwpResponses =
        0

    private var positiveResponses =
        0

    private var negativeResponses =
        0

    private var communicationConfirmed =
        false

    @Volatile
    private var kwpReady =
        false

    private var protocolText =
        ""

    private var protocolNumber =
        ""

    private val fingerprintResults =
        mutableListOf<FingerprintResult>()

    private val live2101Samples =
        mutableListOf<Kwp2101Sample>()

    private val rpm2101Samples =
        mutableListOf<SavedRpmSample>()

    private var last2101Sample:
            Kwp2101Sample? = null

    private val prefs by lazy {
        getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    private val transportLock =
        Any()

    /*
     * ============================================================
     * KWP FRAME
     * ============================================================
     */

    private data class KwpFrame(
        val format: Int,
        val target: Int,
        val source: Int,
        val lengthField: Int,
        val payload: ByteArray,
        val checksum: Int?,
        val calculatedChecksum: Int?,
        val checksumValid: Boolean,
        val rawFrame: ByteArray
    ) {

        val serviceId: Int
            get() =
                if (payload.isNotEmpty())
                    payload[0].toInt() and 0xFF
                else
                    -1

        val localId: Int?
            get() =
                if (payload.size >= 2)
                    payload[1].toInt() and 0xFF
                else
                    null

        val localData: ByteArray
            get() =
                if (payload.size > 2)
                    payload.copyOfRange(
                        2,
                        payload.size
                    )
                else
                    ByteArray(0)

        val negativeService: Int?
            get() =
                if (
                    payload.size >= 3 &&
                    serviceId == 0x7F
                ) {
                    payload[1].toInt() and 0xFF
                } else {
                    null
                }

        val negativeCode: Int?
            get() =
                if (
                    payload.size >= 3 &&
                    serviceId == 0x7F
                ) {
                    payload[2].toInt() and 0xFF
                } else {
                    null
                }

        val data: ByteArray
            get() = localData
    }

    private data class FingerprintResult(
        val command: String,
        val expectedService: Int?,
        val responseService: Int?,
        val validFrame: Boolean,
        val checksumValid: Boolean,
        val raw: String
    )

    /*
     * ============================================================
     * ACTIVITY
     * ============================================================
     */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        buildUi()

        loadPersistentSamples()

        updateSampleStatus()

        Thread {
            runExplorer()
        }.start()
    }

    /*
     * ============================================================
     * UI BUILDER
     * ============================================================
     */

    private fun buildUi() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    12,
                    12,
                    12,
                    12
                )
            }

        val title =
            TextView(this).apply {

                text =
                    "YADRA KWP 2101 LAB"

                textSize =
                    20f

                setTextColor(
                    Color.WHITE
                )

                setBackgroundColor(
                    Color.rgb(
                        25,
                        25,
                        25
                    )
                )

                setPadding(
                    12,
                    12,
                    12,
                    12
                )
            }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        /*
         * --------------------------------------------------------
         * FIRST ROW
         * --------------------------------------------------------
         */

        val row1 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        btnRead2101 =
            Button(this).apply {

                text =
                    "READ 2101"

                setOnClickListener {

                    Thread {
                        read2101Sample(
                            "LIVE"
                        )
                    }.start()
                }
            }

        btnAnalyze =
            Button(this).apply {

                text =
                    "ANALYZE"

                setOnClickListener {

                    analyzeAllSamples()
                }
            }

        btnClear =
            Button(this).apply {

                text =
                    "CLEAR"

                setOnClickListener {

                    clearPersistentSamples()
                }
            }

        addButtonToRow(
            row1,
            btnRead2101
        )

        addButtonToRow(
            row1,
            btnAnalyze
        )

        addButtonToRow(
            row1,
            btnClear
        )

        root.addView(row1)

        /*
         * --------------------------------------------------------
         * SECOND ROW
         * --------------------------------------------------------
         */

        val row2 =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        val offButton =
            makeSampleButton(
                "OFF"
            ) {

                Thread {
                    read2101Sample(
                        "OFF"
                    )
                }.start()
            }

        val idleButton =
            makeSampleButton(
                "IDLE"
            ) {

                Thread {
                    read2101Sample(
                        "IDLE"
                    )
                }.start()
            }

        addButtonToRow(
            row2,
            offButton
        )

        addButtonToRow(
            row2,
            idleButton
        )

        root.addView(row2)

        /*
         * --------------------------------------------------------
         * RPM SCROLL
         * --------------------------------------------------------
         */

        val rpmScroll =
            HorizontalScrollView(this)

        val rpmContainer =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        val rpmValues =
            listOf(
                500,
                750,
                1000,
                1250,
                1500,
                1750,
                2000,
                2250,
                2500,
                2750,
                3000,
                3250,
                3500,
                3750,
                4000,
                4250,
                4500,
                4750,
                5000,
                5250,
                5500,
                5750,
                6000
            )

        for (rpm in rpmValues) {

            val button =
                makeRpmButton(
                    rpm
                )

            rpmButtons.add(
                button
            )

            rpmContainer.addView(
                button
            )
        }

        val customRpm =
            Button(this).apply {

                text =
                    "CUSTOM RPM"

                setOnClickListener {
                    showCustomRpmDialog()
                }
            }

        rpmContainer.addView(
            customRpm
        )

        rpmScroll.addView(
            rpmContainer
        )

        root.addView(
            rpmScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        /*
         * --------------------------------------------------------
         * COPY LOG
         * --------------------------------------------------------
         */

        btnCopy =
            Button(this).apply {

                text =
                    "COPY LOG"

                setOnClickListener {
                    copyLog()
                }
            }

        root.addView(
            btnCopy
        )

        /*
         * --------------------------------------------------------
         * STATUS
         * --------------------------------------------------------
         */

        val status =
            TextView(this).apply {

                tag =
                    "sample_status"

                textSize =
                    13f

                setPadding(
                    8,
                    8,
                    8,
                    8
                )
            }

        root.addView(
            status
        )

        /*
         * --------------------------------------------------------
         * OUTPUT
         * --------------------------------------------------------
         */

        val horizontal =
            HorizontalScrollView(this)

        val vertical =
            ScrollView(this)

        outputText =
            TextView(this).apply {

                textSize =
                    12f

                setTextIsSelectable(
                    true
                )

                setPadding(
                    8,
                    8,
                    8,
                    8
                )

                typeface =
                    android.graphics.Typeface.MONOSPACE
            }

        horizontal.addView(
            outputText
        )

        vertical.addView(
            horizontal
        )

        root.addView(
            vertical,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun addButtonToRow(
        row: LinearLayout,
        button: Button
    ) {

        row.addView(
            button,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
    }

    private fun makeSampleButton(
        textValue: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {

            text =
                textValue

            setOnClickListener {
                action()
            }
        }
    }

    private fun makeRpmButton(
        rpm: Int
    ): Button {

        return Button(this).apply {

            text =
                rpm.toString()

            setOnClickListener {

                Thread {

                    read2101Sample(
                        "${rpm}RPM",
                        rpm
                    )

                }.start()
            }
        }
    }

    /*
     * ============================================================
     * CUSTOM RPM
     * ============================================================
     */

    private fun showCustomRpmDialog() {

        val input =
            EditText(this).apply {

                hint =
                    "RPM"

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                setSingleLine(true)
            }

        AlertDialog.Builder(this)
            .setTitle(
                "READ CUSTOM RPM"
            )
            .setMessage(
                "Enter actual engine RPM."
            )
            .setView(input)
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "READ"
            ) { _, _ ->

                val rpm =
                    input.text
                        .toString()
                        .trim()
                        .toIntOrNull()

                if (
                    rpm == null ||
                    rpm < 0 ||
                    rpm > 12000
                ) {

                    appendLog(
                        "INVALID RPM"
                    )

                    return@setPositiveButton
                }

                Thread {

                    read2101Sample(
                        "${rpm}RPM",
                        rpm
                    )

                }.start()
            }
            .show()
    }

    /*
     * ============================================================
     * SAMPLE STATUS
     * ============================================================
     */

    private fun updateSampleStatus() {

        if (!::outputText.isInitialized) {
            return
        }

        val status =
            outputText.rootView
                .findViewWithTag<TextView>(
                    "sample_status"
                )

        status?.text =
            "Saved samples: ${live2101Samples.size}    " +
                    "Saved RPM points: ${rpm2101Samples.size}"
    }

    /*
     * ============================================================
     * PERMANENT STORAGE
     * ============================================================
     */

    private fun savePersistentSamples() {

        try {

            val samplesJson =
                JSONArray()

            for (sample in live2101Samples) {

                val obj =
                    JSONObject()

                obj.put(
                    "label",
                    sample.label
                )

                obj.put(
                    "data",
                    bytesToHex(
                        sample.data
                    )
                )

                samplesJson.put(
                    obj
                )
            }

            val rpmJson =
                JSONArray()

            for (entry in rpm2101Samples) {

                val obj =
                    JSONObject()

                obj.put(
                    "rpm",
                    entry.rpm
                )

                obj.put(
                    "label",
                    entry.sample.label
                )

                obj.put(
                    "data",
                    bytesToHex(
                        entry.sample.data
                    )
                )

                rpmJson.put(
                    obj
                )
            }

            prefs.edit()
                .putString(
                    KEY_SAMPLES,
                    samplesJson.toString()
                )
                .putString(
                    KEY_RPM_SAMPLES,
                    rpmJson.toString()
                )
                .apply()

            runOnUiThread {
                updateSampleStatus()
            }

        } catch (e: Exception) {

            appendLog(
                "SAVE ERROR: ${e.message}"
            )
        }
    }

    private fun loadPersistentSamples() {

        live2101Samples.clear()
        rpm2101Samples.clear()

        try {

            val samplesString =
                prefs.getString(
                    KEY_SAMPLES,
                    null
                )

            if (!samplesString.isNullOrBlank()) {

                val array =
                    JSONArray(
                        samplesString
                    )

                for (i in 0 until array.length()) {

                    val obj =
                        array.getJSONObject(i)

                    val label =
                        obj.optString(
                            "label"
                        )

                    val hex =
                        obj.optString(
                            "data"
                        )

                    val data =
                        hexToBytes(hex)

                    if (
                        label.isNotBlank() &&
                        data.isNotEmpty()
                    ) {

                        live2101Samples.add(
                            Kwp2101Sample(
                                label,
                                data
                            )
                        )
                    }
                }
            }

            val rpmString =
                prefs.getString(
                    KEY_RPM_SAMPLES,
                    null
                )

            if (!rpmString.isNullOrBlank()) {

                val array =
                    JSONArray(
                        rpmString
                    )

                for (i in 0 until array.length()) {

                    val obj =
                        array.getJSONObject(i)

                    val rpm =
                        obj.optInt(
                            "rpm",
                            -1
                        )

                    val label =
                        obj.optString(
                            "label"
                        )

                    val hex =
                        obj.optString(
                            "data"
                        )

                    val data =
                        hexToBytes(hex)

                    if (
                        rpm >= 0 &&
                        label.isNotBlank() &&
                        data.isNotEmpty()
                    ) {

                        rpm2101Samples.add(
                            SavedRpmSample(
                                rpm,
                                Kwp2101Sample(
                                    label,
                                    data
                                )
                            )
                        )
                    }
                }
            }

            /*
             * If old storage contains RPM samples but the
             * separate RPM list does not, rebuild it.
             */

            if (
                rpm2101Samples.isEmpty() &&
                live2101Samples.isNotEmpty()
            ) {

                val rpmRegex =
                    Regex(
                        "^(\\d+)RPM$",
                        RegexOption.IGNORE_CASE
                    )

                for (sample in live2101Samples) {

                    val match =
                        rpmRegex.matchEntire(
                            sample.label
                        )

                    if (match != null) {

                        val rpm =
                            match.groupValues[1]
                                .toIntOrNull()

                        if (rpm != null) {

                            rpm2101Samples.add(
                                SavedRpmSample(
                                    rpm,
                                    sample
                                )
                            )
                        }
                    }
                }
            }

            appendLog(
                "Persistent samples loaded: " +
                        live2101Samples.size
            )

            appendLog(
                "Persistent RPM points loaded: " +
                        rpm2101Samples.size
            )

        } catch (e: Exception) {

            appendLog(
                "LOAD ERROR: ${e.message}"
            )
        }
    }

    private fun clearPersistentSamples() {

        AlertDialog.Builder(this)
            .setTitle(
                "CLEAR SAVED DATA"
            )
            .setMessage(
                "Delete all stored 2101 samples and RPM points?"
            )
            .setNegativeButton(
                "CANCEL",
                null
            )
            .setPositiveButton(
                "DELETE"
            ) { _, _ ->

                live2101Samples.clear()

                rpm2101Samples.clear()

                last2101Sample =
                    null

                prefs.edit()
                    .remove(KEY_SAMPLES)
                    .remove(KEY_RPM_SAMPLES)
                    .apply()

                appendLog(
                    "ALL PERSISTENT SAMPLES CLEARED."
                )

                updateSampleStatus()
            }
            .show()
    }

    /*
     * ============================================================
     * HEX STORAGE
     * ============================================================
     */

    private fun bytesToHex(
        data: ByteArray
    ): String {

        val sb =
            StringBuilder()

        for (b in data) {

            sb.append(
                String.format(
                    Locale.US,
                    "%02X",
                    b.toInt() and 0xFF
                )
            )
        }

        return sb.toString()
    }

    private fun hexToBytes(
        hex: String
    ): ByteArray {

        val clean =
            hex
                .replace(
                    "\\s".toRegex(),
                    ""
                )
                .uppercase(Locale.US)

        if (
            clean.isEmpty() ||
            clean.length % 2 != 0
        ) {
            return ByteArray(0)
        }

        val result =
            ByteArray(
                clean.length / 2
            )

        for (i in result.indices) {

            val index =
                i * 2

            result[i] =
                clean.substring(
                    index,
                    index + 2
                ).toInt(16).toByte()
        }

        return result
    }

    /*
     * ============================================================
     * COPY LOG
     * ============================================================
     */

    private fun copyLog() {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "YADRA KWP LOG",
                outputText.text.toString()
            )
        )

        appendLog(
            "LOG COPIED."
        )
    }

    /*
     * ============================================================
     * MAIN EXPLORER
     * ============================================================
     */

    private fun runExplorer() {

        appendLog(
            "YADRA KWP ECU EXPLORER"
        )

        appendLog(
            "=============================="
        )

        appendLog(
            "MODE"
        )

        appendLog(
            "Unknown ECU / KWP exploration"
        )

        appendLog(
            "Protocol was already discovered."
        )

        appendLog(
            "46-protocol scan is NOT repeated."
        )

        appendLog(
            "Selected protocol: $SAVED_PROTOCOL"
        )

        appendLog("CONNECT")

        appendLog(
            "------------------------------"
        )

        val connected =
            try {

                YadraConnectionManager
                    .connectToElm327Transport()

            } catch (e: Exception) {

                appendLog(
                    "CONNECT ERROR: ${e.message}"
                )

                false
            }

        appendLog(
            "ELM CONNECTED: $connected"
        )

        if (!connected) {

            appendLog(
                "ELM CONNECTION FAILED."
            )

            return
        }

        /*
         * ELM setup
         */

        runElmCommand(
            "ATI"
        )

        runElmCommand(
            "ATE0"
        )

        runElmCommand(
            "ATL0"
        )

        runElmCommand(
            "ATS0"
        )

        runElmCommand(
            "ATH1"
        )

        runElmCommand(
            "ATAT1"
        )

        runElmCommand(
            "ATST32"
        )

        val dp =
            runElmCommand(
                "ATDP"
            )

        val dpn =
            runElmCommand(
                "ATDPN"
            )

        protocolText =
            dp

        protocolNumber =
            dpn

        appendLog(
            "ATDP RESULT:"
        )

        appendLog(dp)

        appendLog(
            "ATDPN RESULT:"
        )

        appendLog(dpn)

        val kwpDetected =
            dp.contains(
                "ISO 14230",
                ignoreCase = true
            ) ||
                    dp.contains(
                        "KWP",
                        ignoreCase = true
                    ) ||
                    dpn.contains(
                        "A5",
                        ignoreCase = true
                    ) ||
                    SAVED_PROTOCOL.equals(
                        dpn.trim(),
                        ignoreCase = true
                    )

        if (!kwpDetected) {

            appendLog(
                "WARNING: KWP FAST not explicitly detected."
            )

        } else {

            appendLog(
                "KWP FAST DETECTED."
            )
        }

        /*
         * The ELM configuration is ready.
         */

        kwpReady =
            true

        /*
         * Basic probe
         */

        runKwpCommand(
            "0100"
        )

        /*
         * Fingerprint services
         */

        testFingerprintCommand(
            "3E",
            0x7E
        )

        testFingerprintCommand(
            "1A90",
            0x5A
        )

        testFingerprintCommand(
            "1A91",
            0x5A
        )

        testFingerprintCommand(
            "2101",
            0x61
        )

        /*
         * First automatic 2101 sample
         */

        analyzeSingle2101()

        /*
         * DTC
         */

        testFingerprintCommand(
            "13",
            0x53
        )

        /*
         * Final
         */

        appendLog(
            "=============================="
        )

        appendLog(
            "EXPLORATION COMPLETE"
        )

        appendLog(
            "VALID KWP RESPONSES: $validKwpResponses"
        )

        appendLog(
            "POSITIVE RESPONSES: $positiveResponses"
        )

        appendLog(
            "NEGATIVE RESPONSES: $negativeResponses"
        )

        appendLog(
            "2101 SAMPLES: ${live2101Samples.size}"
        )

        appendLog(
            "RPM POINTS: ${rpm2101Samples.size}"
        )
    }

    /*
     * ============================================================
     * ELM COMMAND
     * ============================================================
     */

    private fun runElmCommand(
        command: String
    ): String {

        commandCounter++

        appendLog(
            "COMMAND $commandCounter"
        )

        appendLog(
            "COMMAND: $command"
        )

        val response =
            try {

                synchronized(
                    transportLock
                ) {

                    YadraConnectionManager
                        .sendCommand(
                            command
                        )
                }

            } catch (e: Exception) {

                appendLog(
                    "ERROR: ${e.message}"
                )

                ""
            }

        appendLog(
            "TX: $command"
        )

        appendLog(
            "RX: $response"
        )

        return response
    }

    /*
     * ============================================================
     * KWP COMMAND
     * ============================================================
     */

    private fun runKwpCommand(
        command: String
    ): String {

        commandCounter++

        appendLog(
            "COMMAND $commandCounter"
        )

        appendLog(
            "COMMAND: $command"
        )

        val response =
            try {

                synchronized(
                    transportLock
                ) {

                    YadraConnectionManager
                        .sendCommand(
                            command
                        )
                }

            } catch (e: Exception) {

                appendLog(
                    "ERROR: ${e.message}"
                )

                ""
            }

        appendLog(
            "TX: $command"
        )

        appendLog(
            "RX: $response"
        )

        return response
    }

    /*
     * ============================================================
     * FINGERPRINT
     * ============================================================
     */

    private fun testFingerprintCommand(
        command: String,
        expectedService: Int
    ) {

        val response =
            runKwpCommand(
                command
            )

        val frames =
            parseKwpResponse(
                response
            )

        var matched =
            false

        for (frame in frames) {

            registerKwpFrame(
                frame
            )

            if (
                frame.checksumValid &&
                frame.serviceId ==
                expectedService
            ) {

                matched =
                    true
            }
        }

        fingerprintResults.add(
            FingerprintResult(
                command = command,
                expectedService = expectedService,
                responseService =
                    frames.firstOrNull()
                        ?.serviceId,
                validFrame = frames.isNotEmpty(),
                checksumValid =
                    frames.any {
                        it.checksumValid
                    },
                raw = response
            )
        )

        appendLog(
            if (matched)
                "FINGERPRINT MATCH: $command"
            else
                "FINGERPRINT NO MATCH: $command"
        )
    }

    /*
     * ============================================================
     * 2101 INITIAL SAMPLE
     * ============================================================
     */

    private fun analyzeSingle2101() {

        if (!kwpReady) {
            appendLog(
                "2101 skipped: KWP not ready."
            )
            return
        }

        val response =
            runKwpCommand(
                "2101"
            )

        val frames =
            parseKwpResponse(
                response
            )

        val frame =
            frames.firstOrNull {
                it.checksumValid &&
                        it.serviceId == 0x61 &&
                        it.localId == 0x01
            }

        if (frame == null) {

            appendLog(
                "2101 INITIAL: valid 61/01 frame not found."
            )

            return
        }

        val data =
            frame.localData

        if (data.isEmpty()) {

            appendLog(
                "2101 INITIAL: empty data."
            )

            return
        }

        val sample =
            Kwp2101Sample(
                label = "INITIAL",
                data = data.copyOf()
            )

        replaceSample(
            sample
        )

        last2101Sample =
            sample

        savePersistentSamples()

        appendLog(
            "INITIAL 2101 SAMPLE SAVED PERMANENTLY."
        )

        appendLog(
            Kwp2101Analyzer.createTable(
                sample
            )
        )
    }

    /*
     * ============================================================
     * READ 2101 SAMPLE
     * ============================================================
     */

    private fun read2101Sample(
        label: String,
        rpm: Int? = null
    ) {

        if (!kwpReady) {

            appendLog(
                "READ 2101 ignored: KWP transport is not ready yet."
            )

            return
        }

        appendLog(
            "=============================="
        )

        appendLog(
            "READ SAMPLE: $label"
        )

        if (rpm != null) {

            appendLog(
                "TARGET RPM: $rpm"
            )
        }

        val response =
            runKwpCommand(
                "2101"
            )

        val frames =
            parseKwpResponse(
                response
            )

        val frame =
            frames.firstOrNull {
                it.checksumValid &&
                        it.serviceId == 0x61 &&
                        it.localId == 0x01
            }

        if (frame == null) {

            appendLog(
                "2101: valid 61/01 frame NOT FOUND."
            )

            return
        }

        val data =
            frame.localData.copyOf()

        if (data.isEmpty()) {

            appendLog(
                "2101: EMPTY LOCAL DATA."
            )

            return
        }

        if (data.size != 90) {

            appendLog(
                "WARNING: expected 90 data bytes, got ${data.size}"
            )
        }

        val sample =
            Kwp2101Sample(
                label = label,
                data = data
            )

        replaceSample(
            sample
        )

        last2101Sample =
            sample

        if (rpm != null) {

            rpm2101Samples.removeAll {
                it.rpm == rpm
            }

            rpm2101Samples.add(
                SavedRpmSample(
                    rpm = rpm,
                    sample = sample
                )
            )

            rpm2101Samples.sortBy {
                it.rpm
            }
        }

        savePersistentSamples()

        appendLog(
            "SAMPLE SAVED PERMANENTLY:"
        )

        appendLog(
            "LABEL = $label"
        )

        appendLog(
            "SIZE = ${data.size}"
        )

        if (rpm != null) {

            appendLog(
                "RPM = $rpm"
            )
        }

        appendLog(
            Kwp2101Analyzer.createTable(
                sample
            )
        )

        appendLog(
            "TOTAL SAVED SAMPLES = ${live2101Samples.size}"
        )

        appendLog(
            "TOTAL SAVED RPM POINTS = ${rpm2101Samples.size}"
        )

        runOnUiThread {
            updateSampleStatus()
        }
    }

    /*
     * ============================================================
     * REPLACE SAMPLE
     * ============================================================
     */

    private fun replaceSample(
        sample: Kwp2101Sample
    ) {

        val index =
            live2101Samples.indexOfFirst {
                it.label.equals(
                    sample.label,
                    ignoreCase = true
                )
            }

        if (index >= 0) {

            live2101Samples[index] =
                sample

        } else {

            live2101Samples.add(
                sample
            )
        }
    }

    /*
     * ============================================================
     * ANALYZE ALL
     * ============================================================
     */

    private fun analyzeAllSamples() {

        if (live2101Samples.isEmpty()) {

            appendLog(
                "NO SAVED 2101 SAMPLES."
            )

            return
        }

        val sortedRpm =
            rpm2101Samples
                .sortedBy {
                    it.rpm
                }
                .map {
                    Pair(
                        it.rpm,
                        it.sample
                    )
                }

        appendLog(
            "=============================="
        )

        appendLog(
            "ANALYZING ALL PERSISTENT DATA"
        )

        appendLog(
            "SAMPLES = ${live2101Samples.size}"
        )

        appendLog(
            "RPM POINTS = ${sortedRpm.size}"
        )

        appendLog(
            Kwp2101Analyzer.analyze(
                samples =
                    live2101Samples.toList(),
                rpmSamples =
                    sortedRpm
            )
        )

        /*
         * Print all RPM samples in order.
         */

        appendLog(
            "========== SAVED RPM MAP =========="
        )

        for (entry in rpm2101Samples.sortedBy {
            it.rpm
        }) {

            appendLog(
                "${entry.rpm} RPM -> ${entry.sample.label}"
            )
        }
    }

    /*
     * ============================================================
     * KWP FRAME REGISTRATION
     * ============================================================
     */

    private fun registerKwpFrame(
        frame: KwpFrame
    ) {

        validKwpResponses++

        if (
            frame.serviceId in
            0x50..0x77
        ) {

            positiveResponses++

        } else if (
            frame.serviceId == 0x7F
        ) {

            negativeResponses++
        }

        communicationConfirmed =
            communicationConfirmed ||
                    frame.checksumValid
    }

    /*
     * ============================================================
     * PARSER
     * ============================================================
     */

    private fun parseKwpResponse(
        response: String
    ): List<KwpFrame> {

        val candidates =
            extractKwpHexCandidates(
                response
            )

        val result =
            mutableListOf<KwpFrame>()

        for (candidate in candidates) {

            val frame =
                parseOneKwpFrame(
                    candidate
                )

            if (frame != null) {

                result.add(
                    frame
                )

                appendFrameLog(
                    frame
                )
            }
        }

        return result
    }

    private fun parseOneKwpFrame(
        bytes: ByteArray
    ): KwpFrame? {

        if (bytes.size < 5) {
            return null
        }

        val format =
            bytes[0].toInt() and 0xFF

        val target =
            bytes[1].toInt() and 0xFF

        val source =
            bytes[2].toInt() and 0xFF

        val lengthField =
            bytes[3].toInt() and 0xFF

        val payloadLength =
            when {

                (format and 0xC0) == 0x80 ->
                    lengthField

                (format and 0xC0) == 0xC0 ->
                    lengthField

                else ->
                    return null
            }

        val payloadStart =
            4

        val payloadEnd =
            payloadStart +
                    payloadLength

        if (
            payloadEnd >= bytes.size
        ) {
            return null
        }

        val payload =
            bytes.copyOfRange(
                payloadStart,
                payloadEnd
            )

        val checksum =
            bytes[payloadEnd]
                .toInt() and 0xFF

        var sum =
            0

        for (
        i in 0 until payloadEnd
        ) {

            sum +=
                bytes[i].toInt() and 0xFF

            sum =
                sum and 0xFF
        }

        val calculated =
            sum

        return KwpFrame(
            format = format,
            target = target,
            source = source,
            lengthField = lengthField,
            payload = payload,
            checksum = checksum,
            calculatedChecksum = calculated,
            checksumValid =
                checksum == calculated,
            rawFrame =
                bytes.copyOf()
        )
    }

    /*
     * ============================================================
     * HEX CANDIDATE EXTRACTION
     * ============================================================
     */

    private fun extractKwpHexCandidates(
        response: String
    ): List<ByteArray> {

        val clean =
            response
                .replace(
                    "\r",
                    " "
                )
                .replace(
                    "\n",
                    " "
                )

        val tokenRegex =
            Regex(
                "(?i)\\b[0-9A-F]{2}\\b"
            )

        val tokens =
            tokenRegex
                .findAll(clean)
                .map {
                    it.value
                }
                .toList()

        if (tokens.isEmpty()) {
            return emptyList()
        }

        val all =
            ByteArray(
                tokens.size
            )

        for (i in tokens.indices) {

            all[i] =
                tokens[i]
                    .toInt(16)
                    .toByte()
        }

        val result =
            mutableListOf<ByteArray>()

        /*
         * Search possible KWP frames.
         */

        for (start in all.indices) {

            if (
                start + 4 >=
                all.size
            ) {
                break
            }

            val format =
                all[start]
                    .toInt() and 0xFF

            if (
                (format and 0xC0) != 0x80 &&
                (format and 0xC0) != 0xC0
            ) {
                continue
            }

            val length =
                all[start + 3]
                    .toInt() and 0xFF

            val total =
                5 + length

            if (
                start + total <=
                all.size
            ) {

                result.add(
                    all.copyOfRange(
                        start,
                        start + total
                    )
                )
            }
        }

        /*
         * If no structured frame was found,
         * also try the entire token stream.
         */

        if (
            result.isEmpty() &&
            all.size >= 5
        ) {

            result.add(
                all
            )
        }

        return result
    }

    /*
     * ============================================================
     * FRAME LOG
     * ============================================================
     */

    private fun appendFrameLog(
        frame: KwpFrame
    ) {

        appendLog(
            "KWP FRAME"
        )

        appendLog(
            "FORMAT: " +
                    hexByte(frame.format)
        )

        appendLog(
            "TARGET: " +
                    hexByte(frame.target)
        )

        appendLog(
            "SOURCE: " +
                    hexByte(frame.source)
        )

        appendLog(
            "LENGTH: " +
                    frame.lengthField
        )

        appendLog(
            "SERVICE: " +
                    if (frame.serviceId >= 0)
                        hexByte(frame.serviceId)
                    else
                        "NONE"
        )

        if (frame.localId != null) {

            appendLog(
                "LOCAL ID: " +
                        hexByte(
                            frame.localId!!
                        )
            )
        }

        appendLog(
            "DATA SIZE: " +
                    frame.localData.size
        )

        appendLog(
            "CHECKSUM RX: " +
                    if (frame.checksum != null)
                        hexByte(
                            frame.checksum!!
                        )
                    else
                        "NONE"
        )

        appendLog(
            "CHECKSUM CALC: " +
                    if (
                        frame.calculatedChecksum != null
                    )
                        hexByte(
                            frame.calculatedChecksum!!
                        )
                    else
                        "NONE"
        )

        appendLog(
            "CHECKSUM VALID: " +
                    frame.checksumValid
        )

        appendLog(
            "RAW: " +
                    bytesToHex(
                        frame.rawFrame
                    )
        )

        if (
            frame.serviceId == 0x7F
        ) {

            appendLog(
                "NEGATIVE SERVICE: " +
                        (
                                frame.negativeService
                                    ?.let {
                                        hexByte(it)
                                    }
                                    ?: "NONE"
                                )
            )

            appendLog(
                "NEGATIVE CODE: " +
                        (
                                frame.negativeCode
                                    ?.let {
                                        hexByte(it)
                                    }
                                    ?: "NONE"
                                )
            )
        }
    }

    /*
     * ============================================================
     * LOG
     * ============================================================
     */

    private fun appendLog(
        text: String
    ) {

        runOnUiThread {

            if (!::outputText.isInitialized) {
                return@runOnUiThread
            }

            outputText.append(
                text
            )

            outputText.append(
                "\n"
            )

            val scrollParent =
                outputText.parent?.parent

            if (
                scrollParent is ScrollView
            ) {

                scrollParent.post {
                    scrollParent.fullScroll(
                        ScrollView.FOCUS_DOWN
                    )
                }
            }
        }
    }

    /*
     * ============================================================
     * HELPERS
     * ============================================================
     */

    private fun hexByte(
        value: Int
    ): String {

        return String.format(
            Locale.US,
            "%02X",
            value and 0xFF
        )
    }

    private fun weightParams(): String {

        return """
            KWP FAST
            Protocol: $SAVED_PROTOCOL
            ECU: $ECU_ADDRESS
            2101 local ID: 01
            Expected response: 61 01
        """.trimIndent()
    }
}