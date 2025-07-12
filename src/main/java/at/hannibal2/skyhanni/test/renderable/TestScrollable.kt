package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.StringRenderable

@SkyHanniModule(devOnly = true)
object TestScrollable : RenderableTestSuite.TestRenderable("scrollable") {

    private val scroll = ScrollValue()
    private val text = TextInput()

    override fun renderable() = Renderable.searchableScrollable(
        table = mapOf(
            Pair(listOf(StringRenderable("Help"), StringRenderable("ME"), StringRenderable("NOW!")), "HELP"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm not ok"), StringRenderable("how are you?")), "NOT OK"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm ok"), StringRenderable("how are you?")), "OK"),
            Pair(listOf(StringRenderable("I'm last"), StringRenderable("where are you?")), "LAST"),
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
