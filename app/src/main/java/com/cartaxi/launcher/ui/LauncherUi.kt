package com.cartaxi.launcher.ui

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.cartaxi.launcher.data.GridCell
import com.cartaxi.launcher.data.LauncherConfig
import com.cartaxi.launcher.data.SplitMode
import com.cartaxi.launcher.data.WidgetType

/** Dựng toàn bộ màn hình xe: bản đồ + lưới widget theo đúng tỷ lệ % và chế độ chia. */
class LauncherUi(private val context: Context, private val config: LauncherConfig) {
    private val widgets = mutableListOf<LauncherWidget>()
    private val map: MapController
    val root: LinearLayout

    init {
        val mapFrame = FrameLayout(context)
        map = MapController(context, mapFrame)
        val panel = buildPanel()
        val horizontal = config.mode == SplitMode.HORIZONTAL
        val mapW = config.mapPercent.toFloat()
        val panelW = 100f - mapW
        root = LinearLayout(context).apply {
            setBackgroundColor(0xFF0E1116.toInt())
            if (horizontal) {
                orientation = LinearLayout.HORIZONTAL
                addView(mapFrame, lp(0, MATCH, mapW))
                addView(panel, lp(0, MATCH, panelW))
            } else {
                orientation = LinearLayout.VERTICAL
                addView(mapFrame, lp(MATCH, 0, mapW))
                addView(panel, lp(MATCH, 0, panelW))
            }
        }
    }

    private fun buildPanel(): LinearLayout {
        val panel = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        for (r in 0 until config.rows) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0 until config.cols) {
                val w = createWidget(config.cellAt(r, c))
                val v: View = if (w != null) { widgets += w; cardFrame(context, w.view) } else View(context)
                val m = context.dp(4)
                row.addView(v, lp(0, MATCH, 1f).apply { setMargins(m, m, m, m) })
            }
            panel.addView(row, lp(MATCH, 0, 1f))
        }
        return panel
    }

    private fun createWidget(cell: GridCell): LauncherWidget? = when (cell.type) {
        WidgetType.NONE -> null
        WidgetType.LUNAR -> LunarWidget(context)
        WidgetType.IMAGE -> ImageWidget(context, cell.images, config.slideSeconds)
        WidgetType.MEDIA -> MediaWidget(context)
        WidgetType.TELEMETRY -> TelemetryWidget(context)
    }

    fun refresh() {
        widgets.forEach { it.refresh() }
        map.refresh()
    }

    fun onStart() = map.onStart()
    fun onStop() = map.onStop()
    fun destroy() = map.destroy()
}
