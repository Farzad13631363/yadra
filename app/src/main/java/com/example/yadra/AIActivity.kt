package com.example.yadra

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class AIActivity : AppCompatActivity() {

    private lateinit var scrollConversation: ScrollView
    private lateinit var messagesLayout: LinearLayout

    private lateinit var editQuestion: EditText
    private lateinit var btnSend: Button

    private lateinit var btnAskDtc: Button
    private lateinit var btnAskSymptoms: Button
    private lateinit var btnAnalyzeEcu: Button

    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_ai)

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {

        scrollConversation =
            findViewById(R.id.scrollAiConversation)

        messagesLayout =
            findViewById(R.id.layoutAiMessages)

        editQuestion =
            findViewById(R.id.editAiQuestion)

        btnSend =
            findViewById(R.id.btnSendAi)

        btnAskDtc =
            findViewById(R.id.btnAskDtc)

        btnAskSymptoms =
            findViewById(R.id.btnAskSymptoms)

        btnAnalyzeEcu =
            findViewById(R.id.btnAnalyzeEcu)

        txtStatus =
            findViewById(R.id.txtAiStatus)
    }

    private fun setupListeners() {

        btnSend.setOnClickListener {
            sendUserQuestion()
        }

        btnAskDtc.setOnClickListener {

            editQuestion.setText(
                "کد خطای خودرو من چیست و چه علتی می‌تواند داشته باشد؟"
            )

            editQuestion.setSelection(
                editQuestion.text.length
            )

            editQuestion.requestFocus()

            showKeyboard()
        }

        btnAskSymptoms.setOnClickListener {

            editQuestion.setText(
                "خودرو من مشکل دارد. علائم خرابی را بررسی کن و بگو چه قسمت‌هایی باید بررسی شوند."
            )

            editQuestion.setSelection(
                editQuestion.text.length
            )

            editQuestion.requestFocus()

            showKeyboard()
        }

        btnAnalyzeEcu.setOnClickListener {
            sendEcuAnalysisRequest()
        }
    }

    private fun sendUserQuestion() {

        val question =
            editQuestion.text
                .toString()
                .trim()

        if (question.isEmpty()) {
            return
        }

        addUserMessage(question)

        editQuestion.text.clear()

        hideKeyboard()

        txtStatus.text = "● Processing..."
        txtStatus.setTextColor(
            Color.rgb(255, 193, 7)
        )

        addAiMessage(
            "سؤال شما دریافت شد.\n\n" +
                    "در مرحله بعد این بخش به هوش مصنوعی متصل می‌شود " +
                    "تا بتواند کدهای خطا، علائم خرابی و اطلاعات ECU " +
                    "را تحلیل کند."
        )

        txtStatus.text = "● AI Ready"
        txtStatus.setTextColor(
            Color.rgb(0, 230, 118)
        )
    }

    private fun sendEcuAnalysisRequest() {

        addUserMessage(
            "اطلاعات ECU خودرو را تحلیل کن."
        )

        txtStatus.text = "● Analyzing ECU..."
        txtStatus.setTextColor(
            Color.rgb(255, 193, 7)
        )

        addAiMessage(
            "برای تحلیل ECU، اطلاعات زنده خودرو باید دریافت شود.\n\n" +
                    "در مرحله بعد اطلاعات ECU به صورت خودکار " +
                    "جمع‌آوری و برای تحلیل هوش مصنوعی ارسال می‌شود."
        )

        txtStatus.text = "● AI Ready"
        txtStatus.setTextColor(
            Color.rgb(0, 230, 118)
        )

        scrollToBottom()
    }

    private fun addUserMessage(message: String) {

        val card = CardView(this)

        card.radius = 18f
        card.cardElevation = 3f
        card.setCardBackgroundColor(
            Color.rgb(25, 118, 210)
        )

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            50,
            8,
            0,
            8
        )

        card.layoutParams = params

        val textView = TextView(this)

        textView.text = message

        textView.setTextColor(
            Color.WHITE
        )

        textView.textSize = 15f

        textView.setPadding(
            18,
            14,
            18,
            14
        )

        textView.gravity = Gravity.RIGHT

        card.addView(textView)

        messagesLayout.addView(card)

        scrollToBottom()
    }

    private fun addAiMessage(message: String) {

        val card = CardView(this)

        card.radius = 18f
        card.cardElevation = 3f
        card.setCardBackgroundColor(
            Color.rgb(23, 29, 36)
        )

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.setMargins(
            0,
            8,
            50,
            8
        )

        card.layoutParams = params

        val container =
            LinearLayout(this)

        container.orientation =
            LinearLayout.VERTICAL

        container.setPadding(
            18,
            14,
            18,
            14
        )

        val title = TextView(this)

        title.text = "🤖 Yadra AI"

        title.setTextColor(
            Color.rgb(126, 87, 194)
        )

        title.textSize = 14f

        title.setTypeface(
            null,
            Typeface.BOLD
        )

        val textView = TextView(this)

        textView.text = message

        textView.setTextColor(
            Color.rgb(207, 216, 220)
        )

        textView.textSize = 15f

        textView.setLineSpacing(
            3f,
            1f
        )

        val titleParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        titleParams.bottomMargin = 6

        container.addView(
            title,
            titleParams
        )

        container.addView(textView)

        card.addView(container)

        messagesLayout.addView(card)

        scrollToBottom()
    }

    private fun scrollToBottom() {

        scrollConversation.post {
            scrollConversation.fullScroll(
                View.FOCUS_DOWN
            )
        }
    }

    private fun showKeyboard() {

        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        inputMethodManager.showSoftInput(
            editQuestion,
            InputMethodManager.SHOW_IMPLICIT
        )
    }

    private fun hideKeyboard() {

        val inputMethodManager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(
            editQuestion.windowToken,
            0
        )
    }
}