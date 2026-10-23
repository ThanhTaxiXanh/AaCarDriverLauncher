package com.cartaxi.launcher.car

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.cartaxi.launcher.LauncherApp

/**
 * Nhận Surface từ Android Auto -> tạo VirtualDisplay -> hiển thị Presentation chứa layout tuỳ biến.
 * Khi cấu hình trên điện thoại thay đổi, layout được dựng lại ngay.
 */
class SurfaceRenderer(private val carContext: CarContext) : SurfaceCallback {
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: LauncherPresentation? = null
    private var container: SurfaceContainer? = null
    private val main = Handler(Looper.getMainLooper())
    private val repo = (carContext.applicationContext as LauncherApp).configRepository
    private var observer: AutoCloseable? = null

    fun attach() {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
        observer = repo.observe { main.post { container?.let { build(it) } } }
    }

    fun release() {
        observer?.close()
        observer = null
        teardown()
        container = null
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        container = surfaceContainer
        build(surfaceContainer)
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        teardown()
        container = null
    }

    private fun build(sc: SurfaceContainer) {
        teardown()
        val surface = sc.surface ?: return
        if (sc.width <= 0 || sc.height <= 0) return
        val dm = carContext.getSystemService(DisplayManager::class.java)
        val vd = dm.createVirtualDisplay(
            "CarDriverLauncher",
            sc.width, sc.height, sc.dpi, surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
        )
        virtualDisplay = vd
        presentation = LauncherPresentation(carContext, vd.display, repo.load()).also { it.show() }
    }

    private fun teardown() {
        presentation?.release()
        presentation = null
        virtualDisplay?.release()
        virtualDisplay = null
    }
}
