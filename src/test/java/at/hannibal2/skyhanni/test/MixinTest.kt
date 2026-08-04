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
        // Fabric Loader enables refmap remapping in development, which makes Mixin retry failed
        // target selection with the method descriptor stripped. Production launches do no such
        // thing, so disable it to match their strictness — otherwise a selector whose descriptor
        // does not exist on this Minecraft version still resolves by name alone and the audit
        // passes on a mixin that cannot apply in production.
        environment.setOption(Option.REFMAP_REMAP, false)
        environment.audit()
    }

    /**
     * The audit above runs against the Minecraft jar Loom produced, whose local variable names are
     * invented by the remapper (`bl` for a boolean, `entityRenderState` for an `EntityRenderState`,
     * and so on). Obfuscated Minecraft ships every local as `$$0`, `$$1`, … instead, so a selector
     * matching a made-up development name resolves during the audit and silently injects nothing at
     * runtime. Only positional selectors survive the difference.
     */
    @Test
    fun `mixins do not select minecraft locals by name`() {
        Assumptions.assumeTrue(
            System.getProperty(OBFUSCATED_PROPERTY).toBoolean(),
            "Minecraft is not obfuscated on this version, so its local variable names match production.",
        )
        val offenders = mixinClasses().flatMap { it.nameBasedLocalSelectors() }
        Assertions.assertTrue(offenders.isEmpty()) {
            offenders.joinToString(
                prefix = "Selecting an obfuscated Minecraft local by name only works in development. " +
                    "Use `index` or `ordinal` instead:\n  - ",
                separator = "\n  - ",
            )
        }
    }

    private fun mixinClasses(): List<ClassNode> {
        val classLoader = javaClass.classLoader
        val discovered = SkyHanniMixinPlugin().mixins
        Assertions.assertTrue(discovered.isNotEmpty()) {
            "Mixin discovery returned nothing, so this test would pass without inspecting a single mixin. " +
                "SkyHanniMixinPlugin resolves them relative to its own code source, " +
                "which the mixinTest classpath has to expose."
        }
        return discovered.map { mixin ->
            val path = "$MIXIN_PACKAGE_PATH/${mixin.replace('.', '/')}.class"
            val bytes = checkNotNull(classLoader.getResourceAsStream(path)) {
                "Mixin $mixin was discovered but $path is not on the classpath"
            }.use { it.readBytes() }
            ClassNode().also { ClassReader(bytes).accept(it, ClassReader.SKIP_CODE) }
        }
    }

    private fun ClassNode.nameBasedLocalSelectors(): List<String> {
        if (mixinTargets().none { it.startsWith("net.minecraft.") || it.startsWith("com.mojang.blaze3d.") }) {
            return emptyList()
        }
        return methods.flatMap { method ->
            val selectors = method.annotations().selectedNames(MODIFY_VARIABLE_ANNOTATION, "@ModifyVariable") +
                method.parameterAnnotations().selectedNames(LOCAL_ANNOTATION, "@Local")
            selectors.map { "${name.substringAfterLast('/')}.${method.name}${method.desc}: $it" }
        }
    }

    private fun ClassNode.mixinTargets(): List<String> {
        val mixin = annotations().firstOrNull { it.desc == MIXIN_ANNOTATION } ?: return emptyList()
        return mixin.arrayValue("value").filterIsInstance<Type>().map { it.className } +
            mixin.arrayValue("targets").filterIsInstance<String>().map { it.replace('/', '.') }
    }

    private fun List<AnnotationNode>.selectedNames(descriptor: String, display: String): List<String> =
        filter { it.desc == descriptor }
            .flatMap { it.arrayValue("name").filterIsInstance<String>() }
            .map { "$display(name = \"$it\")" }

    private fun AnnotationNode.arrayValue(key: String): List<*> {
        val values = values ?: return emptyList<Any>()
        val index = values.indices.step(2).firstOrNull { values[it] == key } ?: return emptyList<Any>()
        return (values[index + 1] as? List<*>).orEmpty()
    }

    private fun ClassNode.annotations(): List<AnnotationNode> =
        visibleAnnotations.orEmpty() + invisibleAnnotations.orEmpty()

    private fun MethodNode.annotations(): List<AnnotationNode> =
        visibleAnnotations.orEmpty() + invisibleAnnotations.orEmpty()

    private fun MethodNode.parameterAnnotations(): List<AnnotationNode> =
        (visibleParameterAnnotations.orEmpty().asList() + invisibleParameterAnnotations.orEmpty().asList())
            .filterNotNull()
            .flatten()

    companion object {
        private const val OBFUSCATED_PROPERTY = "skyhanni.minecraftIsObfuscated"
        private const val MIXIN_PACKAGE_PATH = "at/hannibal2/skyhanni/mixins/transformers"
        private const val MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;"
        private const val MODIFY_VARIABLE_ANNOTATION = "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;"
        private const val LOCAL_ANNOTATION = "Lcom/llamalad7/mixinextras/sugar/Local;"
    }
}
