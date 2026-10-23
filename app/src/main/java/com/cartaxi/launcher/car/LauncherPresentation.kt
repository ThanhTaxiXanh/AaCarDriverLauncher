package com.cartaxi.launcher.car

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import com.cartaxi.launcher.data.LauncherConfig
import com.cartaxi.launcher.ui.LauncherUi

/** Cửa sổ chứa View (MapView + widget) hiển thị trên VirtualDisplay gắn với Surface của Android Auto. */
class LauncherPresentation(
    outer: Context,
    display: Display,
    private val config: LauncherConfig
) : Presentation(outer, display) {
    private lateinit var ui: LauncherUi
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            ui.refresh()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = LauncherUi(context, config)
        setContentView(ui.root)
    }

    override fun onStart() {
        super.onStart()
        ui.onStart()
        handler.post(tick)
    }

    override fun onStop() {
        handler.removeCallbacks(tick)
        ui.onStop()
        super.onStop()
    }

    fun release() {
        handler.removeCallbacks(tick)
        runCatching { dismiss() }
        if (::ui.isInitialized) ui.destroy()
    }
}
