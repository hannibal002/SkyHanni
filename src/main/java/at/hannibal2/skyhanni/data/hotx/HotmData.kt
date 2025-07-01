package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.api.HotmApi.MayhemPerk
import at.hannibal2.skyhanni.api.HotmApi.SkymallPerk
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.local.HotxTree
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import java.util.regex.Matcher
import kotlin.math.pow

private fun calculateCoreOfTheMountainLoot(level: Int): Map<HotmReward, Double> = buildMap {
    for (i in 1..level) {
        when (i) {
            1, 5, 7 -> addOrPut(HotmReward.EXTRA_TOKENS, 1.0)
            2 -> addOrPut(HotmReward.ABILITY_LEVEL, 1.0)
            3 -> addOrPut(HotmReward.EXTRA_COMMISSION_SLOTS, 1.0)
            4 -> addOrPut(HotmReward.MORE_BASE_MITHRIL_POWER, 1.0)
            6 -> addOrPut(HotmReward.MORE_BASE_GEMSTONE_POWER, 2.0)
            8 -> addOrPut(HotmReward.MORE_BASE_GLACITE_POWER, 3.0)
            9 -> addOrPut(HotmReward.MINESHAFT_CHANCE, 10.0)
            10 -> addOrPut(HotmReward.EXTRA_TOKENS, 2.0)
        }
    }
}

// Heart of the Mountain
enum class HotmData(
    override val guiName: String,
    override val maxLevel: Int,
    override val costFun: (Int) -> (Double?),
    override val rewardFun: (Int) -> (Map<HotmReward, Double>),
    val powderType: HotmApi.PowderType?,
) : HotxData<HotmReward> {

    MINING_SPEED(
        "Mining Speed",
        50,
        { level -> (level + 1.0).pow(3.0) },
        { level -> mapOf(HotmReward.MINING_SPEED to level * 20.0) },
        HotmApi.PowderType.MITHRIL,
    ),
    MINING_FORTUNE(
        "Mining Fortune",
        50,
        { level -> (level + 1.0).pow(3.05) },
        { level -> mapOf(HotmReward.MINING_FORTUNE to level * 2.0) },
        HotmApi.PowderType.MITHRIL,
    ),
    TITANIUM_INSANIUM(
        "Titanium Insanium",
        50,
        { level -> (level + 1.0).pow(3.1) },
        { level -> mapOf(HotmReward.TITANIUM_CHANCE to 2.0 + (level * 0.1)) },
        HotmApi.PowderType.MITHRIL,
    ),
    LUCK_OF_THE_CAVE(
        "Luck of the Cave",
        45,
        { level -> (level + 1.0).pow(3.07) },
        { level -> mapOf(HotmReward.EXTRA_CHANCE_TRIGGER_RARE_OCCURRENCES to 5.0 + level) },
        HotmApi.PowderType.MITHRIL,
    ),
    EFFICIENT_MINER(
        "Efficient Miner",
        100,
        { level -> (level + 1.0).pow(2.6) },
        { level -> mapOf(HotmReward.MINING_SPREAD to 3.0 * level) },
        HotmApi.PowderType.MITHRIL,
    ),
    QUICK_FORGE(
        "Quick Forge",
        20,
        { level -> (level + 1.0).pow(3.2) },
        { level -> mapOf(HotmReward.FORGE_TIME_DECREASE to if (level >= 20) 30.0 else 10.0 + (level * 0.5)) },
        HotmApi.PowderType.MITHRIL,
    ),
    OLD_SCHOOL(
        "Old-School",
        20,
        { level -> (level + 1.0).pow(4.0) },
        { level -> mapOf(HotmReward.ORE_FORTUNE to level * 5.0) },
        HotmApi.PowderType.GEMSTONE,
    ),
    PROFESSIONAL(
        "Professional",
        140,
        { level -> (level + 1.0).pow(2.3) },
        { level -> mapOf(HotmReward.MINING_SPEED to 50.0 + (level * 5.0)) },
        HotmApi.PowderType.GEMSTONE
    ),
    MOLE(
        "Mole",
        200,
        { level -> (level + 1.0).pow(2.17883) },
        { level -> mapOf(HotmReward.MINING_SPREAD to 50.0 + ((level - 1) * (350 / 199))) },
        HotmApi.PowderType.GEMSTONE,
    ),
    GEM_LOVER(
        "Gem Lover",
        20,
        { level -> (level + 1.0).pow(4.0) },
        { level -> mapOf(HotmReward.GEMSTONE_FORTUNE to 20.0 + (level * 4.0)) },
        HotmApi.PowderType.GEMSTONE,
    ),
    SEASONED_MINEMAN(
        "Seasoned Mineman",
        100,
        { level -> (level + 1.0).pow(2.3) },
        { level -> mapOf(HotmReward.MINING_WISDOM to 5.0 + (level * 0.1)) },
        HotmApi.PowderType.GEMSTONE,
    ),
    FORTUNATE_MINEMAN(
        "Fortunate Mineman",
        50,
        { level -> (level + 1.0).pow(3.2) },
        { level -> mapOf(HotmReward.MINING_FORTUNE to level * 3.0) },
        HotmApi.PowderType.GEMSTONE,
    ),
    BLOCKHEAD(
        "Blockhead",
        20,
        { level -> (level + 1.0).pow(4.0) },
        { level -> mapOf(HotmReward.BLOCK_FORTUNE to level * 5.0) },
        HotmApi.PowderType.GEMSTONE,
    ),
    KEEP_IT_COOL(
        "Keep It Cool",
        50,
        { level -> (level + 1.0).pow(3.07) },
        { level -> mapOf(HotmReward.HEAT_RESISTANCE to level * 0.4) },
        HotmApi.PowderType.GEMSTONE,
    ),

    LONESOME_MINER(
        "Lonesome Miner",
        45,
        { level -> (level + 1.0).pow(3.07) },
        { level -> mapOf(HotmReward.COMBAT_STAT_BOOST to 5.0 + ((level - 1.0) * 0.5)) },
        HotmApi.PowderType.GEMSTONE,
    ),
    GREAT_EXPLORER(
        "Great Explorer",
        20,
        { level -> (level + 1.0).pow(4.0) },
        { level ->
            mapOf(
                HotmReward.CHANCE_OF_TREASURE_CHEST to (0.2 * (0.2 + 0.04 * (level - 1.0))),
                HotmReward.LOCKS_OF_TREASURE_CHEST to 1 + level * 0.2,
            )
        },
        HotmApi.PowderType.GEMSTONE,
    ),

    POWDER_BUFF(
        "Powder Buff",
        50,
        { level -> (level + 1.0).pow(3.2) },
        { level ->
            mapOf(
                HotmReward.MORE_MITHRIL_POWER to level.toDouble(),
                HotmReward.MORE_GEMSTONE_POWER to level.toDouble(),
            )
        },
        HotmApi.PowderType.GEMSTONE,
    ),
    SPEEDY_MINEMAN(
        "Speedy Mineman",
        50,
        { level -> (level + 1.0).pow(3.2) },
        { level -> mapOf(HotmReward.MINING_SPEED to level * 40.0) },
        HotmApi.PowderType.GEMSTONE,
    ),

    SUBTERRANEAN_FISHER(
        "Subterranean Fisher",
        40,
        { level -> (level + 1.0).pow(3.07) },
        { level ->
            mapOf(
                HotmReward.FISHING_SPEED to 5 + (level * 0.5),
                HotmReward.SEA_CREATURE_CHANCE to 1 + (level * 0.1),
            )
        },
        HotmApi.PowderType.GEMSTONE,
    ),

    // Static

    SKY_MALL("Sky Mall", 1, { null }, { emptyMap() }, null),
    PRECISION_MINING("Precision Mining", 1, { null }, { mapOf(HotmReward.MINING_SPEED_BOOST to 30.0) }, null),
    FRONT_LOADED(
        "Front Loaded",
        1,
        { null },
        {
            mapOf(
                HotmReward.MINING_SPEED to 250.0,
                HotmReward.GEMSTONE_FORTUNE to 150.0,
                HotmReward.MORE_GEMSTONE_POWER to 200.0,
            )
        },
        null,
    ),
    DAILY_GRIND("Daily Grind", 1, { null }, { emptyMap() }, null),
    DAILY_POWDER("Daily Powder", 1, { null }, { emptyMap() }, null),
    // Abilities

    PICKOBULUS(
        "Pickobulus",
        3,
        { null },
        { level ->
            mapOf(
                HotmReward.ABILITY_RADIUS to 3.0,
                HotmReward.ABILITY_COOLDOWN to 60.0 - 10.0 * (level - 1),
            )
        },
        null,
    ),
    MINING_SPEED_BOOST(
        "Mining Speed Boost",
        3,
        { null },
        { level ->
            mapOf(
                HotmReward.MINING_SPEED_BOOST to 200.0 + 50.0 * (level - 1),
                HotmReward.ABILITY_DURATION to 10.0 + 5 * (level - 1),
                HotmReward.ABILITY_COOLDOWN to 120.0,
            )
        },
        null,
    ),
    MANIAC_MINER(
        "Maniac Miner",
        3,
        { null },
        { level ->
            mapOf(
                HotmReward.ABILITY_DURATION to 20.0 + level * 5.0,
                HotmReward.ABILITY_COOLDOWN to 60.0,
                HotmReward.BREAKING_POWER to 1.0,
            )
        },
        null,
    ),

    SHEER_FORCE(
        "Sheer Force",
        3,
        { null },
        { level ->
            mapOf(
                HotmReward.ABILITY_DURATION to 20.0 + 5 * (level - 1),
                HotmReward.MINING_SPREAD to 200.0,
            )
        },
        null,
    ),

    ANOMALOUS_DESIRE(
        "Anomalous Desire",
        3,
        { null },
        { level ->
            mapOf(
                HotmReward.EXTRA_CHANCE_TRIGGER_RARE_OCCURRENCES to 30.0 + (level - 1) * 10.0,
                HotmReward.ABILITY_COOLDOWN to 120.0 - (level - 1) * 10.0,
                HotmReward.ABILITY_DURATION to 30.0,
            )
        },
        null,
    ),

    CORE_OF_THE_MOUNTAIN(
        "Core of the Mountain", 10, { null },
        { level -> calculateCoreOfTheMountainLoot(level) },
        null,
    ),

    // Mining V3

    NO_STONE_UNTURNED(
        "No Stone Unturned",
        50,
        { level -> (level + 1.0).pow(3.05) },
        { level -> mapOf(HotmReward.UNKNOWN to 0.5 * level) },
        HotmApi.PowderType.GLACITE,
    ),

    STRONG_ARM(
        "Strong Arm",
        100,
        { level -> (level + 1.0).pow(2.3) },
        { level -> mapOf(HotmReward.MINING_SPEED to 5.0 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    STEADY_HAND(
        "Steady Hand",
        100,
        { level -> (level + 1.0).pow(2.6) },
        { level -> mapOf(HotmReward.GEMSTONE_SPREAD to 0.1 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    WARM_HEART(
        "Warm Heart",
        50,
        { level -> (level + 1.0).pow(3.1) },
        { level -> mapOf(HotmReward.COLD_RESISTANCE to 0.4 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    SURVEYOR(
        "Surveyor",
        20,
        { level -> (level + 1.0).pow(4.0) },
        { level -> mapOf(HotmReward.MINESHAFT_CHANCE to 0.75 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    METAL_HEAD(
        "Metal Head",
        20,
        { level -> (level + 1.0).pow(4.0) },
        { level -> mapOf(HotmReward.DWARVEN_METAL_FORTUNE to 5.0 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    RAGS_TO_RICHES(
        "Rags to Riches",
        50,
        { level -> (level + 1.0).pow(3.05) },
        { level -> mapOf(HotmReward.MINING_FORTUNE to 4.0 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    EAGER_ADVENTURER(
        "Eager Adventurer",
        100,
        { level -> (level + 1.0).pow(2.3) },
        { level -> mapOf(HotmReward.MINING_SPEED to 4.0 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    CRYSTALLINE(
        "Crystalline",
        50,
        { level -> (level + 1.0).pow(3.3) },
        { level -> mapOf(HotmReward.UNKNOWN to 0.5 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    GIFTS_FROM_THE_DEPARTED(
        "Gifts from the Departed",
        100,
        { level -> (level + 1.0).pow(2.45) },
        { level -> mapOf(HotmReward.UNKNOWN to 0.2 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    MINING_MASTER(
        "Mining Master",
        10,
        { level -> (level + 7.0).pow(5.0) },
        { level -> mapOf(HotmReward.PRISTINE to 0.1 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    DEAD_MANS_CHEST(
        "Dead Man's Chest",
        50,
        { level -> (level + 1.0).pow(3.2) },
        { level -> mapOf(HotmReward.UNKNOWN to 1.0 * level) },
        HotmApi.PowderType.GLACITE,
    ),
    VANGUARD_SEEKER(
        "Vanguard Seeker",
        50,
        { level -> (level + 1.0).pow(3.1) },
        { level -> mapOf(HotmReward.UNKNOWN to 1.0 * level) },
        HotmApi.PowderType.GLACITE,
    ),

    MINESHAFT_MAYHEM("Mineshaft Mayhem", 1, { null }, { emptyMap() }, null),
    GEMSTONE_INFUSION("Gemstone Infusion", 1, { null }, { emptyMap() }, null),
    MINERS_BLESSING("Miner's Blessing", 1, { null }, { mapOf(HotmReward.MAGIC_FIND to 30.0) }, null),
    ;

    override val guiNamePattern by patternGroup.pattern("perk.name.${name.lowercase().replace("_", "")}", "§.$guiName")

    override val printName = name.allLettersFirstUppercase()

    /** Level that considering [blueEgg]*/
    override val effectiveLevel: Int get() = rawLevel.takeIf { it != Int.MIN_VALUE }?.plus(blueEgg()) ?: 0

    private fun blueEgg() = if (this != CORE_OF_THE_MOUNTAIN && maxLevel != 1 && HotmApi.isBlueEggActive) 1 else 0

    override var slot: Slot? = null

    override var item: ItemStack? = null

    override val totalCostMaxLevel = calculateTotalCost(maxLevel)

    override fun getStorage(): HotxTree? = ProfileStorageData.profileSpecific?.mining?.hotmTree

    // TODO move all object functions into hotm api?
    @SkyHanniModule
    companion object : HotxHandler<HotmData, HotmReward, SkymallPerk>(entries) {

        override val name: String = "HotM"
        override val rotatingPerkClazz = SkymallPerk::class

        val storage get() = ProfileStorageData.profileSpecific?.mining?.hotmTree

        val abilities =
            listOf(PICKOBULUS, MINING_SPEED_BOOST, MANIAC_MINER, GEMSTONE_INFUSION, ANOMALOUS_DESIRE, SHEER_FORCE)

        override val inventoryPattern by patternGroup.pattern(
            "inventory",
            "Heart of the Mountain",
        )

        // <editor-fold desc="Patterns">
        /**
         * REGEX-TEST: §5§o§7Level 1§8/50 §7(§b0 §l0%§7):skull:
         * REGEX-TEST: §7Level 1§8/50
         */
        override val levelPattern by patternGroup.pattern(
            "perk.level",
            "(?:§.)*§(?<color>.)Level (?<level>\\d+).*",
        )

        override val notUnlockedPattern by patternGroup.pattern(
            "perk.notunlocked",
            "(?:§.)*Requires.*|.*Mountain!|(?:§.)*Click to unlock!|",
        )

        /**
         * REGEX-TEST: §a§lSELECTED
         * REGEX-TEST: §a§lENABLED
         */
        override val enabledPattern by patternGroup.pattern(
            "perk.enable",
            "§a§lENABLED|(?:§.)*SELECTED",
        )

        /**
         * REGEX-TEST: §eClick to select!
         * REGEX-TEST: §c§lDISABLED
         */
        @Suppress("UnusedPrivateProperty")
        private val disabledPattern by patternGroup.pattern(
            "perk.disabled",
            "§c§lDISABLED|§eClick to select!",
        ) // unused for now since the assumption is when enabled isn't found, it is disabled,
        // but the value might be useful in the future or for debugging

        /**
         * REGEX-TEST: §7Cost
         */
        val perkCostPattern by patternGroup.pattern(
            "perk.cost",
            "(?:§.)*§7Cost",
        )

        override val resetChatPattern by patternGroup.pattern(
            "reset.chat",
            "§aReset your §r§5Heart of the Mountain§r§a! Your Perks and Abilities have been reset\\.",
        )

        override val heartItemPattern by patternGroup.pattern(
            "inventory.heart",
            "§5Heart of the Mountain",
        )
        override val resetItemPattern by patternGroup.pattern(
            "inventory.reset",
            "§cReset Heart of the Mountain",
        )

        /**
         * REGEX-TEST: §7Token of the Mountain: §515
         */
        override val heartTokensPattern by patternGroup.pattern(
            "inventory.heart.token",
            "§7Token of the Mountain: §5(?<token>\\d+)",
        )

        /**
         * REGEX-TEST:   §8- §54 Token of the Mountain
         */
        override val resetTokensPattern by patternGroup.pattern(
            "inventory.reset.token",
            "\\s+§8- §5(?<token>\\d+) Token of the Mountain",
        )

        private val mayhemChatPattern by patternGroup.pattern(
            "mayhem",
            "§b§lMAYHEM! §r§7(?<perk>.*)",
        )

        /**
         * REGEX-TEST:  Mithril: §r§299,918
         * REGEX-TEST:  Gemstone: §r§d37,670
         */
        private val powderPattern by patternGroup.pattern(
            "widget.powder",
            "\\s*(?<type>\\w+): (?:§.)+(?<amount>[\\d,.]+)",
        )
        // </editor-fold>

        override var tokens: Int
            get() = ProfileStorageData.profileSpecific?.mining?.tokens ?: 0
            set(value) {
                ProfileStorageData.profileSpecific?.mining?.tokens = value
            }

        override var availableTokens: Int
            get() = ProfileStorageData.profileSpecific?.mining?.availableTokens ?: 0
            set(value) {
                ProfileStorageData.profileSpecific?.mining?.availableTokens = value
            }

        init {
            HotmApi.PowderType.entries.forEach {
                it.heartPattern
                it.resetPattern
            }
            SkymallPerk.entries.forEach {
                it.chatPattern
                it.itemPattern
            }
            MayhemPerk.entries.forEach {
                it.chatPattern
            }
            for (level in 0..CORE_OF_THE_MOUNTAIN.maxLevel) {
                val map = mutableMapOf<HotmReward, Double>()
                if (level >= 1) map.addOrPut(HotmReward.EXTRA_TOKENS, 1.0)
                if (level >= 2) map.addOrPut(HotmReward.ABILITY_LEVEL, 1.0)
                if (level >= 3) map.addOrPut(HotmReward.EXTRA_COMMISSION_SLOTS, 1.0)
                if (level >= 4) map.addOrPut(HotmReward.MORE_BASE_MITHRIL_POWER, 1.0)
                if (level >= 5) map.addOrPut(HotmReward.EXTRA_TOKENS, 1.0)
                if (level >= 6) map.addOrPut(HotmReward.MORE_BASE_GEMSTONE_POWER, 2.0)
                if (level >= 7) map.addOrPut(HotmReward.EXTRA_TOKENS, 1.0)
                if (level >= 8) map.addOrPut(HotmReward.MORE_BASE_GLACITE_POWER, 3.0)
                if (level >= 9) map.addOrPut(HotmReward.MINESHAFT_CHANCE, 10.0)
                if (level >= 10) map.addOrPut(HotmReward.EXTRA_TOKENS, 2.0)

                coreOfTheMountainPerks[level] = map
            }
        }

        override fun currencyReset(full: Boolean) {
            super.currencyReset(full)
            if (full) {
                HotmApi.PowderType.entries.forEach(HotmApi.PowderType::resetFull)
            } else {
                HotmApi.PowderType.entries.forEach(HotmApi.PowderType::resetTree)
            }
        }

        override val readingLevelTransform: Matcher.() -> Int = {
            group("level").toInt().transformIf({ group("color") == "b" }, { this.minus(1) })
        }

        override fun Slot.extraHandling(entry: HotmData, lore: List<String>) {
            // Hi I'm not empty
        }

        override val core = CORE_OF_THE_MOUNTAIN

        override fun readFromHeartOrReset(line: String, isHeartItem: Boolean) {
            HotmApi.PowderType.entries.forEach {
                it.pattern(isHeartItem).matchMatcher(line) {
                    val powder = group("powder").formatLong()
                    if (isHeartItem) it.setAmount(powder)
                    else it.total += powder
                    return
                }
            }
        }

        @HandleEvent(onlyOnSkyblock = true)
        fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
            ScoreboardPattern.powderPattern.firstMatcher(event.added) {
                val type = HotmApi.PowderType.entries.firstOrNull { it.displayName == group("type") } ?: return
                val amount = group("amount").formatLong()
                type.setAmount(amount, postEvent = true)
            }
        }

        @HandleEvent
        override fun onInventoryClose(event: InventoryCloseEvent) = super.onInventoryClose(event)

        @HandleEvent(onlyOnSkyblock = true)
        override fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) = super.onInventoryFullyOpened(event)

        override fun extraInventoryHandling() {
            abilities.filter { it.isUnlocked }.forEach {
                it.rawLevel = if (CORE_OF_THE_MOUNTAIN.rawLevel >= 1) 2 else 1
            }
        }

        @HandleEvent
        fun onWidgetUpdate(event: WidgetUpdateEvent) {
            if (!event.isWidget(TabWidget.POWDER)) return
            event.lines.forEach { line ->
                powderPattern.matchMatcher(line) {
                    val type = HotmApi.PowderType.entries.firstOrNull { it.displayName == group("type") } ?: return
                    val amount = group("amount").formatLong()
                    type.setAmount(amount, postEvent = true)
                }
            }
        }

        override fun setRotatingPerk(newRotatingPerk: SkymallPerk?) {
            HotmApi.skymall = newRotatingPerk
            ChatUtils.debug("setting skymall to ${HotmApi.skymall}")
        }

        @HandleEvent(onlyOnSkyblock = true)
        override fun onChat(event: SkyHanniChatEvent) = super.onChat(event)

        override fun tryBlock(event: SkyHanniChatEvent) {
            if (!chatConfig.hideSkyMall || IslandTypeTags.MINING.inAny()) return
            event.blockedReason = "skymall"
        }

        override fun extraChatHandling(event: SkyHanniChatEvent) {
            DelayedRun.runNextTick {
                mayhemChatPattern.matchMatcher(event.message) {
                    val perk = group("perk")
                    HotmApi.mineshaftMayhem = MayhemPerk.entries.firstOrNull { it.chatPattern.matches(perk) } ?: run {
                        ErrorManager.logErrorStateWithData(
                            "Could not read the mayhem effect from chat",
                            "no chatPattern matched",
                            "chat" to event.message,
                            "perk" to perk,
                        )
                        null
                    }
                    ChatUtils.debug("setting mineshaftMayhem to ${HotmApi.mineshaftMayhem}")
                }
            }
        }

        override val rotatingPerkEntry: HotmData = SKY_MALL

        @HandleEvent
        fun onIslandChange(event: IslandChangeEvent) {
            if (HotmApi.mineshaftMayhem == null) return
            HotmApi.mineshaftMayhem = null
            ChatUtils.debug("resetting mineshaftMayhem")
        }

        @HandleEvent
        fun onDebug(event: DebugDataCollectEvent) {
            event.title("HotM")
            event.addIrrelevant {
                add("Tokens : $availableTokens/$tokens")
                HotmApi.PowderType.entries.forEach {
                    add("${it.displayName} Powder: ${it.current}/${it.total}")
                }
                add("Ability: ${HotmApi.activeMiningAbility?.printName}")
                add("Blue Egg: ${HotmApi.isBlueEggActive}")
                add("SkyMall: ${HotmApi.skymall}")
                add("Mineshaft Mayhem: ${HotmApi.mineshaftMayhem}")
            }
            debugTree(event)
        }
    }
}

private val chatConfig get() = SkyHanniMod.feature.chat

private val coreOfTheMountainPerks = mutableMapOf<Int, Map<HotmReward, Double>>()

private val patternGroup = RepoPattern.group("mining.hotm")

enum class HotmReward {
    MINING_SPEED,
    MINING_FORTUNE,
    MINING_WISDOM,
    FORGE_TIME_DECREASE,
    TITANIUM_CHANCE,
    MORE_BASE_MITHRIL_POWER,
    MORE_BASE_GEMSTONE_POWER,
    MORE_BASE_GLACITE_POWER,
    MORE_MITHRIL_POWER,
    MORE_GEMSTONE_POWER,
    COMBAT_STAT_BOOST,
    CHANCE_OF_TREASURE_CHEST,
    LOCKS_OF_TREASURE_CHEST,
    EXTRA_CHANCE_TRIGGER_RARE_OCCURRENCES,
    MINING_SPEED_BOOST,
    ABILITY_DURATION,
    ABILITY_RADIUS,
    ABILITY_COOLDOWN,
    ABILITY_LEVEL,
    MINESHAFT_CHANCE,
    EXTRA_TOKENS,
    EXTRA_COMMISSION_SLOTS,
    UNKNOWN,
    COLD_RESISTANCE,
    MINING_SPREAD,
    GEMSTONE_SPREAD,
    ORE_FORTUNE,
    BLOCK_FORTUNE,
    GEMSTONE_FORTUNE,
    DWARVEN_METAL_FORTUNE,
    HEAT_RESISTANCE,
    MAGIC_FIND,
    PRISTINE,
    FISHING_SPEED,
    SEA_CREATURE_CHANCE,
    BREAKING_POWER,
}
