package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.ScoreboardData
import net.minecraft.client.gui.FontRenderer

object GuiIngameHook {

    @JvmStatic
    fun drawString(
        instance: FontRenderer,
        text: String,
        x: Int,
        y: Int,
        color: Int,
    ) = ScoreboardData.tryToReplaceScoreboardLine(text)?.let {
        instance.drawString(it, x, y, color)
    } ?: 0

}
