package com.cartaxi.launcher.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface

/** Vẽ biển báo bằng Canvas: biển tốc độ (tròn viền đỏ), cảnh báo (tam giác), camera. */
object SignRenderer {
    private val RED = Color.rgb(212, 18, 28)

    private fun textPaint(size: Float, color: Int = Color.BLACK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private fun Canvas.centeredText(text: String, cx: Float, cy: Float, p: Paint) {
        drawText(text, cx, cy - (p.descent() + p.ascent()) / 2f, p)
    }

    fun drawSpeed(c: Canvas, text: String, cx: Float, cy: Float, r: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        c.drawCircle(cx, cy, r, fill)
        val ringW = r * 0.22f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RED; style = Paint.Style.STROKE; strokeWidth = ringW
        }
        c.drawCircle(cx, cy, r - ringW / 2f, ring)
        val size = if (text.length >= 3) r * 0.78f else r * 1.0f
        c.centeredText(text, cx, cy, textPaint(size))
    }

    fun speed(value: Int, size: Int): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
            drawSpeed(Canvas(it), value.toString(), size / 2f, size / 2f, size / 2f - 1f)
        }

    fun warning(size: Int): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val s = size.toFloat()
        val path = Path().apply {
            moveTo(s / 2f, s * 0.07f)
            lineTo(s * 0.95f, s * 0.9f)
            lineTo(s * 0.05f, s * 0.9f)
            close()
        }
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = RED; style = Paint.Style.STROKE; strokeWidth = s * 0.09f; strokeJoin = Paint.Join.ROUND
        })
        c.centeredText("!", s / 2f, s * 0.62f, textPaint(s * 0.42f))
        return b
    }

    fun camera(size: Int): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val s = size.toFloat()
        val r = s * 0.14f
        c.drawRoundRect(2f, 2f, s - 2f, s - 2f, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(25, 70, 160) })
        c.drawRoundRect(2f, 2f, s - 2f, s - 2f, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = s * 0.06f
        })
        c.centeredText("CAM", s / 2f, s / 2f, textPaint(s * 0.3f, Color.WHITE))
        return b
    }
}
