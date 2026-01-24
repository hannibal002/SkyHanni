package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.HotfApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.local.HotxTree
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.pow

private fun calculateCenterOfTheForestLoot(level: Int): Map<HotfReward, Double> = buildMap {
    for (i in 1..level) {
        when (i) {
            1 -> {
                addOrPut(HotfReward.ABILITY_LEVEL, 1.0)
                addOrPut(HotfReward.EXTRA_TOKENS, 1.0)
            }
            2 -> addOrPut(HotfReward.SWEEP_PERCENT, 5.0)
            3 -> {
                addOrPut(HotfReward.BONUS_WHISPERS_TREE_GIFTS, 20.0)
                addOrPut(HotfReward.BONUS_WHISPERS_LOGS, 2.0)
            }
            4 -> addOrPut(HotfReward.SWEEP_PERCENT, 10.0)
            5 -> addOrPut(HotfReward.EXTRA_TOKENS, 1.0)
        }
    }
}

// Heart of the Forest
enum class HotfData(
    override val guiName: String,
    override val maxLevel: Int,
    override val costFun: (Int) -> (Double?),
    override val rewardFun: (Int) -> (Map<HotfReward, Double>),
) : HotxData<HotfReward> {
    SWEEP(
        "Sweep", 50,
        { level -> (level + 1.0).pow(3.0) },
        { level -> mapOf(HotfReward.SWEEP to level * 1.0) },
    ),
    FORAGING_FORTUNE(
        "Foraging Fortune", 50,
        { level -> (level + 1.0).pow(3.105) },
        { level -> mapOf(HotfReward.FORAGING_FORTUNE to level * 3.0) },
    ),
    STRENGTH_BOOST(
        "Strength Boost", 50,
        { level -> (level + 1.0).pow(3.1) },
        { level -> mapOf(HotfReward.STRENGTH to level * 2.0) },
    ),
    DAMAGE_BOOST(
        "Damage Boost", 2,
        { null },
        { level ->
            mapOf(
                HotfReward.AXE_DAMAGE_MULTIPLIER to 2.0,
                HotfReward.ABILITY_DURATION to 10.0,
                HotfReward.ABILITY_COOLDOWN to 120.0 - 5 * (level - 1),
            )
        },
    ),
    SPEED_BOOST(
        "Speed Boost", 50,
        { level -> (level + 1.0).pow(3.1) },
        { level -> mapOf(HotfReward.SPEED to level * 1.0) },
    ),
    AXE_TOSS(
        "Axe Toss", 2,
        { null },
        { level ->
            mapOf(
                HotfReward.THROW_PENALTY_REDUCTION to 100.0,
                HotfReward.ABILITY_DURATION to 10.0,
                HotfReward.ABILITY_COOLDOWN to 120.0 - 1 * (level - 1),
            )
        },
    ),
    LUCK_OF_THE_FOREST(
        "Luck of the Forest", 40,
        { level -> (level + 1.0).pow(3.07) },
        { level -> mapOf(HotfReward.BONUS_TREE_GIFT_LOOT to level * 0.5) },
    ),
    DAILY_WISHES(
        "Daily Wishes", 100,
        { level -> 200.0 + (level * 18.0) },
        { level ->
            mapOf(
                HotfReward.BONUS_WHISPERS_DAILY_FIG to level * 200.0,
                HotfReward.BONUS_WHISPERS_DAILY_MANGROVE to level * 200.0,
            )
        },
    ),
    GIFTS_250(
        "250 Gifts", 40,
        { level -> (level + 1.0).pow(3.07) },
        { level ->
            mapOf(
                HotfReward.BONUS_TREE_GIFT_LOOT to level * 1.0,
                HotfReward.BONUS_WHISPERS_TREE_GIFTS to 20.0,
            )
        },
    ),
    LOTTERY(
        "Lottery", 2,
        { null },
        { emptyMap() },
    ),
    FORAGING_MADNESS(
        "Foraging Madness", 2,
        { null },
        {
            mapOf(
                HotfReward.SWEEP to 10.0,
                HotfReward.FORAGING_FORTUNE to 50.0,
            )
        },
    ),
    DEEP_WATERS(
        "Deep Waters", 50,
        { level -> (level + 1.0).pow(2.9) },
        { level -> mapOf(HotfReward.PRESSURE_RESISTANCE to level * 1.0) },
    ),
    EFFICIENT_FORAGER(
        "Efficient Forager", 100,
        { level -> (level + 1.0).pow(2.6) },
        { level -> mapOf(HotfReward.FORAGING_WISDOM to 5.0 + (level * 0.1)) },
    ),
    COLLECTOR(
        "Collector", 50,
        { level -> (level + 1.0).pow(2.9) },
        { level -> mapOf(HotfReward.EXTRA_RESOURCE_CHANCE to level * 2.0) },
    ),
    EARLY_BIRD(
        "Early Bird", 2,
        { null },
        {
            mapOf(
                HotfReward.SWEEP to 10.0,
                HotfReward.FORAGING_FORTUNE to 100.0,
            )
        },
    ),
    PRECISION_CUTTING(
        "Precision Cutting", 2,
        { null },
        { emptyMap() },
    ),
    MONSTER_HUNTER(
        "Monster Hunter", 2,
        { null },
        { mapOf(HotfReward.BONUS_WHISPERS_HUNTING to 40.0) },
    ),
    TREE_WHISPERER(
        "Tree Whisperer", 2,
        { null },
        { mapOf(HotfReward.BONUS_WHISPERS_TREE_GIFTS to 200.0) },
    ),
    HOMING_AXE(
        "Homing Axe", 2,
        { null },
        { emptyMap() },
    ),
    FOREST_STRENGTH(
        "Forest Strength", 50,
        { level -> (level + 1.0).pow(3.4) },
        { level ->
            mapOf(
                HotfReward.STRENGTH_PERCENT_FORAGING_FORTUNE to level * 0.1,
                HotfReward.STRENGTH_PERCENT_SWEEP to level * 0.1,
            )
        },
    ),
    HUNTERS_LUCK(
        "Hunter's Luck", 50,
        { level -> (level + 1.0).pow(3.2) },
        { level -> mapOf(HotfReward.HUNTER_FORTUNE to level * 1.0) },
    ),
    GALATEAS_MIGHT(
        "Galatea's Might", 50,
        { level -> (level + 1.0).pow(3.2) },
        { level -> mapOf(HotfReward.BONUS_COMBAT_STATS_PERCENT to level * 0.5) },
    ),
    ESSENCE_FORTUNE(
        "Essence Fortune", 50,
        { level -> (level + 1.0).pow(3.2) },
        { level -> mapOf(HotfReward.DOUBLE_ESSENCE_CHANCE to level * 0.5) },
    ),
    FOREST_SPEED(
        "Forest Speed", 50,
        { level -> (level + 1.0).pow(3.4) },
        { level ->
            mapOf(
                HotfReward.SPEED_PERCENT_FORAGING_FORTUNE to level * 0.2,
                HotfReward.SPEED_PERCENT_SWEEP to level * 0.2,
            )
        },
    ),
    MANIAC_SLICER(
        "Maniac Slicer", 2,
        { null },
        { level ->
            mapOf(
                HotfReward.ABILITY_DURATION to 15.0 + 5 * (level - 1),
                HotfReward.ABILITY_COOLDOWN to 60.0 - (level - 1),
            )
        },
    ),
    HALF_EMPTY(
        "Half Empty", 25,
        { level -> (level + 1.0).pow(4.1) },
        { level ->
            mapOf(
                HotfReward.FORAGING_FORTUNE to level * 2.0,
                HotfReward.SWEEP to level * 1.0,
            )
        },
    ),
    RICOCHET(
        "Ricochet", 10,
        { level -> (level + 1.0).pow(5.5) },
        { level -> mapOf(HotfReward.AXE_BOUNCE_CHANCE to level * 1.0) }
    ),
    HALF_FULL(
        "Half Full", 25,
        { level -> (level + 1.0).pow(4.1) },
        { level ->
            mapOf(
                HotfReward.FORAGING_FORTUNE to level * 2.0,
                HotfReward.SWEEP to level * 1.0,
            )
        },
    ),
    CENTER_OF_THE_FOREST(
        "Center of the Forest", 5,
        { null },
        { level -> calculateCenterOfTheForestLoot(level) },
    ),
    ;

    override val guiNamePattern by patternGroup.pattern("perk.name.${name.lowercase().replace("_", "")}", "§.$guiName")

    override val printName = name.allLettersFirstUppercase()

    override val effectiveLevel: Int get() = rawLevel

    override var slot: Slot? = null
    override var item: ItemStack? = null
    override val totalCostMaxLevel = calculateTotalCost(maxLevel)
    override fun getStorage(): HotxTree? = ProfileStorageData.profileSpecific?.foraging?.hotFTree

    @SkyHanniModule
    companion object : HotxHandler<HotfData, HotfReward, HotfApi.LotteryPerk>(entries) {
        override val name: String = "HotF"
        override val core: HotfData = CENTER_OF_THE_FOREST
        override val rotatingPerks = HotfApi.LotteryPerk.entries
        override val rotatingPerkEntry = LOTTERY
        override var currentRotPerk = HotfApi.lottery
        override val applicableIslandType = IslandTypeTags.FORAGING

        override var tokens: Int
            get() = ProfileStorageData.profileSpecific?.foraging?.tokens ?: 0
            set(value) {
                ProfileStorageData.profileSpecific?.foraging?.tokens = value
            }
        override var availableTokens: Int
            get() = ProfileStorageData.profileSpecific?.mining?.availableTokens ?: 0
            set(value) {
                ProfileStorageData.profileSpecific?.mining?.availableTokens = value
            }

        var whispersCurrent: Long
            get() = ProfileStorageData.profileSpecific?.foraging?.whispers?.available ?: 0
            set(value) {
                ProfileStorageData.profileSpecific?.foraging?.whispers?.available = value
            }

        var whispersTotal: Long
            get() = ProfileStorageData.profileSpecific?.foraging?.whispers?.total ?: 0
            set(value) {
                ProfileStorageData.profileSpecific?.foraging?.whispers?.total = value
            }

        // <editor-fold desc="Patterns">
        /**
         * REGEX-TEST: §7§a§lSELECTED
         * REGEX-TEST: §a§lENABLED
         */
        override val enabledPattern: Pattern by patternGroup.pattern(
            "perk.enable",
            "§a§lENABLED|(?:§.)*SELECTED",
        )

        /**
         * REGEX-TEST: Heart of the Forest
         */
        override val inventoryPattern: Pattern by patternGroup.pattern(
            "inventory",
            "Heart of the Forest",
        )

        /**
         * REGEX-TEST: §7Level 21§8/50
         */
        override val levelPattern: Pattern by patternGroup.pattern(
            "perk.level",
            "(?:§.)*Level (?<level>\\d+).*",
        )

        /**
         * REGEX-TEST: §aForest§c!
         * REGEX-TEST: §7§cRequires Strength Boost
         * REGEX-TEST: §7§cRequires Damage Boost
         * REGEX-TEST: §7§cRequires Tier 5
         * REGEX-TEST: §7§eClick to unlock!
         */
        override val notUnlockedPattern: Pattern by patternGroup.pattern(
            "perk.notunlocked",
            "(?:§.)*Requires.*|.*Forest(?:§.)*!|(?:§.)*Click to unlock!",
        )

        /**
         * REGEX-TEST: '§aHeart of the Forest'
         */
        override val heartItemPattern: Pattern by patternGroup.pattern(
            "inventory.heart",
            "§aHeart of the Forest",
        )

        /**
         * REGEX-TEST: §cReset Heart of the Forest
         */
        override val resetItemPattern: Pattern by patternGroup.pattern(
            "inventory.reset",
            "§cReset Heart of the Forest",
        )

        /**
         * REGEX-TEST: §7Tokens of the Forest: §a0
         */
        override val heartTokensPattern: Pattern by patternGroup.pattern(
            "inventory.heart.token",
            "§7Tokens of the Forest: §a(?<token>\\d+)",
        )

        /**
         * REGEX-TEST:   §8- §a5 §aToken of the Forest
         */
        override val resetTokensPattern: Pattern by patternGroup.pattern(
            "inventory.reset.token",
            "\\s*§8- §a(?<token>\\d+) §aToken of the Forest",
        )

        /**
         * REGEX-TEST:  §7You have reset your §r§aHeart of the Forest§r§7! Your §r§aPerks §r§7and §r§aAbilities §r§7have been reset.
         */
        override val resetChatPattern by patternGroup.pattern(
            "reset.chat",
            "\\s*§7You have reset your §r§aHeart of the Forest§r§7! Your §r§aPerks §r§7and §r§aAbilities §r§7have been reset\\.",
        )

        /**
         * REGEX-TEST: §7Forest Whispers: §325,271
         */
        private val whisperHeartPattern by patternGroup.pattern(
            "whisper.heart",
            "§7Forest Whispers: §.(?<whisper>[\\d,]*)",
        )

        /**
         * REGEX-TEST:  §8- §3114,060 Forest Whispers
         */
        private val whisperResetPattern by patternGroup.pattern(
            "whisper.reset",
            "\\s+§8- §.(?<whisper>[\\d,]*) Forest Whispers",
        )
        // </editor-fold>

        override val readingLevelTransform: Matcher.() -> Int = {
            group("level").toInt()
        }

        override fun extraInventoryHandling() {
            // Hi I'm not empty
        }

        override fun Slot.extraHandling(entry: HotfData, lore: List<String>) {
            // Hi I'm not empty
        }

        override fun extraChatHandling(event: SkyHanniChatEvent.Allow) {
            // Hi I'm not empty
        }

        override fun tryBlock(event: SkyHanniChatEvent.Allow) {
            if (!chatConfig.hideLottery || IslandTypeTags.FORAGING.inAny()) return
            event.blockedReason = "lottery"
        }

        override fun readFromHeartOrReset(line: String, isHeartItem: Boolean) {
            val pattern = if (isHeartItem) whisperHeartPattern else whisperResetPattern
            val whisper = pattern.matchMatcher(line) { group("whisper").formatLong() } ?: return
            if (isHeartItem) {
                whispersCurrent = whisper
                whispersTotal = whisper
            } else {
                whispersTotal += whisper
            }
        }

        override fun currencyReset(full: Boolean) {
            super.currencyReset(full)
            if (full) {
                whispersCurrent = 0
                whispersTotal = 0
            } else {
                whispersCurrent = whispersTotal
            }
        }

        @HandleEvent(onlyOnSkyblock = true)
        override fun onChat(event: SkyHanniChatEvent.Allow) = super.onChat(event)

        @HandleEvent
        fun onDebug(event: DebugDataCollectEvent) {
            event.title("HotF")
            event.addIrrelevant {
                add("Tokens : $availableTokens/$tokens")
                add("Whisper : $whispersCurrent/$whispersTotal")
            }
            debugTree(event)
        }

    }
}

private val chatConfig get() = SkyHanniMod.feature.chat

private val patternGroup = RepoPattern.group("foraging.hotf")

enum class HotfReward {
    ABILITY_COOLDOWN,
    ABILITY_DURATION,
    ABILITY_LEVEL,
    AXE_BOUNCE_CHANCE,
    AXE_DAMAGE_MULTIPLIER,
    BONUS_COMBAT_STATS_PERCENT,
    BONUS_TREE_GIFT_LOOT,
    BONUS_WHISPERS_DAILY_FIG,
    BONUS_WHISPERS_DAILY_MANGROVE,
    BONUS_WHISPERS_HUNTING,
    BONUS_WHISPERS_LOGS,
    BONUS_WHISPERS_TREE_GIFTS,
    DOUBLE_ESSENCE_CHANCE,
    EXTRA_RESOURCE_CHANCE,
    EXTRA_TOKENS,
    FORAGING_FORTUNE,
    FORAGING_WISDOM,
    HUNTER_FORTUNE,
    PRESSURE_RESISTANCE,
    SPEED,
    SPEED_PERCENT_FORAGING_FORTUNE,
    SPEED_PERCENT_SWEEP,
    STRENGTH,
    STRENGTH_PERCENT_FORAGING_FORTUNE,
    STRENGTH_PERCENT_SWEEP,
    SWEEP,
    SWEEP_PERCENT,
    THROW_PENALTY_REDUCTION,
}
