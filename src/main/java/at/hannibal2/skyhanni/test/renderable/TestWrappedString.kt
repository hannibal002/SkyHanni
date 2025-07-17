package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule(devOnly = true)
object TestWrappedString : RenderableTestSuite.TestRenderable("wrapped_string") {

    override fun renderable(): Renderable {
        val testString = mapOf(
            "§r§b§lI'm §ccool §4and §7color§dful." to 30,
            "I'm am very long text without formating at all, so do not mind me here. I'm definitely fine, at do not break at all." to 70,
            "IDONotUseSpaceBecauseICanSoIWillLookAwfulWhenSplit" to 20,
            "12345§a67890§bABCDE" to 40,
            "§lThisIsBoldTextThatWillBeWrapped§rAndReset" to 48,
            "§nUnderlinedAnd§mStrikethrough§rDone" to 48,
            "§2Green §lBoldStartsHere" to 20,
            "§eYellow§5Purple§9BlueNoSpacesHere" to 30,
            "Start§dPink§rNormalAgainAfterReset" to 60,
            "§1§2§3§4EdgeCase§rText" to 16,
            "Split here §bblue text continues" to 40,
            "Multiple   spaces §cred  here" to 60,
            "Line1 with text\n§dLine2 pink text" to 80,
            "This is §2greenThenNormal without break" to 40,
            "End with code §5 " to 40,
            "§eYellow §lBold§r normal" to 20,
        )

        val render = with(Renderable) {
            testString.map {
                table(
                    listOf(
                        listOf(text("Orignal:"), text(it.key)),
                        listOf(
                            empty(),
                            fixedSizeLine(text("Limited Width", horizontalAlign = HorizontalAlignment.CENTER), it.value).renderBounds(),
                        ),
                        listOf(text("Wrapped:"), wrappedText(it.key, it.value).renderBounds()),
                    ),
                )
            }
        }
        return Renderable.vertical(render)
    }
}
