package com.agentbook.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.agentbook.pro.ui.screens.MainScreen
import com.agentbook.pro.ui.theme.AgentBookProTheme
import com.agentbook.pro.ui.theme.NeonBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AgentBookProTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NeonBackground)
                ) {
                    MainScreen()
                }
            }
        }
    }
}
