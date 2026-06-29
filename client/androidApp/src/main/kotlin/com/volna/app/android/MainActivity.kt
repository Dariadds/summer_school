package com.volna.app.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.volna.app.VolnaApp
import com.volna.app.core.storage.PlatformSessionStorage
import com.volna.app.map.PlatformMapLauncher

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformSessionStorage.initialize(applicationContext)
        PlatformMapLauncher.initialize(applicationContext)
        setContent {
            VolnaApp()
        }
    }
}
