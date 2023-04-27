package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import net.minecraft.client.gui.FontRenderer

private val piggyPattern = "Piggy: (?<coins>.*)".toPattern()

fun drawString(
    instance: FontRenderer,
    text: String,
    x: Int,
    y: Int,
    color: Int,
): Int {
    if (SkyHanniMod.feature.misc.hideScoreboardNumbers) {
        if (text.startsWith("§c") && text.length <= 4) {
            return 0
        }
    }
    if (SkyHanniMod.feature.misc.hidePiggyScoreboard) {
        val matcher = piggyPattern.matcher(text)
        if (matcher.matches()) {
            val coins = matcher.group("coins")
            return instance.drawString("Purse: $coins", x, y, color)
        }
    }

    return instance.drawString(text, x, y, color)
}
