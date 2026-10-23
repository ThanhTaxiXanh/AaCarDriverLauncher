package com.cartaxi.launcher.ui

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.widget.FrameLayout
import com.cartaxi.launcher.telemetry.OsmRepository
import com.cartaxi.launcher.telemetry.SignKind
import com.cartaxi.launcher.telemetry.TelemetryRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location

/** Bản đồ Mapbox 3D (Standard style, pitch 60°) bám theo xe + vẽ biển báo lên bản đồ. */
class MapController(private val context: Context, container: FrameLayout) {
    private val mapView = MapView(context, MapInitOptions(context, textureView = true))
    private var manager: PointAnnotationManager? = null
    private var shownVersion = -1
    private val iconCache = HashMap<String, Bitmap>()
    private val consumer: (Location) -> Unit = { follow(it) }

    init {
        container.addView(mapView, FrameLayout.LayoutParams(MATCH, MATCH))
        mapView.mapboxMap.loadStyle(Style.STANDARD) { onStyleReady() }
        TelemetryRepository.addConsumer(consumer)
    }

    private fun onStyleReady() {
        mapView.location.updateSettings {
            enabled = true
            puckBearingEnabled = true
            puckBearing = PuckBearing.COURSE
            locationPuck = createDefault2DPuck(withBearing = true)
        }
        mapView.mapboxMap.setCamera(CameraOptions.Builder().zoom(16.5).pitch(60.0).build())
        manager = mapView.annotations.createPointAnnotationManager().also { it.iconAllowOverlap = true }
        shownVersion = -1
    }

    private fun follow(loc: Location) {
        val kmh = loc.speed * 3.6f
        val zoom = when {
            kmh < 30f -> 17.5
            kmh < 60f -> 16.8
            else -> 16.0
        }
        val builder = CameraOptions.Builder()
            .center(Point.fromLngLat(loc.longitude, loc.latitude))
            .zoom(zoom)
            .pitch(60.0)
            .padding(EdgeInsets(mapView.height * 0.4, 0.0, 0.0, 0.0))
        if (loc.hasBearing() && loc.speed > 1.5f) builder.bearing(loc.bearing.toDouble())
        mapView.camera.easeTo(builder.build(), mapAnimationOptions { duration(900) })
    }

    /** Gọi mỗi giây. */
    fun refresh() {
        val m = manager ?: return
        if (shownVersion == OsmRepository.version) return
        shownVersion = OsmRepository.version
        m.deleteAll()
        val size = context.dp(34)
        val options = OsmRepository.signs.map { s ->
            val key = "${s.kind}:${s.value}"
            val bmp = iconCache.getOrPut(key) {
                when (s.kind) {
                    SignKind.SPEED -> SignRenderer.speed(s.value, size)
                    SignKind.CAMERA -> SignRenderer.camera(size)
                    SignKind.WARNING -> SignRenderer.warning(size)
                }
            }
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(s.lon, s.lat))
                .withIconImage(bmp)
        }
        if (options.isNotEmpty()) m.create(options)
    }

    fun onStart() = mapView.onStart()
    fun onStop() = mapView.onStop()

    fun destroy() {
        TelemetryRepository.removeConsumer(consumer)
        mapView.onDestroy()
    }
}
