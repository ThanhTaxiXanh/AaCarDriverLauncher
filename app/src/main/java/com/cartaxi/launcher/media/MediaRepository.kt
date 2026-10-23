package com.cartaxi.launcher.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState

/** Đọc & điều khiển trình phát nhạc đang chạy trên điện thoại (Spotify, YouTube Music, ...). */
object MediaRepository {
    @Volatile var title = ""
        private set
    @Volatile var artist = ""
        private set
    @Volatile var isPlaying = false
        private set
    @Volatile var hasAccess = false
        private set

    var onChanged: (() -> Unit)? = null

    private var controller: MediaController? = null
    private var registered = false

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { pick(it) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = update()
        override fun onPlaybackStateChanged(state: PlaybackState?) = update()
    }

    /** An toàn khi gọi nhiều lần; trả về false nếu chưa được cấp quyền truy cập thông báo. */
    fun connect(context: Context) {
        if (registered) return
        val cn = ComponentName(context.applicationContext, MediaListenerService::class.java)
        try {
            val m = context.applicationContext.getSystemService(MediaSessionManager::class.java)
            m.addOnActiveSessionsChangedListener(sessionsListener, cn)
            hasAccess = true
            registered = true
            pick(m.getActiveSessions(cn))
        } catch (e: SecurityException) {
            hasAccess = false
        }
    }

    private fun pick(list: List<MediaController>?) {
        val c = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list?.firstOrNull()
        if (c != null && controller != null && c.sessionToken == controller?.sessionToken) {
            update(); return
        }
        controller?.unregisterCallback(callback)
        controller = c
        c?.registerCallback(callback)
        update()
    }

    private fun update() {
        val md = controller?.metadata
        title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        isPlaying = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
        onChanged?.invoke()
    }

    fun playPause() {
        val c = controller ?: return
        if (isPlaying) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
}
