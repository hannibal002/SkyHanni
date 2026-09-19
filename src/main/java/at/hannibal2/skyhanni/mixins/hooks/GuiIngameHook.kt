package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

object GuiIngameHook {

    @JvmStatic
    fun drawString(
        renderer: Font,
        drawContext: GuiGraphicsExtractor,
        text: Component,
        x: Int,
        y: Int,
        color: Int,
        shadow: Boolean,
    ) = ScoreboardData.tryToReplaceScoreboardLine(text).let {
        drawContext.text(renderer, it, x, y, color, shadow)
    }

}
