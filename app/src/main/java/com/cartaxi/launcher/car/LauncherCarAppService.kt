package com.cartaxi.launcher.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class LauncherCarAppService : CarAppService() {
    // Cho phép mọi host: phù hợp khi cài APK thủ công. Nếu phát hành lên Play, hãy dùng allowlist.
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = LauncherSession()
}
