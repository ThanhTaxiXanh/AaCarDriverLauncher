package com.cartaxi.launcher.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

/** Tốc độ, quãng đường, vận tốc trung bình, trạng thái GPS. */
object TelemetryRepository : LocationListener {
    @Volatile var speedKmh = 0f
        private set
    @Volatile var accuracyM = 0f
        private set
    @Volatile var distanceM = 0.0
        private set
    @Volatile var avgKmh = 0f
        private set
    @Volatile var lastFixTime = 0L
        private set

    val hasFix: Boolean get() = System.currentTimeMillis() - lastFixTime < 5_000

    private var movingSeconds = 0.0
    private var last: Location? = null
    private var started = false
    private var manager: LocationManager? = null
    private val consumers = CopyOnWriteArrayList<(Location) -> Unit>()

    fun addConsumer(c: (Location) -> Unit) { consumers.add(c) }
    fun removeConsumer(c: (Location) -> Unit) { consumers.remove(c) }

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (started) return
        val lm = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
            manager = lm
            started = true
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    fun stop() {
        if (!started) return
        manager?.removeUpdates(this)
        started = false
    }

    fun resetTrip() {
        distanceM = 0.0
        movingSeconds = 0.0
        avgKmh = 0f
    }

    override fun onLocationChanged(loc: Location) {
        lastFixTime = System.currentTimeMillis()
        accuracyM = loc.accuracy
        val kmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
        speedKmh = if (kmh < 1f) 0f else kmh

        last?.let { prev ->
            val dt = (loc.time - prev.time) / 1000.0
            if (dt in 0.1..10.0 && loc.accuracy <= 30f && loc.speed > 0.8f) {
                distanceM += prev.distanceTo(loc)
                movingSeconds += dt
                if (movingSeconds > 0) avgKmh = (distanceM / movingSeconds * 3.6).toFloat()
            }
        }
        last = loc

        OsmRepository.onLocation(loc)
        consumers.forEach { it(loc) }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
