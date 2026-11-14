package io.github.jesseracine.interstitialdiary

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// Replaces the MainActivity that `tauri android init` generates (the build workflow copies this file over it).
//
// Android draws the app under the status bar, the camera cutout and the navigation bar ("edge to edge"). The web app
// knows nothing about those, so its header ended up under the clock. Here the web view is pushed in by exactly the
// space those take, and by the keyboard when it is open, and the strip behind the bars gets the web app's background
// colour so it looks like part of the page.
class MainActivity : TauriActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    val content = findViewById<View>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
      val taken = insets.getInsets(
        WindowInsetsCompat.Type.systemBars() or
          WindowInsetsCompat.Type.displayCutout() or
          WindowInsetsCompat.Type.ime()
      )
      view.setPadding(taken.left, taken.top, taken.right, taken.bottom)
      WindowInsetsCompat.CONSUMED
    }
    paintBackground()
  }

  // The manifest keeps the activity alive when the phone switches between light and dark, so repaint here.
  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    paintBackground()
  }

  // The web app's --bg colours (index.css): light and dark, following the phone's setting like the status bar icons do.
  private fun paintBackground() {
    val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    window.decorView.setBackgroundColor(Color.parseColor(if (night) "#161513" else "#f7f5f1"))
  }
}
