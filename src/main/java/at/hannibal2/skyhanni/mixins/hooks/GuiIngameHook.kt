package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.ScoreboardData
import net.minecraft.client.gui.Font

//#if MC > 1.21
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.GuiGraphics
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
//#endif

object GuiIngameHook {

    @JvmStatic
    fun drawString(
        renderer: Font,
        //#if MC < 1.21
        //$$ text: String,
        //#else
        drawContext: GuiGraphics,
        text: Component,
        //#endif
        x: Int,
        y: Int,
        color: Int,
//#if MC < 1.21
//$$     ) = ScoreboardData.tryToReplaceScoreboardLine(text)?.let {
        //#else
        ) = ScoreboardData.tryToReplaceScoreboardLine(text.formattedTextCompatLessResets())?.let {
        //#endif
        //#if MC < 1.21
        //$$ renderer.drawString(it, x, y, color)
        //#else
        drawContext.drawString(renderer, it, x, y, color, false)
        //#endif
    } ?: 0

}
