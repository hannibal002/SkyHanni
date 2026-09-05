package at.hannibal2.skyhanni.config.features.gui.customscoreboard

enum class ChunkedStatsLine(
    private val configLine: String,
) {
    PURSE("§6Purse"),
    MOTES("§dMotes"),
    BANK("§6Bank"),
    BITS("§bBits"),
    COPPER("§cCopper"),
    SOWDUST("§2Sowdust"),
    GEMS("§aGems"),
    HEAT("§cHeat"),
    COLD("§bCold"),
    NORTH_STARS("§dNorth Stars"),
    ;

    override fun toString() = configLine
}
