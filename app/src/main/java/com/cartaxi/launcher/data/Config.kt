package com.cartaxi.launcher.data

import org.json.JSONArray
import org.json.JSONObject

/** HORIZONTAL = chia Trái/Phải (bản đồ bên trái). VERTICAL = chia Trên/Dưới (bản đồ ở trên). */
enum class SplitMode { HORIZONTAL, VERTICAL }

enum class WidgetType(val label: String) {
    NONE("Trống"),
    LUNAR("Lịch Âm Dương"),
    IMAGE("Hình ảnh"),
    MEDIA("Media"),
    TELEMETRY("Telemetry")
}

data class GridCell(
    val type: WidgetType = WidgetType.NONE,
    val images: List<String> = emptyList()
)

data class LauncherConfig(
    val mode: SplitMode = SplitMode.HORIZONTAL,
    val mapPercent: Int = 60,
    val rows: Int = 2,
    val cols: Int = 1,
    val cells: List<GridCell> = listOf(GridCell(WidgetType.TELEMETRY), GridCell(WidgetType.LUNAR)),
    val slideSeconds: Int = 10
) {
    fun cellAt(row: Int, col: Int): GridCell = cells.getOrElse(row * cols + col) { GridCell() }

    fun withGrid(newRows: Int, newCols: Int): LauncherConfig {
        val r = newRows.coerceIn(1, MAX_GRID)
        val c = newCols.coerceIn(1, MAX_GRID)
        val newCells = List(r * c) { i ->
            val rr = i / c
            val cc = i % c
            if (rr < rows && cc < cols) cellAt(rr, cc) else GridCell()
        }
        return copy(rows = r, cols = c, cells = newCells)
    }

    fun withCell(index: Int, cell: GridCell): LauncherConfig =
        copy(cells = cells.toMutableList().also { if (index in it.indices) it[index] = cell })

    fun toJson(): String {
        val arr = JSONArray()
        cells.forEach { cell ->
            arr.put(
                JSONObject()
                    .put("type", cell.type.name)
                    .put("images", JSONArray(cell.images))
            )
        }
        return JSONObject()
            .put("mode", mode.name)
            .put("mapPercent", mapPercent)
            .put("rows", rows)
            .put("cols", cols)
            .put("slideSeconds", slideSeconds)
            .put("cells", arr)
            .toString()
    }

    companion object {
        const val MAX_GRID = 4

        fun fromJson(json: String?): LauncherConfig {
            if (json.isNullOrBlank()) return LauncherConfig()
            return try {
                val o = JSONObject(json)
                val rows = o.optInt("rows", 2).coerceIn(1, MAX_GRID)
                val cols = o.optInt("cols", 1).coerceIn(1, MAX_GRID)
                val arr = o.optJSONArray("cells") ?: JSONArray()
                val cells = List(rows * cols) { i ->
                    val c = arr.optJSONObject(i)
                    if (c == null) GridCell() else {
                        val imgs = c.optJSONArray("images")
                        GridCell(
                            type = runCatching { WidgetType.valueOf(c.optString("type")) }
                                .getOrDefault(WidgetType.NONE),
                            images = List(imgs?.length() ?: 0) { k -> imgs!!.getString(k) }
                        )
                    }
                }
                LauncherConfig(
                    mode = runCatching { SplitMode.valueOf(o.optString("mode")) }
                        .getOrDefault(SplitMode.HORIZONTAL),
                    mapPercent = o.optInt("mapPercent", 60).coerceIn(30, 80),
                    rows = rows,
                    cols = cols,
                    cells = cells,
                    slideSeconds = o.optInt("slideSeconds", 10).coerceIn(3, 120)
                )
            } catch (e: Exception) {
                LauncherConfig()
            }
        }
    }
}
