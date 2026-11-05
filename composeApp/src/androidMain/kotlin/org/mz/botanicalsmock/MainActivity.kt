package org.mz.botanicalsmock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.mz.botanicalsmock.model.DemoStorage
import org.mz.botanicalsmock.model.DemoStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
