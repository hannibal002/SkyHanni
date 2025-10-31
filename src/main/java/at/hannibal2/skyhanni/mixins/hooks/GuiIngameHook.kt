package at.hannibal2.hanni.mixins.hooks

import at.hannibal2.hanni.data.ScoreboardData
import net.minecraft.client.gui.FontRenderer

//#if MC > 1.21
//$$ import net.minecraft.text.Text
//$$ import net.minecraft.client.gui.DrawContext
//$$ import at.hannibal2.hanni.utils.compat.formattedTextCompatLessResets
//#endif

object GuiIngameHook {

    @JvmStatic
    fun drawString(
        renderer: FontRenderer,
        //#if MC < 1.21
        text: String,
        //#else
        //$$ drawContext: DrawContext,
        //$$ text: Text,
        //#endif
        x: Int,
        y: Int,
        color: Int,
//#if MC < 1.21
    ) = ScoreboardData.tryToReplaceScoreboardLine(text)?.let {
        //#else
        //$$ ) = ScoreboardData.tryToReplaceScoreboardLine(text.formattedTextCompatLessResets())?.let {
        //#endif
        //#if MC < 1.21
        renderer.drawString(it, x, y, color)
        //#else
        //$$ drawContext.drawText(renderer, it, x, y, color, false)
        //#endif
    } ?: 0

}
