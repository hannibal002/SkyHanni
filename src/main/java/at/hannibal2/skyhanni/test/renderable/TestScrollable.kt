package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.ScrollValue

@SkyHanniModule(devOnly = true)
object TestScrollable : RenderableTestSuite.TestRenderable("scrollable") {

    private val scroll = ScrollValue()
    private val text = TextInput()

    override fun renderable() = Renderable.searchableScrollable(
        table = mapOf(
            Pair(listOf(RenderableString("Help"), RenderableString("ME"), RenderableString("NOW!")), "HELP"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm not ok"), RenderableString("how are you?")), "NOT OK"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm ok"), RenderableString("how are you?")), "OK"),
            Pair(listOf(RenderableString("I'm last"), RenderableString("where are you?")), "LAST"),
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
