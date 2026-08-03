package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.mixins.init.SkyHanniMixinPlugin
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
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
        environment.audit()
    }

    @Test
    fun `mixin discovery is successful`() {
        val classLoader = javaClass.classLoader
        val discovered = SkyHanniMixinPlugin().mixins
        Assertions.assertTrue(discovered.isNotEmpty()) {
            "Mixin discovery returned nothing, so this test would pass without inspecting a single mixin. " +
                "SkyHanniMixinPlugin resolves them relative to its own code source, " +
                "which the mixinTest classpath has to expose."
        }
        discovered.forEach { mixin ->
            val path = "$MIXIN_PACKAGE_PATH/${mixin.replace('.', '/')}.class"
            val bytes = checkNotNull(classLoader.getResourceAsStream(path)) {
                "Mixin $mixin was discovered but $path is not on the classpath"
            }.use { it.readBytes() }
            ClassNode().also { ClassReader(bytes).accept(it, ClassReader.SKIP_CODE) }
        }
    }

    companion object {
        private const val MIXIN_PACKAGE_PATH = "at/hannibal2/skyhanni/mixins/transformers"
    }
}
