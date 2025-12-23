package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

object GuiIngameHook {

    @JvmStatic
    fun drawString(
        renderer: Font,
        drawContext: GuiGraphics,
        text: Component,
        x: Int,
        y: Int,
        color: Int,
    ) = ScoreboardData.tryToReplaceScoreboardLine(text.formattedTextCompatLessResets()).let {
        drawContext.drawString(renderer, it, x, y, color, false)
    }

}
