package com.cartaxi.launcher.telemetry

import android.location.Location
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class SignKind { SPEED, CAMERA, WARNING }

data class RoadSign(val lat: Double, val lon: Double, val kind: SignKind, val value: Int = 0)

/**
 * Lấy dữ liệu OpenStreetMap qua Overpass API:
 *  - tốc độ giới hạn của đoạn đường hiện tại (tag maxspeed)
 *  - biển giới hạn tốc độ, camera tốc độ, biển cảnh báo/gờ giảm tốc quanh vị trí
 * Dữ liệu OSM tại Việt Nam có thể chưa đầy đủ.
 */
object OsmRepository {
    @Volatile var currentLimit: Int? = null
        private set
    @Volatile var signs: List<RoadSign> = emptyList()
        private set
    @Volatile var version: Int = 0
        private set

    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)

    private var lastLimitLoc: Location? = null
    private var lastLimitTime = 0L
    private var lastSignLoc: Location? = null
    private var lastSignTime = 0L

    /** Gọi trên main thread mỗi lần có vị trí mới. */
    fun onLocation(loc: Location) {
        val now = System.currentTimeMillis()
        if (busy.get()) return
        val limitDue = lastLimitLoc.let { it == null || (it.distanceTo(loc) > 60f && now - lastLimitTime > 5_000) } ||
            now - lastLimitTime > 30_000
        val signDue = lastSignLoc.let { it == null || it.distanceTo(loc) > 400f } ||
            now - lastSignTime > 120_000
        if (!limitDue && !signDue) return

        if (limitDue) { lastLimitLoc = Location(loc); lastLimitTime = now }
        if (signDue) { lastSignLoc = Location(loc); lastSignTime = now }
        val lat = loc.latitude
        val lon = loc.longitude
        busy.set(true)
        executor.execute {
            try {
                if (limitDue) fetchLimit(lat, lon)
                if (signDue) fetchSigns(lat, lon)
            } finally {
                busy.set(false)
            }
        }
    }

    private fun fetchLimit(lat: Double, lon: Double) {
        val json = query("""[out:json][timeout:8];way(around:25,$lat,$lon)["highway"]["maxspeed"];out tags 5;""")
            ?: return
        val els = json.optJSONArray("elements")
        var found: Int? = null
        if (els != null) {
            for (i in 0 until els.length()) {
                val v = parseSpeed(els.getJSONObject(i).optJSONObject("tags")?.optString("maxspeed"))
                if (v != null) { found = v; break }
            }
        }
        currentLimit = found
    }

    private fun fetchSigns(lat: Double, lon: Double) {
        val s = lat - 0.01
        val n = lat + 0.01
        val w = lon - 0.012
        val e = lon + 0.012
        val bbox = "($s,$w,$n,$e)"
        val ql = """[out:json][timeout:10];(
            node["traffic_sign"="maxspeed"]["maxspeed"]$bbox;
            node["highway"="speed_camera"]$bbox;
            node["hazard"]$bbox;
            node["traffic_calming"~"bump|hump|table"]$bbox;
        );out body 120;"""
        val json = query(ql) ?: return
        val els = json.optJSONArray("elements") ?: return
        val list = ArrayList<RoadSign>()
        for (i in 0 until els.length()) {
            val o = els.getJSONObject(i)
            if (o.optString("type") != "node") continue
            val tags = o.optJSONObject("tags") ?: continue
            val kind: SignKind
            var value = 0
            if (tags.optString("highway") == "speed_camera") {
                kind = SignKind.CAMERA
            } else if (tags.has("maxspeed")) {
                kind = SignKind.SPEED
                value = parseSpeed(tags.optString("maxspeed")) ?: continue
            } else {
                kind = SignKind.WARNING
            }
            list.add(RoadSign(o.getDouble("lat"), o.getDouble("lon"), kind, value))
        }
        signs = list
        version++
    }

    private fun parseSpeed(s: String?): Int? {
        if (s.isNullOrBlank()) return null
        val n = Regex("\\d+").find(s)?.value?.toIntOrNull() ?: return null
        return if (s.contains("mph", ignoreCase = true)) (n * 1.609).toInt() else n
    }

    private fun query(ql: String): JSONObject? = try {
        val conn = (URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "CarDriverLauncher/1.0")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        conn.outputStream.use { out: OutputStream ->
            out.write(("data=" + URLEncoder.encode(ql, "UTF-8")).toByteArray())
        }
        if (conn.responseCode == 200) JSONObject(conn.inputStream.bufferedReader().use { it.readText() }) else null
    } catch (e: Exception) {
        null
    }
}
