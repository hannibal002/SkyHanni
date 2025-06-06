package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.local.HotxTree
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import java.util.regex.Matcher
import java.util.regex.Pattern

enum class HotfData(
    override val guiName: String,
    override val maxLevel: Int,
    override val costFun: (Int) -> (Double?),
    override val rewardFun: (Int) -> (Map<HotfReward, Double>),
) : HotxData<HotfReward> {
    SWEEP(
        "§eSweep", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    FORAGING_FORTUNE(
        "§eForaging Fortune", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    STRENGTH_BOOST(
        "§cStrength Boost", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    DAMAGE_BOOST(
        "§cDamage Boost", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    SPEED_BOOST(
        "§eSpeed Boost", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    AXE_TOSS(
        "§aAxe Toss", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    LUCK_OF_THE_FOREST(
        "§cLuck of the Forest", 40,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    DAILY_WISHES(
        "§cDaily Wishes", 100,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    GIFTS_250(
        "§e250 Gifts(", 40,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    LOTTERY(
        "§cLottery", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    FORAGING_MADNESS(
        "§cForaging Madness", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    DEEP_WATERS(
        "§cDeep Waters", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    EFFICIENT_FORAGER(
        "§cEfficient Forager", 100,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    COLLECTOR(
        "§cCollector", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    EARLY_BIRD(
        "§cEarly Bird", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    PRECISION_CUTTING(
        "§cPrecision Cutting", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    MONSTER_HUNTER(
        "§cMonster Hunter", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    TREE_WHISPERER(
        "§cTree Whisperer", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    HOMING_AXE(
        "§cHoming Axe", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    FOREST_STRENGTH(
        "§cForest Strength", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    HUNTERS_LUCK(
        "§cHunter's Luck", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    GALATEAS_MIGHT(
        "§cGalatea's Might", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    ESSENCE_FORTUNE(
        "§cEssence Fortune", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    FOREST_SPEED(
        "§cForest Speed", 50,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    MANIAC_SLICER(
        "§cManiac Slicer", 2,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    HALF_EMPTY(
        "§cHalf Empty", 25,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    RICOCHET(
        "§cRicochet", 10,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    HALF_FULL(
        "§cHalf Full", 25,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
    ),
    CENTER_OF_THE_FOREST(
        "§cCenter of the Forest", 5,
        { level ->
            0.0 // TODO
        },
        { level ->
            mapOf() // TODO
        },
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
    companion object : HotxHandler<HotfData, HotfReward>() {
        override val name: String = "HotF"
        override val core: HotfData = CENTER_OF_THE_FOREST
        override val data = entries
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
            "(?:§.)*Requires.*|.*Forest(§.)*!|(?:§.)*Click to unlock!",
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
            "§7Tokens of the Forest: §a0",
        )

        /**
         * REGEX-TEST:   §8- §a5 §aToken of the Forest
         */
        override val resetTokensPattern: Pattern by patternGroup.pattern(
            "inventory.reset.token",
            "\\s*§8- §a5 §aToken of the Forest",
        )

        /**
         * REGEX-TEST:  §7You have reset your §r§aHeart of the Forest§r§7! Your §r§aPerks §r§7and §r§aAbilities §r§7have been reset.
         */
        private val resetChatPattern by patternGroup.pattern(
            "reset.chat",
            "\\s*§7You have reset your §r§aHeart of the Forest§r§7! Your §r§aPerks §r§7and §r§aAbilities §r§7have been reset\\.",
        )

        /**
         * REGEX-TEST: §7Cost
         */
        private val perkCostPattern by patternGroup.pattern(
            "perk.cost",
            "(?:§.)*§7Cost",
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

        override val readingLevelTransform: Matcher.() -> Int = {
            group("level").toInt()
        }

        override fun extraInventoryHandling() {}

        override fun Slot.extraHandling(entry: HotfData, lore: List<String>) {}

        override fun readFromHeartOrReset(line: String, isHeartItem: Boolean) {
            (if (isHeartItem) whisperHeartPattern else whisperResetPattern).matchMatcher(line) {
                val whisper = group("whisper").formatLong()
                if (isHeartItem) {
                    whispersCurrent = whisper
                    whispersTotal = whisper
                } else {
                    whispersTotal += whisper
                }
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

        @HandleEvent
        override fun onInventoryClose(event: InventoryCloseEvent) = super.onInventoryClose(event)

        @HandleEvent(onlyOnSkyblock = true)
        override fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) = super.onInventoryFullyOpened(event)

        @HandleEvent(onlyOnSkyblock = true)
        fun onChat(event: SkyHanniChatEvent) {
            if (resetChatPattern.matches(event.message)) {
                resetTree()
                return
            }
        }

        @HandleEvent
        fun onDebug(event: DebugDataCollectEvent) {
            event.title("HotF")
            event.addIrrelevant {
                add("Tokens : ${availableTokens}/${tokens}")
                add("Whisper : ${whispersCurrent}/${whispersTotal}")
            }
            debugTree(event)
        }

    }
}

private val patternGroup = RepoPattern.group("foraging.hotf")

enum class HotfReward
