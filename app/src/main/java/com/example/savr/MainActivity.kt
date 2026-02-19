package com.example.savr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.savr.app.ui.SavrApp
import com.savr.app.ui.theme.SavrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SavrTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SavrApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
