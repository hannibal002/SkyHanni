package at.hannibal2.skyhanni.utils.system

import java.nio.file.Path

abstract class Platform {
    abstract val mcVersion: String
    abstract val isDevEnvironment: Boolean
    abstract val gameDir: Path
    abstract val configDir: Path
    abstract val dataDir: Path
    abstract val logsDir: Path

    abstract fun getLoadedMods(): List<ModInstance>

    abstract fun isModInstalled(modId: String): Boolean

    abstract fun shutdownMinecraft(reason: String? = null)
}
