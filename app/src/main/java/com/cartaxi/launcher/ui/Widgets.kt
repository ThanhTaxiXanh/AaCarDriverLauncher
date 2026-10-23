package com.cartaxi.launcher.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.cartaxi.launcher.LunarCalendar
import com.cartaxi.launcher.media.MediaRepository
import com.cartaxi.launcher.telemetry.OsmRepository
import com.cartaxi.launcher.telemetry.TelemetryRepository
import java.util.Calendar
import kotlin.math.min
import kotlin.math.roundToInt

interface LauncherWidget {
    val view: View
    /** Gọi mỗi giây trên main thread. */
    fun refresh()
}

private const val GREY = 0xFF9AA4B2.toInt()

class LunarWidget(private val ctx: Context) : LauncherWidget {
    private val weekday = autoText(ctx, 10, 40, color = GREY)
    private val solar = autoText(ctx, 14, 80, bold = true)
    private val lunar = autoText(ctx, 12, 50)
    private val canChi = autoText(ctx, 10, 40, color = 0xFFFFC857.toInt())
    private var lastKey = -1

    override val view: View = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ctx.dp(8), ctx.dp(8), ctx.dp(8), ctx.dp(8))
        addView(weekday, lp(MATCH, 0, 1f))
        addView(solar, lp(MATCH, 0, 2.2f))
        addView(lunar, lp(MATCH, 0, 1.3f))
        addView(canChi, lp(MATCH, 0, 1f))
    }

    private val days = arrayOf("Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy")

    init { refresh() }

    override fun refresh() {
        val cal = Calendar.getInstance()
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH) + 1
        val y = cal.get(Calendar.YEAR)
        val key = y * 10000 + m * 100 + d
        if (key == lastKey) return
        lastKey = key
        val l = LunarCalendar.solarToLunar(d, m, y)
        weekday.text = days[cal.get(Calendar.DAY_OF_WEEK) - 1]
        solar.text = "%02d/%02d/%d".format(d, m, y)
        lunar.text = "Âm lịch: %02d/%02d%s".format(l.day, l.month, if (l.leap) " (nhuận)" else "")
        canChi.text = "Năm " + LunarCalendar.canChi(l.year)
    }
}

class ImageWidget(
    private val ctx: Context,
    private val paths: List<String>,
    private val seconds: Int
) : LauncherWidget {
    private val image = ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
    private val empty = autoText(ctx, 10, 28, color = GREY)
    private var index = -1
    private var tick = 0

    override val view: View = FrameLayout(ctx).apply {
        addView(image, FrameLayout.LayoutParams(MATCH, MATCH))
        addView(empty, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    init {
        empty.text = "Chưa chọn ảnh"
        show(0)
    }

    override fun refresh() {
        if (paths.size > 1 && ++tick >= seconds) {
            tick = 0
            show((index + 1) % paths.size)
        }
    }

    private fun show(i: Int) {
        if (paths.isEmpty()) return
        val bmp = BitmapFactory.decodeFile(paths[i]) ?: return
        image.setImageBitmap(bmp)
        index = i
        empty.visibility = View.GONE
    }
}

class MediaWidget(private val ctx: Context) : LauncherWidget {
    private val title = autoText(ctx, 12, 48, bold = true, lines = 2)
    private val artist = autoText(ctx, 10, 36, color = GREY)
    private val state = autoText(ctx, 9, 28, color = 0xFF4FC3F7.toInt())
    private var tick = 0

    override val view: View = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ctx.dp(8), ctx.dp(8), ctx.dp(8), ctx.dp(8))
        addView(title, lp(MATCH, 0, 2f))
        addView(artist, lp(MATCH, 0, 1f))
        addView(state, lp(MATCH, 0, 0.8f))
    }

    init { refresh() }

    override fun refresh() {
        if (!MediaRepository.hasAccess && tick++ % 5 == 0) MediaRepository.connect(ctx)
        if (!MediaRepository.hasAccess) {
            title.text = "Chưa cấp quyền"
            artist.text = "Bật \"Truy cập thông báo\" cho app trên điện thoại"
            state.text = ""
            return
        }
        title.text = MediaRepository.title.ifBlank { "Không có bài đang phát" }
        artist.text = MediaRepository.artist
        state.text = if (MediaRepository.isPlaying) "▶ Đang phát" else "⏸ Tạm dừng"
    }
}

/** Biển tốc độ giới hạn (tròn viền đỏ). */
class SpeedSignView(ctx: Context) : View(ctx) {
    var limit: Int? = null
        set(v) { if (field != v) { field = v; invalidate() } }

    override fun onDraw(canvas: Canvas) {
        val r = min(width, height) / 2f - 2f
        SignRenderer.drawSpeed(canvas, limit?.toString() ?: "--", width / 2f, height / 2f, r)
    }
}

class TelemetryWidget(private val ctx: Context) : LauncherWidget {
    private val speed = autoText(ctx, 24, 160, bold = true)
    private val unit = autoText(ctx, 8, 28, color = GREY)
    private val sign = SpeedSignView(ctx)
    private val trip = autoText(ctx, 10, 30)
    private val gps = autoText(ctx, 9, 26, color = GREY)

    override val view: View = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(ctx.dp(8), ctx.dp(8), ctx.dp(8), ctx.dp(8))
        val speedBox = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            addView(speed, lp(MATCH, 0, 3f))
            addView(unit, lp(MATCH, 0, 1f))
        }
        val top = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(speedBox, lp(0, MATCH, 1f))
            addView(sign, lp(0, MATCH, 1f).apply { setMargins(ctx.dp(6), ctx.dp(4), ctx.dp(6), ctx.dp(4)) })
        }
        addView(top, lp(MATCH, 0, 3f))
        addView(trip, lp(MATCH, 0, 1f))
        addView(gps, lp(MATCH, 0, 1f))
    }

    init { unit.text = "km/h"; refresh() }

    override fun refresh() {
        val kmh = TelemetryRepository.speedKmh.roundToInt()
        val limit = OsmRepository.currentLimit
        sign.limit = limit
        speed.text = kmh.toString()
        speed.setTextColor(if (limit != null && kmh > limit) Color.rgb(255, 80, 80) else Color.WHITE)
        trip.text = "Distance %.1f km   AVG %d km/h".format(
            TelemetryRepository.distanceM / 1000.0, TelemetryRepository.avgKmh.roundToInt()
        )
        gps.text = if (TelemetryRepository.hasFix)
            "GPS: tốt (±%d m)".format(TelemetryRepository.accuracyM.roundToInt())
        else "GPS: đang tìm tín hiệu…"
    }
}
