package com.cartaxi.launcher.data

import android.content.Context
import android.content.SharedPreferences

/** Lưu cấu hình vào SharedPreferences. App điện thoại và dịch vụ Android Auto chạy chung process nên dùng chung. */
class ConfigRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("launcher_config", Context.MODE_PRIVATE)

    fun load(): LauncherConfig = LauncherConfig.fromJson(prefs.getString(KEY, null))

    fun save(config: LauncherConfig) {
        prefs.edit().putString(KEY, config.toJson()).apply()
    }

    /** Giữ tham chiếu tới giá trị trả về, gọi close() để huỷ đăng ký. */
    fun observe(onChange: (LauncherConfig) -> Unit): AutoCloseable {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY) onChange(load())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return AutoCloseable { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val KEY = "config_json"
    }
}
