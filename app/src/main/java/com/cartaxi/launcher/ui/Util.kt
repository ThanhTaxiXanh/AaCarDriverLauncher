package com.cartaxi.launcher.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

fun lp(w: Int, h: Int, weight: Float = 0f) = LinearLayout.LayoutParams(w, h, weight)

/** TextView tự co giãn cỡ chữ theo kích thước ô (cần đặt LayoutParams cố định/weight). */
fun autoText(
    ctx: Context,
    minSp: Int,
    maxSp: Int,
    bold: Boolean = false,
    color: Int = Color.WHITE,
    lines: Int = 1,
    gravity: Int = Gravity.CENTER
): TextView = TextView(ctx).apply {
    setTextColor(color)
    this.gravity = gravity
    maxLines = lines
    includeFontPadding = false
    if (bold) typeface = Typeface.DEFAULT_BOLD
    setAutoSizeTextTypeUniformWithConfiguration(minSp, maxSp, 1, TypedValue.COMPLEX_UNIT_SP)
}

fun cardFrame(ctx: Context, inner: View): FrameLayout = FrameLayout(ctx).apply {
    background = GradientDrawable().apply {
        setColor(0xFF1B2230.toInt())
        cornerRadius = ctx.dp(14).toFloat()
    }
    outlineProvider = ViewOutlineProvider.BACKGROUND
    clipToOutline = true
    addView(inner, FrameLayout.LayoutParams(MATCH, MATCH))
}
