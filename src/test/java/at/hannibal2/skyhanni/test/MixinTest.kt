package at.hannibal2.skyhanni.test

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.transformer.IMixinTransformer

// Inspired by Skyblocker
class MixinTest {

    @Test
    fun `mixins load successfully`() {
        val environment = MixinEnvironment.getCurrentEnvironment()
        Assertions.assertInstanceOf(IMixinTransformer::class.java, environment.activeTransformer)
        environment.audit()
    }
}
