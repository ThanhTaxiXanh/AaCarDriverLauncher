package com.cartaxi.launcher.car

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cartaxi.launcher.telemetry.TelemetryRepository

class LauncherSession : Session() {
    private val renderer by lazy { SurfaceRenderer(carContext) }

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                renderer.attach()
                ensureLocation()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                renderer.release()
                TelemetryRepository.stop()
            }
        })
    }

    override fun onCreateScreen(intent: Intent): Screen = MainCarScreen(carContext)

    private fun ensureLocation() {
        val perm = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(carContext, perm) == PackageManager.PERMISSION_GRANTED) {
            TelemetryRepository.start(carContext)
        } else {
            carContext.requestPermissions(listOf(perm)) { granted, _ ->
                if (granted.isNotEmpty()) TelemetryRepository.start(carContext)
            }
        }
    }
}
