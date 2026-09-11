package at.hannibal2.skyhanni.test

import io.mockk.every
import io.mockk.mockk
import net.fabricmc.api.EnvType
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.fabricmc.loader.impl.game.GameProvider
import net.fabricmc.loader.impl.launch.FabricLauncher
import net.fabricmc.loader.impl.launch.FabricLauncherBase
import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Path

class MinecraftTestBootstrapExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        synchronized(lock) {
            if (bootstrapped) return

            if (FabricLauncherBase.getLauncher() == null) {
                val provider = mockk<GameProvider>()
                every { provider.builtinMods } returns emptyList()
                every { provider.launchDirectory } returns Path.of("run")

                val launcher = mockk<FabricLauncher>()
                every { launcher.environmentType } returns EnvType.CLIENT
                every { launcher.isDevelopment } returns false

                FabricLauncherBase.setLauncher(launcher)

                val loader = FabricLoaderImpl.INSTANCE
                loader.setGameProvider(provider)
            }

            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()

            bootstrapped = true
        }
    }

    companion object {
        private val lock = Any()
        private var bootstrapped = false
    }
}
