package at.hannibal2.skyhanni.test

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class BootstrapExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
    }
}
