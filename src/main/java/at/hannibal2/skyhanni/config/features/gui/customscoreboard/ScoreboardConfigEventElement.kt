package at.hannibal2.skyhanni.config.features.gui.customscoreboard

enum class ScoreboardConfigEventElement(val displayName: String) {
    VOTING("§7(All Voting Lines)"),
    SERVER_CLOSE("§cServer closing soon!"),
    DUNGEONS("§7(All Dungeons Lines)"),
    KUUDRA("§7(All Kuudra Lines)"),
    DOJO("§7(All Dojo Lines)"),
    DARK_AUCTION("Time Left: §b11\nCurrent Item:\n §5Travel Scroll to Sirius"),
    JACOB_CONTEST("§eJacob's Contest\n§e○ §fCarrot §a18m17s\n Collected §e8,264"),
    JACOB_MEDALS("§6§lGOLD §fmedals: §613\n§f§lSILVER §fmedals: §f3\n§c§lBRONZE §fmedals: §c4"),
    TRAPPER("Pelts: §5711\nTracker Mob Location:\n§bMushroom Gorge"),
    GARDEN("§7(All Garden Lines)"),
    FLIGHT_DURATION("Flight Duration: §a10m 0s"),
    WINTER("§7(All Winter Event Lines)"),
    NEW_YEAR("§dNew Year Event!§f 24:25"),
    SPOOKY("§6Spooky Festival§f 50:54\n§7Your Candy:\n§a1 Green§7, §50 Purple §7(§61 §7pts.)"),
    BROODMOTHER("Broodmother§7: §eDormant"),
    MINING_EVENTS("§7(All Mining Event Lines)"),
    GALATEA("§7(All Galatea Lines)"),
    SAFARI("§7(All Safari Lines)"),
    DAMAGE("Dragon HP: §a6,180,925 §c❤\nYour Damage: §c375,298.5"),
    MAGMA_BOSS("§7(All Magma Boss Lines)\n§7Boss: §c0%\n§7Damage Soaked:\n§e▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎§7▎▎▎▎▎"),
    CARNIVAL("§7(All Carnival Lines)"),
    RIFT("§7(All Rift Lines)"),
    ESSENCE("Dragon Essence: §d1,285"),
    QUEUE("Queued: Glacite Mineshafts\nPosition: §b#45 §fSince: §a00:00"),
    ANNIVERSARY("§d5th Anniversary§f 167:59:54"),
    ACTIVE_TABLIST_EVENTS("§7(All Active Tablist Events)\n§dHoppity's Hunt\n §fEnds in: §e26h"),
    STARTING_SOON_TABLIST_EVENTS("§7(All Starting Soon Tablist Events)\n§6Mining Fiesta\n §fStarts in: §e52min"),
    REDSTONE("§e§l⚡ §cRedstone: §e§b7%"),
    ;

    override fun toString(): String = displayName

    companion object {
        @Suppress("StorageNeedsExpose")
        @JvmField
        val defaultOption = listOf(
            VOTING,
            SERVER_CLOSE,
            DUNGEONS,
            KUUDRA,
            DOJO,
            DARK_AUCTION,
            JACOB_CONTEST,
            JACOB_MEDALS,
            GALATEA,
            SAFARI,
            TRAPPER,
            GARDEN,
            FLIGHT_DURATION,
            NEW_YEAR,
            WINTER,
            SPOOKY,
            BROODMOTHER,
            MINING_EVENTS,
            DAMAGE,
            MAGMA_BOSS,
            CARNIVAL,
            RIFT,
            ESSENCE,
            ACTIVE_TABLIST_EVENTS,
            REDSTONE,
        )
    }
}
