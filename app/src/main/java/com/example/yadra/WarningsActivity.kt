package com.example.yadra

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WarningsActivity : AppCompatActivity() {

    private lateinit var warningContainer: LinearLayout
    private lateinit var warningStatus: TextView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_warnings
        )

        warningContainer =
            findViewById(
                R.id.warningContainer
            )

        warningStatus =
            findViewById(
                R.id.txtWarningStatus
            )

        loadWarnings()
    }

    // =========================================================
    // LOAD WARNINGS
    // =========================================================

    private fun loadWarnings() {

        val warnings =
            YadraWarningRepository.getAll()

        showWarnings(
            warnings
        )
    }

    // =========================================================
    // SHOW WARNINGS
    // =========================================================

    private fun showWarnings(
        warnings: List<YadraWarningIcon>
    ) {

        warningContainer.removeAllViews()

        warningStatus.text =
            "تعداد نمادهای موجود: ${warnings.size}"

        if (warnings.isEmpty()) {

            showEmptyMessage()

            return
        }

        warnings.forEach { warning ->

            val warningView =
                createWarningView(
                    warning
                )

            warningContainer.addView(
                warningView
            )
        }
    }

    // =========================================================
    // CREATE WARNING CARD
    // =========================================================

    private fun createWarningView(
        warning: YadraWarningIcon
    ): View {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            dp(16),
            dp(14),
            dp(16),
            dp(14)
        )

        card.setBackgroundColor(
            Color.WHITE
        )

        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardParams.setMargins(
            0,
            0,
            0,
            dp(12)
        )

        card.layoutParams =
            cardParams

        // -----------------------------------------------------
        // TOP ROW
        // -----------------------------------------------------

        val topRow =
            LinearLayout(this)

        topRow.orientation =
            LinearLayout.HORIZONTAL

        topRow.gravity =
            Gravity.CENTER_VERTICAL

        // -----------------------------------------------------
        // COLOR INDICATOR
        // -----------------------------------------------------

        val indicator =
            View(this)

        val indicatorParams =
            LinearLayout.LayoutParams(
                dp(14),
                dp(14)
            )

        indicatorParams.setMargins(
            0,
            0,
            dp(12),
            0
        )

        indicator.layoutParams =
            indicatorParams

        indicator.setBackgroundColor(
            getWarningColor(
                warning.color
            )
        )

        topRow.addView(
            indicator
        )

        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------

        val title =
            TextView(this)

        title.text =
            warning.titleFa

        title.setTextColor(
            Color.rgb(
                23,
                25,
                29
            )
        )

        title.textSize =
            18f

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        val titleParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )

        title.layoutParams =
            titleParams

        topRow.addView(
            title
        )

        // -----------------------------------------------------
        // SEVERITY
        // -----------------------------------------------------

        val severity =
            TextView(this)

        severity.text =
            getSeverityText(
                warning.severity
            )

        severity.setTextColor(
            getWarningColor(
                warning.color
            )
        )

        severity.textSize =
            13f

        severity.setTypeface(
            null,
            Typeface.BOLD
        )

        topRow.addView(
            severity
        )

        card.addView(
            topRow
        )

        // -----------------------------------------------------
        // ENGLISH TITLE
        // -----------------------------------------------------

        val englishTitle =
            TextView(this)

        englishTitle.text =
            warning.titleEn

        englishTitle.setTextColor(
            Color.rgb(
                107,
                112,
                120
            )
        )

        englishTitle.textSize =
            13f

        val englishParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        englishParams.setMargins(
            dp(26),
            dp(4),
            0,
            0
        )

        englishTitle.layoutParams =
            englishParams

        card.addView(
            englishTitle
        )

        // -----------------------------------------------------
        // DESCRIPTION
        // -----------------------------------------------------

        val description =
            TextView(this)

        description.text =
            warning.descriptionFa

        description.setTextColor(
            Color.rgb(
                75,
                80,
                88
            )
        )

        description.textSize =
            14f

        description.setPadding(
            0,
            dp(10),
            0,
            0
        )

        card.addView(
            description
        )

        // -----------------------------------------------------
        // CATEGORY
        // -----------------------------------------------------

        val category =
            TextView(this)

        category.text =
            "دسته: ${
                getCategoryText(
                    warning.category
                )
            }"

        category.setTextColor(
            Color.rgb(
                107,
                112,
                120
            )
        )

        category.textSize =
            13f

        category.setPadding(
            0,
            dp(8),
            0,
            0
        )

        card.addView(
            category
        )

        // -----------------------------------------------------
        // POSSIBLE CAUSES
        // -----------------------------------------------------

        if (
            warning.possibleCausesFa.isNotEmpty()
        ) {

            val causesTitle =
                TextView(this)

            causesTitle.text =
                "دلایل احتمالی"

            causesTitle.setTextColor(
                Color.rgb(
                    23,
                    25,
                    29
                )
            )

            causesTitle.textSize =
                14f

            causesTitle.setTypeface(
                null,
                Typeface.BOLD
            )

            causesTitle.setPadding(
                0,
                dp(10),
                0,
                dp(4)
            )

            card.addView(
                causesTitle
            )

            warning.possibleCausesFa.forEach {
                    cause ->

                val causeText =
                    TextView(this)

                causeText.text =
                    "• $cause"

                causeText.setTextColor(
                    Color.rgb(
                        90,
                        95,
                        103
                    )
                )

                causeText.textSize =
                    13f

                causeText.setPadding(
                    dp(8),
                    dp(2),
                    0,
                    dp(2)
                )

                card.addView(
                    causeText
                )
            }
        }

        // -----------------------------------------------------
        // RECOMMENDED ACTION
        // -----------------------------------------------------

        val actionTitle =
            TextView(this)

        actionTitle.text =
            "اقدام پیشنهادی"

        actionTitle.setTextColor(
            Color.rgb(
                23,
                25,
                29
            )
        )

        actionTitle.textSize =
            14f

        actionTitle.setTypeface(
            null,
            Typeface.BOLD
        )

        actionTitle.setPadding(
            0,
            dp(10),
            0,
            dp(4)
        )

        card.addView(
            actionTitle
        )

        val action =
            TextView(this)

        action.text =
            warning.recommendedActionFa

        action.setTextColor(
            getWarningColor(
                warning.color
            )
        )

        action.textSize =
            13f

        card.addView(
            action
        )

        return card
    }

    // =========================================================
    // SEVERITY TEXT
    // =========================================================

    private fun getSeverityText(
        severity: YadraWarningSeverity
    ): String {

        return when (severity) {

            YadraWarningSeverity.CRITICAL ->
                "خطر"

            YadraWarningSeverity.WARNING ->
                "بررسی"

            YadraWarningSeverity.INFORMATION ->
                "اطلاعات"

            YadraWarningSeverity.STATUS ->
                "فعال"
        }
    }

    // =========================================================
    // CATEGORY TEXT
    // =========================================================

    private fun getCategoryText(
        category: YadraWarningCategory
    ): String {

        return when (category) {

            YadraWarningCategory.ENGINE ->
                "موتور"

            YadraWarningCategory.BRAKE_SAFETY ->
                "ترمز و ایمنی"

            YadraWarningCategory.STABILITY_TRACTION ->
                "پایداری و کنترل کشش"

            YadraWarningCategory.TRANSMISSION ->
                "گیربکس"

            YadraWarningCategory.FUEL_FLUIDS ->
                "سوخت و مایعات"

            YadraWarningCategory.LIGHTING ->
                "چراغ‌ها"

            YadraWarningCategory.DRIVER_ASSIST ->
                "کمک راننده"

            YadraWarningCategory.ACCESS_BODY ->
                "درب و بدنه"

            YadraWarningCategory.COMFORT_STATUS ->
                "وضعیت و امکانات"
        }
    }

    // =========================================================
    // WARNING COLOR
    // =========================================================

    private fun getWarningColor(
        color: YadraWarningColor
    ): Int {

        return when (color) {

            YadraWarningColor.RED ->
                Color.rgb(
                    220,
                    38,
                    38
                )

            YadraWarningColor.AMBER ->
                Color.rgb(
                    245,
                    158,
                    11
                )

            YadraWarningColor.YELLOW ->
                Color.rgb(
                    234,
                    179,
                    8
                )

            YadraWarningColor.GREEN ->
                Color.rgb(
                    22,
                    163,
                    74
                )

            YadraWarningColor.BLUE ->
                Color.rgb(
                    37,
                    99,
                    235
                )

            YadraWarningColor.WHITE ->
                Color.rgb(
                    80,
                    80,
                    80
                )
        }
    }

    // =========================================================
    // EMPTY STATE
    // =========================================================

    private fun showEmptyMessage() {

        val emptyText =
            TextView(this)

        emptyText.text =
            "نماد هشداری برای نمایش وجود ندارد."

        emptyText.setTextColor(
            Color.rgb(
                100,
                105,
                112
            )
        )

        emptyText.textSize =
            16f

        emptyText.gravity =
            Gravity.CENTER

        emptyText.setPadding(
            dp(16),
            dp(40),
            dp(16),
            dp(40)
        )

        warningContainer.addView(
            emptyText
        )
    }

    // =========================================================
    // DP
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}