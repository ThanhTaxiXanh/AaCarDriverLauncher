package com.cartaxi.launcher.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.cartaxi.launcher.LauncherApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = (application as LauncherApp).configRepository
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ConfigScreen(repo)
            }
        }
    }
}
