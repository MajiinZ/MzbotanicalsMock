package org.mz.botanicalsmock

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.mz.botanicalsmock.model.DemoStorage
import org.mz.botanicalsmock.model.DemoStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashProvider ->
            val root = window.decorView as ViewGroup
            val artwork = ImageView(this).apply {
                setImageResource(R.drawable.splash_artwork)
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            root.addView(artwork, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
            ))
            splashProvider.remove()
            artwork.animate().alpha(0f).setDuration(650L)
                .withEndAction { root.removeView(artwork) }.start()
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("mzbotanicals-offline-demo", MODE_PRIVATE)
        val storage = object : DemoStorage {
            override fun load(): String? = preferences.getString("state-v1", null)
            override fun save(value: String) {
                check(preferences.edit().putString("state-v1", value).commit()) { "Your demo changes could not be saved." }
            }
        }
        val store = DemoStore(storage, System::currentTimeMillis)
        setContent { MockApp(store) }
    }
}
