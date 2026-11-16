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
    removeSavedSite() // before the web view exists
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

  // Earlier builds of the web app saved a copy of the site in the web view (a "service worker" and its cache) so that it
  // could work offline. Here the whole site is already inside the app, so that copy only hides new versions: the old one
  // is served first, and it can never be replaced, because the web view's update check for it goes to the real network
  // instead of through the app. The web app no longer creates one (src/lib/nativeApp.ts); this deletes any left behind
  // by an older build. Only that folder: the journal's own database and everything else stay. It runs once per process,
  // before the first web view (a later activity restart in the same process would find the folder in use). Errors are
  // ignored: a copy that cannot be removed is no worse than before, and a missing folder is the normal case.
  private fun removeSavedSite() {
    if (savedSiteChecked) return
    savedSiteChecked = true
    try {
      java.io.File(applicationInfo.dataDir, "app_webview/Default/Service Worker").deleteRecursively()
    } catch (ignored: Exception) {
      // see above: nothing to do
    }
  }

  companion object {
    private var savedSiteChecked = false
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
