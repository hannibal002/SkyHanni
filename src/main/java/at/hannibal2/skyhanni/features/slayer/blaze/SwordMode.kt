package at.hannibal2.skyhanni.features.slayer.blaze

enum class SwordMode(val formattedName: String, val color: String, var active: Boolean = false) {
    AURIC("§e§lAURIC", "§e"),
    ASHEN("§8§lASHEN", "§8"),
    SPIRIT("§f§lSPIRIT", "§f"),
    CRYSTAL("§b§lCRYSTAL", "§b"),
    ;
}