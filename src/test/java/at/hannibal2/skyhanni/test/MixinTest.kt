package at.hannibal2.skyhanni.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.MixinEnvironment.Option
import org.spongepowered.asm.mixin.transformer.IMixinTransformer

/**
 * Audits mixins to ensure their validity without launching a full Minecraft client.
 * Implementation inspired by [Skyblocker](https://github.com/SkyblockerMod/Skyblocker).
 */
class MixinTest {

    @Test
    fun `mixins load successfully`() {
        val environment = MixinEnvironment.getCurrentEnvironment()
        Assertions.assertInstanceOf(IMixinTransformer::class.java, environment.activeTransformer)
        // Fabric Loader enables refmap remapping in development, which makes Mixin retry failed
        // target selection with the method descriptor stripped. Production launches do no such
        // thing, so disable it to match their strictness — otherwise a selector whose descriptor
        // does not exist on this Minecraft version still resolves by name alone and the audit
        // passes on a mixin that cannot apply in production.
        environment.setOption(Option.REFMAP_REMAP, false)
        environment.audit()
    }
}
