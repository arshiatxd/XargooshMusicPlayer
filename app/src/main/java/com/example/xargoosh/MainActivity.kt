package com.example.xargoosh

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xargoosh.core.design.themes.XargooshTheme
import com.example.xargoosh.core.design.themes.ThemeManager
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : AppCompatActivity() {
  companion object {
    const val ACTION_OPEN_NOW_PLAYING = "com.xargoosh.music.action.OPEN_NOW_PLAYING"
  }

  private val openNowPlayingRequests = MutableStateFlow(0L)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleLaunchIntent(intent)

    ThemeManager.initialize(this)

    enableEdgeToEdge()
    setContent {
      val appTheme by ThemeManager.currentTheme.collectAsStateWithLifecycle()
      XargooshTheme(appTheme = appTheme) { 
          Surface(modifier = Modifier.fillMaxSize(), color = XargooshTheme.colors.background) { 
              val openNowPlayingRequest by openNowPlayingRequests.collectAsStateWithLifecycle()
              MainNavigation(openNowPlayingRequest = openNowPlayingRequest)
          } 
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleLaunchIntent(intent)
  }

  private fun handleLaunchIntent(intent: Intent?) {
    if (intent?.action == ACTION_OPEN_NOW_PLAYING) {
      openNowPlayingRequests.value += 1L
    }
  }
}
