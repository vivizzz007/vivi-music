package com.music.vivi

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.ComponentActivity
import com.music.vivi.ui.crash.CrashPage

class CrashActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var crashData = intent.getStringExtra("Crash Data")
        setContent {
            CrashPage(crashData)
        }
    }
}