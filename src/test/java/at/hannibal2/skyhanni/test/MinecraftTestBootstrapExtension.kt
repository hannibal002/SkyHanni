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
import java.lang.reflect.Field
import java.nio.file.Path

class MinecraftTestBootstrapExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        synchronized(lock) {
            if (bootstrapped) return

            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()

            if (!FabricLauncherBase.isMixinReady()) {
                launcherField.set(null, launcher)
                FabricLoaderImpl.INSTANCE.setGameProvider(provider)
            }

            bootstrapped = true
        }
    }

    companion object {
        private val lock = Any()
        private var bootstrapped = false

        private val provider by lazy {
            mockk<GameProvider>(relaxed = true) {
                every { builtinMods } returns emptyList()
                every { launchDirectory } returns Path.of(
                    SharedConstants.getCurrentVersion().name()
                )
            }
        }

        private val launcher by lazy {
            mockk<FabricLauncher>(relaxed = true) {
                every { environmentType } returns EnvType.CLIENT
                every { isDevelopment } returns false
            }
        }

        private val launcherField: Field by lazy {
            FabricLauncherBase::class.java
                .getDeclaredField("launcher")
                .apply { isAccessible = true }
        }
    }
}
