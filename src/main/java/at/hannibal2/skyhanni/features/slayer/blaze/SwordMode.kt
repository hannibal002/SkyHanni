package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.utils.LorenzColor

enum class SwordMode(val formattedName: String, val color: LorenzColor, val chatColor: String = color.getChatColor(), var active: Boolean = false) {
    AURIC("§e§lAURIC", LorenzColor.YELLOW),
    ASHEN("§8§lASHEN", LorenzColor.DARK_GRAY),
    SPIRIT("§f§lSPIRIT", LorenzColor.WHITE),
    CRYSTAL("§b§lCRYSTAL", LorenzColor.AQUA),
    ;
}