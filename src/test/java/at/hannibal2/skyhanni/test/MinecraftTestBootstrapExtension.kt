package at.hannibal2.skyhanni.test

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Path

class MinecraftTestBootstrapExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        synchronized(lock) {
            if (bootstrapped) return

            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()

            mockkStatic(FabricLoader::class)
            val loader = mockk<FabricLoader>()
            every { FabricLoader.getInstance() } returns loader
            every { loader.isDevelopmentEnvironment } returns false
            val gameDir = Path.of("../versions/${SharedConstants.getCurrentVersion().name()}/run").toAbsolutePath()
            every { loader.gameDir } returns gameDir
            every { loader.configDir } returns gameDir.resolve("config")

            bootstrapped = true
        }
    }

    companion object {
        private val lock = Any()
        private var bootstrapped = false
    }
}
