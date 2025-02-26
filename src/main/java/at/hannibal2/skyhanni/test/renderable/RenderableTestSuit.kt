package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds

@SkyHanniModule(devOnly = true)
object RenderableTestSuit {

    private val register = mutableMapOf<String, TestRenderable>()

    private val storage get() = SkyHanniMod.feature.storage

    private val active = mutableSetOf<TestRenderable>()

    @HandleEvent
    fun onGuiRender(event: GuiRenderEvent.GuiOnTopRenderEvent) {
        for (test in active) {
            test.position.renderRenderable(test.renderable()?.renderBounds(LorenzColor.RED.addOpacity(50)), posLabel = "Test: $test")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shrenderable") {
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                val input = it.joinToString(" ")
                if (input.isBlank()) {
                    ChatUtils.userError("No Argument provided")
                }
                val test = register[input]
                if (test == null) {
                    ChatUtils.userError("Unknown Test '$input'")
                    return@callback
                }
                if (active.contains(test)) {
                    ChatUtils.chat("Test '$input' is now §cdisabled§e.")
                    active.remove(test)
                    return@callback
                }
                ChatUtils.chat("Test '$input' is now §aactive§e.")
                active.add(test)
                return@callback
            }
            autoComplete { args ->
                val input = args.joinToString(" ")
                register.keys.filter { it.startsWith(input) }
            }
        }
    }

    /**
     * How to use:
     * Declare an object with [RenderableTestSuit.TestRenderable] as supertype
     * Annotate the object with "@SkyHanniModule(devOnly = true)"
     * Give it a lowercase [name] (this is used to call it via the command later on)
     * Define your test with [renderable] function
     *
     * Ingame call it with "\shrenderable [name]" to show it (calling it again will disable it)
     */
    abstract class TestRenderable(val name: String) {

        abstract fun renderable(): Renderable?

        val position: Position get() = storage.testRenderablePositions.getOrPut(name) { Position(20, 20) }

        init {
            register[name] = this
        }

        final override fun equals(other: Any?): Boolean {
            if (other !is TestRenderable) return false
            return other.name == this.name
        }

        final override fun hashCode(): Int = name.hashCode()

        override fun toString(): String = name
    }
}
