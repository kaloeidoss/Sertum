package com.sertum.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sertum.player.ui.SertumApp
import com.sertum.player.ui.theme.SertumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SertumTheme {
                SertumApp()
            }
        }
    }
}
