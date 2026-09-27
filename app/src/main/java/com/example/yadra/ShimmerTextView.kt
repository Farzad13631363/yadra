package com.example.yadra

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class ShimmerTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var shimmerAnimator: ValueAnimator? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        paint.isAntiAlias = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startShimmer()
    }

    private fun startShimmer() {
        shimmerAnimator?.cancel()

        shimmerAnimator = ValueAnimator.ofFloat(-1.5f, 2.5f).apply {
            duration = 2600L
            startDelay = 500L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener {
                updateShimmer(it.animatedValue as Float)
                invalidate()
            }

            start()
        }
    }

    private fun updateShimmer(progress: Float) {
        val width = measuredWidth.toFloat().coerceAtLeast(1f)

        val darkGold = Color.rgb(115, 72, 15)
        val gold = Color.rgb(210, 158, 55)
        val brightGold = Color.rgb(255, 225, 125)

        val center = progress * width
        val shineWidth = width * 0.32f

        paint.shader = LinearGradient(
            center - shineWidth,
            0f,
            center + shineWidth,
            0f,
            intArrayOf(
                darkGold,
                gold,
                brightGold,
                Color.WHITE,
                brightGold,
                gold,
                darkGold
            ),
            floatArrayOf(
                0.0f,
                0.25f,
                0.42f,
                0.50f,
                0.58f,
                0.75f,
                1.0f
            ),
            Shader.TileMode.CLAMP
        )

        paint.setShadowLayer(
            22f,
            0f,
            0f,
            Color.argb(170, 218, 166, 65)
        )
    }

    override fun onDetachedFromWindow() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null

        paint.shader = null
        paint.clearShadowLayer()

        super.onDetachedFromWindow()
    }
}