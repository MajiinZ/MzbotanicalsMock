package org.mz.botanicalsmock

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.mz.botanicalsmock.model.DemoStorage
import org.mz.botanicalsmock.model.DemoStore

private class DesktopDemoStorage : DemoStorage {
    private val directory = Path.of(System.getProperty("user.home"), ".mzbotanicals-mock")
    private val file = directory.resolve("demo-state-v1.json")
    override fun load(): String? = if (Files.exists(file)) Files.readString(file) else null
    override fun save(value: String) {
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, "demo-state-", ".tmp")
        try {
            Files.writeString(temporary, value)
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally { Files.deleteIfExists(temporary) }
    }
}

fun main() {
    val store = DemoStore(DesktopDemoStorage(), System::currentTimeMillis)
    application {
        val state = rememberWindowState(width = 1180.dp, height = 800.dp, position = androidx.compose.ui.window.WindowPosition(Alignment.Center))
        Window(onCloseRequest = ::exitApplication, title = "MZ Botanicals Mock — Offline demo", state = state) {
            window.minimumSize = Dimension(420, 560)
            MockApp(store)
        }
    }
}
