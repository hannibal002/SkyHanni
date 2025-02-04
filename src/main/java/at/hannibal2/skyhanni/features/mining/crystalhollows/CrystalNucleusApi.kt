package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.mining.nucleus.CrystalNucleusTrackerConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusCrystalFoundEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusCrystalPlacedEvent
import at.hannibal2.skyhanni.events.mining.CrystalNucleusLootEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.fromItemNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CrystalNucleusApi {

    private val patternGroup = RepoPattern.group("mining.crystalnucleus")
    private val config get() = SkyHanniMod.feature.mining.crystalNucleusTracker

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST:   §r§5§lCRYSTAL NUCLEUS LOOT BUNDLE
     */
    private val startPattern by patternGroup.pattern(
        "loot.start",
        " \\s*§r§5§lCRYSTAL NUCLEUS LOOT BUNDLE.*",
    )

    /**
     * REGEX-TEST: §3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val endPattern by patternGroup.pattern(
        "loot.end",
        "§3§l▬{64}",
    )

    /**
     * REGEX-TEST: §5§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    val crystalCollectedWrapperPattern by patternGroup.pattern(
        "crystal.collected.wrapper",
        "§5§l▬{64}",
    )

    /**
     * REGEX-TEST: §3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    val runCompletedWrapperPattern by patternGroup.pattern(
        "run.completed",
        "§3§l▬{64}",
    )

    /**
     * REGEX-TEST: §f                       §r§5§l✦ CRYSTAL FOUND §r§7(1§r§7/5§r§7)
     */
    val crystalCollectedCountPattern by patternGroup.pattern(
        "crystal.collected.count",
        "§f *§r§5§l✦ CRYSTAL FOUND §r§7\\((?<count>\\d)§r§7/5§r§7\\)",
    )

    /**
     * REGEX-TEST: §f                              §r§5Amethyst Crystal
     * REGEX-TEST: §f                              §r§bSapphire Crystal
     * REGEX-TEST: §f                                §r§6Amber Crystal
     * REGEX-TEST: §f                                §r§eTopaz Crystal
     * REGEX-TEST: §f                                §r§aJade Crystal
     */
    val crystalCollectedIdentifierPattern by patternGroup.pattern(
        "crystal.collected.id",
        "§f *§r(?<crystal>.* Crystal) *",
    )

    /**
     * REGEX-TEST: §5§l✦ §r§dYou placed the §r§bSapphire Crystal§r§d!
     */
    val crystalPlacedPattern by patternGroup.pattern(
        "crystal.placed",
        "§5§l✦ §r§dYou placed the §r(?<crystal>.* Crystal)§r§d!",
    )

    /**
     * REGEX-TEST: §aYou found §r§cScavenged Diamond Axe §r§awith your §r§cMetal Detector§r§a!
     * REGEX-TEST: §aYou found §r§cScavenged Emerald Hammer §r§awith your §r§cMetal Detector§r§a!
     * REGEX-TEST: §aYou found §r§a☘ Flawed Jade Gemstone §r§8x2 §r§awith your §r§cMetal Detector§r§a!
     */
    val scavengeLootPattern by patternGroup.pattern(
        "divan.scavenge",
        "§aYou found §r(?<loot>.*) §r§awith your §r§cMetal Detector§r§a!",
    )

    /**
     * REGEX-TEST: §6§lPICK IT UP!
     */
    val scavengeSecondaryPattern by patternGroup.pattern(
        "divan.scavenge.secondary",
        "§6§lPICK IT UP!",
    )

    /**
     * REGEX-TEST: §e[NPC] §6Keeper of Gold§f: §rExcellent! You have returned the §cScavenged Golden Hammer §rto its rightful place!
     * REGEX-TEST: §e[NPC] §6Keeper of Diamond§f: §rExcellent! You have returned the §cScavenged Diamond Axe §rto its rightful place!
     * REGEX-TEST: §e[NPC] §6Keeper of Emerald§f: §rExcellent! You have returned the §cScavenged Emerald Hammer §rto its rightful place!
     * REGEX-TEST: §e[NPC] §6Keeper of Lapis§f: §rYou found all of the items! Behold... the §aJade Crystal§r!
     */
    val genericKeeperMessage by patternGroup.pattern(
        "npc.keeper",
        "§e\\[NPC\\] §6Keeper of (?<keepertype>.*)§f: §r(?<message>.*)",
    )

    /**
     * REGEX-TEST: Thanks for bringing me the §9Synthetic Heart§r! Bring me 5 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9Robotron Reflector§r! Bring me 5 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9Superlite Motor§r! Bring me 4 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9Synthetic Heart§r! Bring me 3 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9FTX 3070§r! Bring me 2 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9Electron Transmitter§r! Bring me one more component to fix the giant!
     * REGEX-TEST: §rYou've brought me all of the components!
     * REGEX-TEST: §rYou've brought me all of the components... I think? To be honest, I kind of lost count...
     * REGEX-TEST: Wait a minute. This will work just fine.
     */
    @Suppress("MaxLineLength")
    val componentSubmittedPattern by patternGroup.pattern(
        "precursor.submitted",
        "(?:Wait a minute. This will work just fine.|You've brought me all|me the (?<component>.*)§r! Bring me (?<remaining>\\d|one) more).*",
    )

    /**
     * REGEX-TEST: §rThat's not one of the components I need! Bring me one of the missing components:
     */
    val componentListPreamblePattern by patternGroup.pattern(
        "component.list.preamble",
        "§rThat's not one of the components I need! Bring me one of the missing components:",
    )

    /**
     * REGEX-TEST:   §r§9FTX 3070
     * REGEX-TEST:   §r§9Electron Transmitter
     * REGEX-TEST:   §r§9Superlite Motor
     * REGEX-TEST:   §r§9Synthetic Heart
     * REGEX-TEST:   §r§9Control Switch
     * REGEX-TEST:   §r§9Robotron Reflector
     */
    val componentListPattern by patternGroup.pattern(
        "component.list",
        " {2}§r§9(?<component>.*)",
    )

    /**
     * REGEX-TEST: §8§oWhew! That was a close one, better get out of here...
     * REGEX-TEST: §cThe Goblin King's §r§afoul stench §r§chas dissipated!
     */
    val goblinGuardExitMessagePattern by patternGroup.pattern(
        "goblin.guard.exit",
        "§8§oWhew! That was a close one, better get out of here\\.{3}|§cThe Goblin King's §r§afoul stench §r§chas dissipated!",
    )

    /**
     * REGEX-TEST: /warp nuc
     * REGEX-TEST: /warp nucleus
     */
    val nucleusWarpCommandPattern by patternGroup.pattern(
        "nucleus.warp.command",
        "/warp (?:nuc|nucleus)",
    )
    // </editor-fold>

    // <editor-fold desc="Helper Classes">
    enum class NucleusCrystalType(private val gemstoneType: SkyBlockItemModifierUtils.GemstoneType) {
        AMBER(SkyBlockItemModifierUtils.GemstoneType.AMBER),
        AMETHYST(SkyBlockItemModifierUtils.GemstoneType.AMETHYST),
        JADE(SkyBlockItemModifierUtils.GemstoneType.JADE),
        SAPPHIRE(SkyBlockItemModifierUtils.GemstoneType.SAPPHIRE),
        TOPAZ(SkyBlockItemModifierUtils.GemstoneType.TOPAZ),
        ;

        override fun toString() = gemstoneType.toString()

        companion object {
            fun getByNameOrNull(name: String) = SkyBlockItemModifierUtils.GemstoneType.getByNameOrNull(name)?.toNucleusGemstoneType()
            private fun SkyBlockItemModifierUtils.GemstoneType.toNucleusGemstoneType() =
                entries.firstOrNull { it.gemstoneType == this }
        }
    }
    // </editor-fold>

    private var inLootLoop = false
    private var unCheckedBooks: Int = 0
    private val loot = mutableMapOf<NeuInternalName, Int>()
    var inNucleus = false
        private set
    var unclosedCrystalCollected = false
        private set

    val EPIC_BAL_ITEM = "BAL;3".toInternalName()
    val LEGENDARY_BAL_ITEM = "BAL;4".toInternalName()
    val JUNGLE_KEY_ITEM = "JUNGLE_KEY".toInternalName()
    private val LAPIDARY_I_BOOK_ITEM = "LAPIDARY;1".toInternalName()
    private val FORTUNE_IV_BOOK_ITEM = "FORTUNE;4".toInternalName()
    private val PRECURSOR_APPARATUS_ITEM = "PRECURSOR_APPARATUS".toInternalName()
    private val ROBOT_PARTS_ITEMS = listOf(
        "CONTROL_SWITCH",
        "ELECTRON_TRANSMITTER",
        "FTX_3070",
        "ROBOTRON_REFLECTOR",
        "SUPERLITE_MOTOR",
        "SYNTHETIC_HEART",
    ).map { it.toInternalName() }

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inNucleus = event.area == "Crystal Nucleus"
    }

    @HandleEvent
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (unCheckedBooks == 0) return
        if (event.itemStack.displayName != "§fEnchanted Book") return
        when (event.itemStack.getEnchantments()?.keys?.firstOrNull() ?: return) {
            "lapidary" -> loot.addOrPut(LAPIDARY_I_BOOK_ITEM, 1)
            "fortune" -> loot.addOrPut(FORTUNE_IV_BOOK_ITEM, 1)
        }
        unCheckedBooks--
        if (unCheckedBooks == 0) {
            CrystalNucleusLootEvent(loot).post()
            loot.clear()
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (unCheckedBooks == 0 ||
            event.oldIsland != IslandType.CRYSTAL_HOLLOWS ||
            event.newIsland == IslandType.CRYSTAL_HOLLOWS
        ) return
        unCheckedBooks = 0
        if (loot.isNotEmpty()) {
            CrystalNucleusLootEvent(loot).post()
            loot.clear()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS, priority = HandleEvent.HIGH)
    fun onChat(event: SkyHanniChatEvent) {
        event.handleLootLoop()
        event.handleCrystalFound()
        event.handleCrystalPlaced()
    }

    private fun SkyHanniChatEvent.handleCrystalPlaced() {
        crystalPlacedPattern.matchMatcher(message) {
            val crystal = group("crystal") ?: return@matchMatcher
            val gem = NucleusCrystalType.getByNameOrNull(crystal) ?: return@matchMatcher
            CrystalNucleusCrystalPlacedEvent(gem).post()
        }
    }

    private fun SkyHanniChatEvent.handleCrystalFound() {
        if (crystalCollectedWrapperPattern.matches(message)) unclosedCrystalCollected = !unclosedCrystalCollected
        if (!unclosedCrystalCollected) return
        crystalCollectedIdentifierPattern.matchMatcher(message) {
            val crystal = group("crystal") ?: return@matchMatcher
            val gem = NucleusCrystalType.getByNameOrNull(group("crystal") ?: return@matchMatcher) ?: return@matchMatcher
            CrystalNucleusCrystalFoundEvent(gem).post()
        }
    }

    private fun SkyHanniChatEvent.handleLootLoop() {
        if (startPattern.matches(message)) {
            unCheckedBooks = 0
            inLootLoop = true
            return
        }
        if (!inLootLoop) return

        // Add the loot to the map.
        getLoot()?.let { (item, amount) ->
            loot.addOrPut(item, amount)
        }

        // Close the loot loop if the end pattern is matched.
        if (endPattern.matches(message)) {
            inLootLoop = false
            // If there are unchecked books, the loot is not complete, and will be finished in the
            // pickup event handler.
            if (unCheckedBooks > 0) return
            CrystalNucleusLootEvent(loot).post()
            loot.clear()
        }
    }

    private fun SkyHanniChatEvent.getLoot(): Pair<NeuInternalName, Int>? {
        // All loot rewards start with 4 spaces.
        // To simplify regex statements, this check is done outside the main logic.
        // This also nerfs the "§r§a§lREWARDS" message.
        if (!message.startsWith("    ")) return null
        val lootMessage = message.substring(4)

        // Read the item and amount from the message.
        val (itemName, amount) = ItemUtils.readItemAmount(lootMessage) ?: return null

        // Ignore Mithril and Gemstone Powder
        if (itemName.contains(" Powder")) return null
        // Books are not directly added to the loot map, but are checked for later.
        if (itemName.startsWith("§fEnchanted")) {
            unCheckedBooks += amount
            return null
        }
        val item = fromItemNameOrNull(itemName) ?: return null
        return Pair(item, amount)
    }

    fun usesApparatus() =
        config.professorUsage.get() == CrystalNucleusTrackerConfig.ProfessorUsageType.PRECURSOR_APPARATUS

    fun getPrecursorRunPrice() =
        if (usesApparatus()) PRECURSOR_APPARATUS_ITEM.getPrice()
        else ROBOT_PARTS_ITEMS.sumOf {
            it.getPrice()
        }
}
