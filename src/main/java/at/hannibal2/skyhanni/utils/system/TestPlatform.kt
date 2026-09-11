package at.hannibal2.skyhanni.utils.system

import java.nio.file.Path

class TestPlatform : Platform() {
    override val mcVersion: String = "unknown"

    override val isDevEnvironment: Boolean = false

    override val gameDir: Path =
        Path.of(System.getProperty("user.dir"))
            .toAbsolutePath()
            .normalize()

    override val configDir: Path =
        gameDir.resolve("config")

    override val dataDir: Path =
        gameDir.resolve("data")

    override val logsDir: Path =
        gameDir.resolve("logs")

    override fun getLoadedMods(): List<ModInstance> = emptyList()

    override fun isModInstalled(modId: String): Boolean = false

    override fun shutdownMinecraft(reason: String?) {
        error("Minecraft shutdown requested in a test: ${reason.orEmpty()}")
    }
}
