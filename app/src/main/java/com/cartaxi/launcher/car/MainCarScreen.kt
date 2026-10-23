package com.cartaxi.launcher.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.cartaxi.launcher.R
import com.cartaxi.launcher.media.MediaRepository

/**
 * Màn hình chính: toàn bộ vùng hiển thị do SurfaceRenderer vẽ (bản đồ + widget).
 * Surface của Android Auto không nhận cảm ứng trực tiếp, nên nút điều khiển nhạc
 * nằm trên ActionStrip của template.
 */
class MainCarScreen(carContext: CarContext) : Screen(carContext) {
    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                MediaRepository.connect(carContext)
                MediaRepository.onChanged = { invalidate() }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                MediaRepository.onChanged = null
            }
        })
    }

    private fun icon(res: Int) =
        CarIcon.Builder(IconCompat.createWithResource(carContext, res)).setTint(CarColor.DEFAULT).build()

    override fun onGetTemplate(): Template {
        val strip = ActionStrip.Builder()
            .addAction(
                Action.Builder().setIcon(icon(R.drawable.ic_skip_prev))
                    .setOnClickListener { MediaRepository.previous() }.build()
            )
            .addAction(
                Action.Builder()
                    .setIcon(icon(if (MediaRepository.isPlaying) R.drawable.ic_pause else R.drawable.ic_play))
                    .setOnClickListener { MediaRepository.playPause() }.build()
            )
            .addAction(
                Action.Builder().setIcon(icon(R.drawable.ic_skip_next))
                    .setOnClickListener { MediaRepository.next() }.build()
            )
            .build()
        return NavigationTemplate.Builder().setActionStrip(strip).build()
    }
}
