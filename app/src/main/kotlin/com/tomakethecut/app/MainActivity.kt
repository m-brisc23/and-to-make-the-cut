package com.tomakethecut.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tomakethecut.app.navigation.ToMakeTheCutNavHost
import com.tomakethecut.core.ui.theme.ToMakeTheCutTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single-activity architecture: screens are Compose destinations, not Activities. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ToMakeTheCutTheme {
                ToMakeTheCutNavHost()
            }
        }
    }
}
