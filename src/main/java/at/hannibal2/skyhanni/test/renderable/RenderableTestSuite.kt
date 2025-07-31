package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds

@SkyHanniModule(devOnly = true)
object RenderableTestSuite {

    private val register = mutableMapOf<String, TestRenderable>()

    private val storage get() = SkyHanniMod.feature.storage

    private val active = mutableSetOf<TestRenderable>()

    @HandleEvent(GuiRenderEvent.GuiOnTopRenderEvent::class)
    fun onGuiRender() {
        for (test in active) {
            test.position.renderRenderable(test.finalRenderable, posLabel = "Renderable Test: $test")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestrenderable") {
            category = CommandCategory.DEVELOPER_TEST
            description = "Used for testing specific gui element primitives."
            arg("test", BrigadierArguments.greedyString()) { arg ->
                callback { testCommand(getArg(arg)) }
            }
            simpleCallback {
                ChatUtils.userError("No test name provided! Available tests: ${register.keys}")
            }
        }
    }

    private fun testCommand(input: String) {
        val test = register[input]
        if (test == null) {
            ChatUtils.userError("Unknown test '$input'! Available tests: ${register.keys}")
            return
        }
        if (active.contains(test)) {
            ChatUtils.chat("Renderable Test '$input' is now §cdisabled§e.")
            active.remove(test)
            return
        }
        ChatUtils.chat("Renderable Test '$input' is now §aactive§e.")
        active.add(test)
    }

    private val TestRenderable.finalRenderable: Renderable?
        get() = if (shouldRenderBounds) renderable()?.renderBounds(LorenzColor.RED.addOpacity(50)) else renderable()

    /**
     * Interface to define Test code for [Renderable]s.
     *
     * How to use:
     * Declare an object with [RenderableTestSuite.TestRenderable] as supertype.
     * Annotate the object with `@SkyHanniModule(devOnly = true)`.
     * Give it a lowercase [name] (this is used to call it via the command later on).
     * Define your test you want to do with the [renderable] function.
     *
     * Ingame call it with `\shrenderable [name]` to show it (calling it again will disable it)
     *
     * @param name Name of the Test, is the name you have to type to use it ingame with `\shrenderable`.
     * @param shouldRenderBounds Enables a Red Boundary around the test [renderable].
     * @property renderable Function that is called for retrieving the [Renderable] that is going to be tested (and rendered on screen, once active). Define here your test you want to do.
     * @property position The position at which the test will be rendered. There should be no reason that is touched. Other than inside [RenderableTestSuite].
     */
    abstract class TestRenderable(val name: String, val shouldRenderBounds: Boolean = true) {

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
