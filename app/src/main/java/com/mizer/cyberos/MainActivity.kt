package com.mizer.cyberos

import android.content.Intent
import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object Signals {
    val home = mutableIntStateOf(0)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        setContent { CyberApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Нажатие кнопки «Домой»: закрываем настройки и поиск
        Signals.home.intValue++
    }
}

@Composable
fun CyberApp() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx.applicationContext) }
    val cfg by store.flow.collectAsState(initial = null)
    var intro by rememberSaveable { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        if (intro == null) intro = store.shouldShowIntro()
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = cfg?.acc ?: Color.Cyan)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val c = cfg
            if (c != null && intro != null) {
                Home(store, c)
                AnimatedVisibility(visible = intro == true, exit = fadeOut()) {
                    Intro(c.acc) { intro = false }
                }
            }
        }
    }
}
