package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ExtendedChatColor
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.split
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.transpose
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.WrappedStringRenderable
import net.minecraft.client.Minecraft
import java.awt.Color

@SkyHanniModule(devOnly = true)
object TestWrappedString : RenderableTestSuite.TestRenderable("wrapped_string") {

    override fun renderable(): Renderable {
        val testString = mapOf(
            "§r§b§lI'm §ccool §4and §7color§dful." to 30,
            "I'm am very long text without formating at all, so do not mind me here. I'm definitely fine, at do not break at all." to 70,
            "IDONotUseSpaceBecauseICanSoIWillLookAwfulWhenSplit" to 45,
            "12345§a67890§bABCDE" to 40,
            ExtendedChatColor(Color(0, 50, 100).rgb).toString() + "I'm colored with a Extended Color so look out!" to 50,
            ExtendedChatColor(
                Color(0, 0, 100, 50).rgb,
                true,
            ).toString() + "I'm colored with a Transparent Extended Color so I'm hard to see." to 50,
            "§lI'm bold and " + ExtendedChatColor(Color(0, 50, 100).rgb).toString() + "I'm custom colored" to 70,
            "§lI'm §kbold §oand " + ExtendedChatColor(Color(0, 50, 100).rgb).toString() + "I'm custom colored" to 60,
            "§oHello " + ExtendedChatColor(Color(0, 50, 100).rgb).toString() + "my §lold §rfriend." to 35,
            "§lThisIsBoldTextThatWillBeWrapped§rAndReset" to 48,
            "§nUnderlinedAnd§mStrikethrough§rDone" to 48,
            "§2Green §lBoldStartsHere" to 20,
            "§eYellow§5Purple§9BlueNoSpacesHere" to 30,
            "Start§dPink§rNormalAgainAfterReset" to 60,
            "§1§2§3§4EdgeCase§rText" to 16,
            "Split here §bblue text continues" to 40,
            "Multiple   spaces §cred  here" to 60,
            "Line1 with text\n§dLine2 pink text" to 120,
            "This is §2greenThen§rNormal with break" to 40,
            "End with code §5 " to 40,
            "§eYellow §lBold§r normal" to 20,
        ).asSequence().sortedBy { Minecraft.getMinecraft().fontRendererObj.getStringWidth(it.key) }.toList()


        val render = Renderable.table(
            testString.split(3).transpose().map { list ->
                list.mapNotNull {
                    if (it == null) null else
                        Renderable.table(
                            listOf(
                                listOf(StringRenderable("Orignal:"), StringRenderable(it.key)),
                                listOf(
                                    Renderable.placeholder(0, 0),
                                    Renderable.fixedSizeLine(
                                        StringRenderable(
                                            "Limited Width",
                                            horizontalAlign = HorizontalAlignment.CENTER,
                                        ),
                                        it.value,
                                    )
                                        .renderBounds(),
                                ),
                                listOf(StringRenderable("Wrapped:"), WrappedStringRenderable(it.key, it.value).renderBounds()),
                            ),
                        )
                }
            },
        )
        return render
    }
}
