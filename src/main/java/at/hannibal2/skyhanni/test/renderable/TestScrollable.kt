package at.hannibal2.hanni.test.renderable

import at.hannibal2.hanni.data.model.TextInput
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.ScrollValue
import at.hannibal2.hanni.utils.renderables.primitives.text

@HanniModule(devOnly = true)
object TestScrollable : RenderableTestSuite.TestRenderable("scrollable") {

    private val scroll = ScrollValue()
    private val text = TextInput()

    override fun renderable() = Renderable.searchableScrollable(
        table = with(Renderable) {
            mapOf(
                Pair(listOf(text("Help"), text("ME"), text("NOW!")), "HELP"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm not ok"), text("how are you?")), "NOT OK"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm ok"), text("how are you?")), "OK"),
                Pair(listOf(text("I'm last"), text("where are you?")), "LAST"),
            )
        },
        lines = 5,
        key = 0,
        velocity = 2.0,
        scrollValue = scroll,
        textInput = text,
        showScrollableTipsInList = true,
        asTable = true,
    )
}
