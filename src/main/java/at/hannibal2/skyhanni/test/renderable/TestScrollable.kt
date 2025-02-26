package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue

@SkyHanniModule(devOnly = true)
object TestScrollable : RenderableTestSuit.TestRenderable("scrollable") {

    private val scroll = ScrollValue()
    private val text = TextInput()

    override fun renderable() = Renderable.searchableScrollable(
        table = mapOf(
            Pair(listOf(Renderable.string("Help"), Renderable.string("ME"), Renderable.string("NOW!")), "HELP"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm not ok"), Renderable.string("how are you?")), "NOT OK"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm ok"), Renderable.string("how are you?")), "OK"),
            Pair(listOf(Renderable.string("I'm last"), Renderable.string("where are you?")), "LAST"),
        ),
        lines = 5,
        key = 0,
        velocity = 2.0,
        scrollValue = scroll,
        textInput = text,
        showScrollableTipsInList = true,
        asTable = true,
    )
}
