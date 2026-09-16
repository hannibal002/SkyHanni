package at.hannibal2.skyhanni.test

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MinecraftTestBootstrapExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        synchronized(lock) {
            if (bootstrapped) return

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
