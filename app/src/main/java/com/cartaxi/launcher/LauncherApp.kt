package com.cartaxi.launcher

import android.app.Application
import com.cartaxi.launcher.data.ConfigRepository
import com.mapbox.maps.MapboxOptions

class LauncherApp : Application() {
    lateinit var configRepository: ConfigRepository
        private set

    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
        configRepository = ConfigRepository(this)
    }
}
