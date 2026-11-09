package org.mz.botanicalsmock

import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.mz.botanicalsmock.model.DemoStorage
import org.mz.botanicalsmock.model.DemoStore
import org.jetbrains.compose.resources.painterResource
import org.mz.botanicalsmock.generated.resources.Res
import org.mz.botanicalsmock.generated.resources.mz_brand

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
    val sizing = runCatching {
        val configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
        desktopWindowSizing(
            configuration.bounds.width - insets.left - insets.right,
            configuration.bounds.height - insets.top - insets.bottom,
        )
    }.getOrElse { desktopWindowSizing(1280, 800) }
    application {
        val state = rememberWindowState(
            width = sizing.width.dp,
            height = sizing.height.dp,
            position = androidx.compose.ui.window.WindowPosition(Alignment.Center),
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "MZ Botanicals Mock — Offline demo",
            state = state,
            icon = painterResource(Res.drawable.mz_brand),
        ) {
            LaunchedEffect(window) {
                window.minimumSize = Dimension(sizing.minimumWidth, sizing.minimumHeight)
            }
            MockApp(store)
        }
    }
}
