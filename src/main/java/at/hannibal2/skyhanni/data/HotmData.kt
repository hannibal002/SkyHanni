package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.HotmAPI
import at.hannibal2.skyhanni.api.HotmAPI.MayhemPerk
import at.hannibal2.skyhanni.api.HotmAPI.SkymallPerk
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.jsonobjects.local.HotmTree
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.indexOfFirstMatch
import at.hannibal2.skyhanni.utils.RegexUtils.matchFirst
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

enum class HotmData(
    guiName: String,
    val maxLevel: Int,
    val costFun: ((Int) -> (Double?)),
    val rewardFun: ((Int) -> (Map<HotmReward, Double>)),
) {

    MINING_SPEED("Mining Speed",
        50,
        { currentLevel -> (currentLevel + 2.0).pow(3) },
        { level -> mapOf(HotmReward.MINING_SPEED to level * 20.0) }),
    MINING_FORTUNE("Mining Fortune",
        50,
        { currentLevel -> (currentLevel + 2.0).pow(3.05) },
        { level -> mapOf(HotmReward.MINING_FORTUNE to level * 5.0) }),
    QUICK_FORGE("Quick Forge",
        20,
        { currentLevel -> (currentLevel + 2.0).pow(4) },
        { level -> mapOf(HotmReward.FORGE_TIME_DECREASE to 10.0 + (level * 0.5)) }),
    TITANIUM_INSANIUM("Titanium Insanium",
        50,
        { currentLevel -> (currentLevel + 2.0).pow(3.1) },
        { level -> mapOf(HotmReward.TITANIUM_CHANCE to 2.0 + (level * 0.1)) }),
    DAILY_POWDER("Daily Powder",
        100,
        { currentLevel -> 200.0 + (currentLevel * 18.0) },
        { level -> mapOf(HotmReward.DAILY_POWDER to (200.0 + ((level - 1.0) * 18.0)) * 2.0) }),
    LUCK_OF_THE_CAVE("Luck of the Cave",
        45,
        { currentLevel -> (currentLevel + 2.0).pow(3.07) },
        { level -> mapOf(HotmReward.EXTRA_CHANCE_TRIGGER_RARE_OCCURRENCES to 5.0 + level) }),
    CRYSTALLIZED("Crystallized", 30, { currentLevel -> (currentLevel + 2.0).pow(3.4) }, { level ->
        mapOf(
            HotmReward.MINING_SPEED to 20.0 + ((level - 1.0) * 6.0),
            HotmReward.MINING_FORTUNE to 20.0 + ((level - 1.0) * 5.0)
        )
    }),
    EFFICIENT_MINER("Efficient Miner",
        100,
        { currentLevel -> (currentLevel + 2.0).pow(2.6) },
        { level -> mapOf(HotmReward.AVERAGE_BLOCK_BREAKS to (10.0 + (level * 0.4)) * (1.0 + (level * 0.05))) }),
    ORBITER("Orbiter",
        80,
        { currentLevel -> (currentLevel + 1.0) * 70.0 },
        { level -> mapOf(HotmReward.CHANCE_EXTRA_XP_ORBS to 0.2 + (level * 0.01)) }),
    SEASONED_MINEMAN("Seasoned Mineman",
        100,
        { currentLevel -> (currentLevel + 2.0).pow(2.3) },
        { level -> mapOf(HotmReward.MINING_WISDOM to 5.0 + (level * 0.1)) }),
    MOLE("Mole",
        190,
        { currentLevel -> (currentLevel + 2.0).pow(2.2) },
        { level -> mapOf(HotmReward.AVERAGE_BLOCK_BREAKS to 1.0 + ((level + 9.0) * 0.05 * ((level + 8) % 20))) }),
    PROFESSIONAL("Professional",
        140,
        { currentLevel -> (currentLevel + 2.0).pow(2.3) },
        { level -> mapOf(HotmReward.MINING_SPEED to 50.0 + (level * 5.0)) }),
    LONESOME_MINER("Lonesome Miner",
        45,
        { currentLevel -> (currentLevel + 2.0).pow(3.07) },
        { level -> mapOf(HotmReward.COMBAT_STAT_BOOST to 5.0 + ((level - 1.0) * 0.5)) }),
    GREAT_EXPLORER("Great Explorer", 20, { currentLevel -> (currentLevel + 2.0).pow(4.0) }, { level ->
        mapOf(
            HotmReward.CHANCE_OF_TREASURE_CHEST to (0.2 * (0.2 + 0.04 * (level - 1.0))),
            HotmReward.LOCKS_OF_TREASURE_CHEST to 1 + level * 0.2
        )
    }),
    FORTUNATE("Fortunate",
        20,
        { currentLevel -> (currentLevel + 1.0).pow(3.05) },
        { level -> mapOf(HotmReward.MINING_FORTUNE to 20.0 + (level * 4.0)) }),
    POWDER_BUFF("Powder Buff", 50, { currentLevel -> (currentLevel + 1.0).pow(3.2) }, { level ->
        mapOf(
            HotmReward.MORE_MITHRIL_POWER to level.toDouble(), HotmReward.MORE_GEMSTONE_POWER to level.toDouble()
        )
    }),
    MINING_SPEED_II("Mining Speed II",
        50,
        { currentLevel -> (currentLevel + 2.0).pow(3.2) },
        { level -> mapOf(HotmReward.MINING_SPEED to level * 40.0) }),
    MINING_FORTUNE_II("Mining Fortune II",
        50,
        { currentLevel -> (currentLevel + 2.0).pow(3.2) },
        { level -> mapOf(HotmReward.MINING_FORTUNE to level * 5.0) }),

    // Static

    MINING_MADNESS("Mining Madness", 1, { null }, {
        mapOf(
            HotmReward.MINING_SPEED to 50.0, HotmReward.MINING_FORTUNE to 50.0
        )
    }),
    SKY_MALL("Sky Mall", 1, { null }, { emptyMap() }),
    PRECISION_MINING("Precision Mining", 1, { null }, { mapOf(HotmReward.MINING_SPEED_BOOST to 30.0) }),
    FRONT_LOADED("Front Loaded", 1, { null }, {
        mapOf(
            HotmReward.MINING_SPEED to 100.0,
            HotmReward.MINING_FORTUNE to 100.0,
            HotmReward.MORE_BASE_MITHRIL_POWER to 2.0,
            HotmReward.MORE_BASE_GEMSTONE_POWER to 2.0
        )
    }),
    STAR_POWDER("Star Powder", 1, { null }, { mapOf(HotmReward.MORE_MITHRIL_POWER to 300.0) }),
    GOBLIN_KILLER("Goblin Killer", 1, { null }, { emptyMap() }),

    // Abilities

    PICKOBULUS("Pickobulus", 3, { null }, { level ->
        mapOf(
            HotmReward.ABILITY_RADIUS to ceil(level * 0.5) + 1.0,
            HotmReward.ABILITY_COOLDOWN to 130.0 - 10.0 * level
        )
    }),
    MINING_SPEED_BOOST("Mining Speed Boost", 3, { null }, { level ->
        mapOf(
            HotmReward.ABILITY_DURATION to level + 1.0, HotmReward.ABILITY_COOLDOWN to 10.0 + 5.0 * level
        )
    }),
    VEIN_SEEKER("Vein Seeker", 3, { null }, { level ->
        mapOf(
            HotmReward.ABILITY_RADIUS to level + 1.0,
            HotmReward.ABILITY_DURATION to 10.0 + 2.0 * level,
            HotmReward.ABILITY_COOLDOWN to 60.0
        )
    }),
    MANIAC_MINER("Maniac Miner", 3, { null }, { level ->
        mapOf(
            HotmReward.ABILITY_DURATION to 5.0 + level * 5.0, HotmReward.ABILITY_COOLDOWN to 60.0 - level
        )
    }),

    PEAK_OF_THE_MOUNTAIN("Peak of the Mountain", 10, { null }, { emptyMap() }),

    // Mining V3
    DAILY_GRIND("Daily Grind",
        100,
        { currentLevel -> 218.0 + (18.0 * (currentLevel - 2.0)) },
        { level -> mapOf(HotmReward.DAILY_POWDER to 50.0 * level) }),
    DUST_COLLECTOR(
        "Dust Collector",
        20,
        { currentLevel -> (currentLevel + 1.0).pow(4) },
        { level -> mapOf(HotmReward.FOSSIL_DUST to 1.0 * level) }),
    WARM_HEARTED("Warm Hearted",
        50,
        { currentLevel -> floor((currentLevel + 1.0).pow(3.1)) },
        { level -> mapOf(HotmReward.COLD_RESISTANCE to 0.2 * level) }),

    STRONG_ARM("Strong Arm",
        100,
        { currentLevel -> floor((currentLevel + 1.0).pow(2.3)) },
        { level -> mapOf(HotmReward.MINING_SPEED to 5.0 * level) }),
    NO_STONE_UNTURNED("No Stone Unturned",
        50,
        { currentLevel -> floor((currentLevel + 1.0).pow(3.05)) },
        { level -> mapOf(HotmReward.UNKNOWN to 0.5 * level) }),

    SUB_ZERO_MINING("SubZero Mining",
        100,
        { currentLevel -> floor((currentLevel + 1.0).pow(2.3)) },
        { level -> mapOf(HotmReward.MINING_FORTUNE to 1.0 * level) }),
    SURVEYOR("Surveyor",
        20,
        { currentLevel -> (currentLevel + 1.0).pow(4) },
        { level -> mapOf(HotmReward.UNKNOWN to 0.75 * level) }),
    EAGER_ADVENTURER("Eager Adventurer",
        100,
        { currentLevel -> floor((currentLevel + 1.0).pow(2.3)) },
        { level -> mapOf(HotmReward.MINING_SPEED to 2.0 * level) }),

    DEAD_MANS_CHEST("Dead Man's Chest",
        50,
        { currentLevel -> floor((currentLevel + 1.0).pow(3.2)) },
        { level -> mapOf(HotmReward.UNKNOWN to 1.0 * level) }),

    GIFTS_FROM_THE_DEPARTED("Gifts from the Departed",
        100,
        { currentLevel -> floor((currentLevel + 1.0).pow(2.45)) },
        { level -> mapOf(HotmReward.UNKNOWN to 0.2 * level) }),

    EXCAVATOR(
        "Excavator",
        50,
        { currentLevel -> (currentLevel + 1.0).pow(3) },
        { level -> mapOf(HotmReward.UNKNOWN to 0.5 * level) }),
    RAGS_TO_RICHES("Rags to Riches",
        50,
        { currentLevel -> floor((currentLevel + 1.0).pow(3.05)) },
        { level -> mapOf(HotmReward.MINING_FORTUNE to 2.0 * level) }),

    KEEN_EYE("Keen Eye", 1, { null }, { emptyMap() }),
    MINESHAFT_MAYHEM("Mineshaft Mayhem", 1, { null }, { emptyMap() }),
    FROZEN_SOLID("Frozen Solid", 1, { null }, { emptyMap() }),
    GEMSTONE_INFUSION("Gemstone Infusion", 1, { null }, { emptyMap() }),
    HAZARDOUS_MINER("Hazardous Miner", 1, { null }, { emptyMap() }),

    ;

    private val guiNamePattern by patternGroup.pattern("perk.name.${name.lowercase().replace("_", "")}", "§.$guiName")

    val printName = name.allLettersFirstUppercase()

    val rawLevel: Int
        get() = storage?.perks?.get(this.name)?.level ?: 0

    var activeLevel: Int
        get() = storage?.perks?.get(this.name)?.level?.plus(blueEgg()) ?: 0
        private set(value) {
            storage?.perks?.computeIfAbsent(this.name) { HotmTree.HotmPerk() }?.level = value
        }

    val isMaxLevel: Boolean
        get() = activeLevel >= maxLevel // >= to account for +1 from Blue Cheese

    private fun blueEgg() = if (this != PEAK_OF_THE_MOUNTAIN && maxLevel != 1 && HotmAPI.isBlueEggActive) 1 else 0

    var enabled: Boolean
        get() = storage?.perks?.get(this.name)?.enabled ?: false
        private set(value) {
            storage?.perks?.computeIfAbsent(this.name) { HotmTree.HotmPerk() }?.enabled = value
        }

    var isUnlocked: Boolean
        get() = storage?.perks?.get(this.name)?.isUnlocked ?: false
        private set(value) {
            storage?.perks?.computeIfAbsent(this.name) { HotmTree.HotmPerk() }?.isUnlocked = value
        }

    var slot: Slot? = null
        private set

    fun getLevelUpCost() = costFun(rawLevel)

    fun getReward() = rewardFun(activeLevel)

    @SkyHanniModule
    companion object {

        val storage get() = ProfileStorageData.profileSpecific?.mining?.hotmTree

        val abilities =
            listOf(PICKOBULUS, MINING_SPEED_BOOST, VEIN_SEEKER, MANIAC_MINER, HAZARDOUS_MINER, GEMSTONE_INFUSION)

        private val inventoryPattern by patternGroup.pattern(
            "inventory", "Heart of the Mountain"
        )

        private val levelPattern by patternGroup.pattern(
            "perk.level", "§(?<color>.)Level (?<level>\\d+).*"
        )

        private val notUnlockedPattern by patternGroup.pattern(
            "perk.notunlocked", "(§.)*Requires.*|.*Mountain!|(§.)*Click to unlock!|"
        )

        private val enabledPattern by patternGroup.pattern(
            "perk.enable", "§a§lENABLED|(§.)*SELECTED"
        )
        private val disabledPattern by patternGroup.pattern(
            "perk.disabled", "§c§lDISABLED|§7§eClick to select!"
        ) // unused for now since the assumption is when enabled isn't found it is disabled, but the value might be useful in the future or for debugging

        private val resetChatPattern by patternGroup.pattern(
            "reset.chat", "§aReset your §r§5Heart of the Mountain§r§a! Your Perks and Abilities have been reset."
        )

        private val heartItemPattern by patternGroup.pattern(
            "inventory.heart", "§5Heart of the Mountain"
        )
        private val resetItemPattern by patternGroup.pattern(
            "inventory.reset", "§cReset Heart of the Mountain"
        )

        private val heartTokensPattern by patternGroup.pattern(
            "inventory.heart.token", "§7Token of the Mountain: §5(?<token>\\d+)"
        )

        private val resetTokensPattern by patternGroup.pattern(
            "inventory.reset.token", "\\s+§8- §5(?<token>\\d+) Token of the Mountain"
        )

        private val skymallPattern by patternGroup.pattern(
            "skymall", "(?:§eNew buff§r§r§r: §r§f|§8 ■ §7)(?<perk>.*)"
        )

        private val mayhemChatPattern by patternGroup.pattern(
            "mayhem", "§b§lMAYHEM! §r§7(?<perk>.*)"
        )

        var inInventory = false

        var tokens: Int
            get() = ProfileStorageData.profileSpecific?.mining?.tokens ?: 0
            private set(value) {
                ProfileStorageData.profileSpecific?.mining?.tokens = value
            }

        var availableTokens: Int
            get() = ProfileStorageData.profileSpecific?.mining?.availableTokens ?: 0
            private set(value) {
                ProfileStorageData.profileSpecific?.mining?.availableTokens = value
            }

        var heartItem: Slot? = null

        init {
            entries.forEach { it.guiNamePattern }
            HotmAPI.Powder.entries.forEach {
                it.heartPattern
                it.resetPattern
            }
            HotmAPI.SkymallPerk.entries.forEach {
                it.chatPattern
                it.itemPattern
            }
            HotmAPI.MayhemPerk.entries.forEach {
                it.chatPattern
            }
        }

        private fun resetTree() = entries.forEach {
            it.activeLevel = 0
            it.enabled = false
            it.isUnlocked = false
            HotmAPI.Powder.entries.forEach { it.setCurrent(it.getTotal()) }
            availableTokens = tokens
        }

        private fun Slot.parse() {
            val item = this.stack ?: return

            if (this.handlePowder()) return

            val entry = entries.firstOrNull { it.guiNamePattern.matches(item.name) } ?: return
            entry.slot = this

            val lore = item.getLore().takeIf { it.isNotEmpty() } ?: return

            if (entry != PEAK_OF_THE_MOUNTAIN && notUnlockedPattern.matches(lore.last())) {
                entry.activeLevel = 0
                entry.enabled = false
                entry.isUnlocked = false
                return
            }

            entry.isUnlocked = true

            entry.activeLevel = levelPattern.matchMatcher(lore.first()) {
                group("level").toInt().transformIf({ group("color") == "b" }, { this.minus(1) })
            } ?: entry.maxLevel

            // max level + 1 because Blue Cheese Goblin Omelette adds +1 to each level
            if (entry.activeLevel > entry.maxLevel + 1) {
                ErrorManager.skyHanniError(
                    "Hotm Perk '${entry.name}' over max level",
                    "name" to entry.name,
                    "activeLevel" to entry.activeLevel,
                    "maxLevel" to entry.maxLevel,
                )
            }

            if (entry == PEAK_OF_THE_MOUNTAIN) {
                entry.enabled = entry.activeLevel != 0
                return
            }
            entry.enabled = lore.any { enabledPattern.matches(it) }

            if (entry == SKY_MALL) handelSkyMall(lore)
        }

        private fun Slot.handlePowder(): Boolean {
            val item = this.stack ?: return false

            val isHeartItem = when {
                heartItemPattern.matches(item.name) -> true
                resetItemPattern.matches(item.name) -> false
                else -> return false
            }

            if (isHeartItem) { // Reset on the heart Item to remove duplication
                tokens = 0
                availableTokens = 0
                HotmAPI.Powder.entries.forEach { it.reset() }
                heartItem = this
            }

            val lore = item.getLore()

            val tokenPattern = if (isHeartItem) heartTokensPattern else resetTokensPattern

            lore@ for (line in lore) {

                HotmAPI.Powder.entries.forEach {
                    it.pattern(isHeartItem).matchMatcher(line) {
                        val powder = group("powder").replace(",", "").toLong()
                        if (isHeartItem) {
                            it.setCurrent(powder)
                        }
                        it.addTotal(powder)
                        continue@lore
                    }
                }

                tokenPattern.matchMatcher(line) {
                    val token = group("token").toInt()
                    if (isHeartItem) {
                        availableTokens = token
                    }
                    tokens += token
                    continue@lore
                }
            }
            return true
        }

        private val skyMallCurrentEffect by patternGroup.pattern(
            "skymall.current", "§aYour Current Effect"
        )

        private fun handelSkyMall(lore: List<String>) {
            if (!SKY_MALL.enabled || !SKY_MALL.isUnlocked) HotmAPI.skymall = null
            else {
                val index = (lore.indexOfFirstMatch(skyMallCurrentEffect) ?: run {
                    ErrorManager.logErrorStateWithData(
                        "Could not read the skymall effect from the hotm tree",
                        "skyMallCurrentEffect didn't match",
                        "lore" to lore
                    )
                    return
                }) + 1
                skymallPattern.matchMatcher(lore[index]) {
                    val perk = group("perk")
                    HotmAPI.skymall = SkymallPerk.entries.firstOrNull { it.itemPattern.matches(perk) } ?: run {
                        ErrorManager.logErrorStateWithData(
                            "Could not read the skymall effect from the hotm tree",
                            "no itemPattern matched",
                            "lore" to lore,
                            "perk" to perk
                        )
                        null
                    }
                }
            }
        }

        @SubscribeEvent
        fun onScoreboardUpdate(event: ScoreboardChangeEvent) {
            if (!LorenzUtils.inSkyBlock) return

            event.newList.matchFirst(ScoreboardPattern.powderPattern) {
                val type = HotmAPI.Powder.entries.firstOrNull { it.lowName == group("type") } ?: return
                val amount = group("amount").formatLong()
                val difference = amount - type.getCurrent()

                if (difference > 0) {
                    type.gain(difference)
                    ChatUtils.debug("Gained §a${difference.addSeparators()} §e${type.lowName} Powder")
                }
            }
        }

        @SubscribeEvent
        fun onInventoryClose(event: InventoryCloseEvent) {
            if (!inInventory) return
            inInventory = false
            entries.forEach { it.slot = null }
            heartItem = null
        }

        @SubscribeEvent
        fun onInventoryFullyOpen(event: InventoryFullyOpenedEvent) {
            if (!LorenzUtils.inSkyBlock) return
            inInventory = inventoryPattern.matches(event.inventoryName)
            DelayedRun.runNextTick {
                InventoryUtils.getItemsInOpenChest().forEach { it.parse() }
                abilities.filter { it.isUnlocked }.forEach {
                    it.activeLevel = if (PEAK_OF_THE_MOUNTAIN.rawLevel >= 1) 2 else 1
                }
            }
        }

        @SubscribeEvent
        fun onChat(event: LorenzChatEvent) {
            if (!LorenzUtils.inSkyBlock) return
            if (resetChatPattern.matches(event.message)) {
                resetTree()
                return
            }
            skymallPattern.matchMatcher(event.message) {
                val perk = group("perk")
                HotmAPI.skymall = SkymallPerk.entries.firstOrNull { it.chatPattern.matches(perk) } ?: run {
                    ErrorManager.logErrorStateWithData(
                        "Could not read the skymall effect from chat",
                        "no chatPattern matched",
                        "chat" to event.message,
                        "perk" to perk
                    )
                    null
                }
                ChatUtils.debug("setting skymall to ${HotmAPI.skymall}")
                return
            }
            DelayedRun.runNextTick {
                mayhemChatPattern.matchMatcher(event.message) {
                    val perk = group("perk")
                    HotmAPI.mineshaftMayhem = MayhemPerk.entries.firstOrNull { it.chatPattern.matches(perk) } ?: run {
                        ErrorManager.logErrorStateWithData(
                            "Could not read the mayhem effect from chat",
                            "no chatPattern matched",
                            "chat" to event.message,
                            "perk" to perk
                        )
                        null
                    }
                    ChatUtils.debug("setting mineshaftMayhem to ${HotmAPI.mineshaftMayhem}")
                }
            }
        }

        @SubscribeEvent
        fun onWorldSwitch(event: IslandChangeEvent) {
            if (HotmAPI.mineshaftMayhem == null) return
            HotmAPI.mineshaftMayhem = null
            ChatUtils.debug("resetting mineshaftMayhem")
        }

        @SubscribeEvent
        fun onProfileSwitch(event: ProfileJoinEvent) {
            HotmAPI.Powder.entries.forEach {
                if (it.getStorage() == null) {
                    ProfileStorageData.profileSpecific?.mining?.powder?.put(
                        it, ProfileSpecificStorage.MiningConfig.PowderStorage()
                    )
                }
            }
        }

        @SubscribeEvent
        fun onDebug(event: DebugDataCollectEvent) {
            event.title("HotM")
            event.addIrrelevant {
                add("Tokens : $availableTokens/$tokens")
                HotmAPI.Powder.entries.forEach {
                    add("${it.lowName} Powder: ${it.getCurrent()}/${it.getTotal()}")
                }
                add("Ability: ${HotmAPI.activeMiningAbility?.printName}")
                add("Blue Egg: ${HotmAPI.isBlueEggActive}")
                add("SkyMall: ${HotmAPI.skymall}")
                add("Mineshaft Mayhem: ${HotmAPI.mineshaftMayhem}")
            }
            event.title("HotM - Tree")
            event.addIrrelevant(entries.filter { it.isUnlocked }.map {
                "${if (it.enabled) "✔" else "✖"} ${it.printName}: ${it.activeLevel}"
            })
        }
    }
}

private val patternGroup = RepoPattern.group("mining.hotm")

enum class HotmReward {
    MINING_SPEED,
    MINING_FORTUNE,
    MINING_WISDOM,
    FORGE_TIME_DECREASE,
    TITANIUM_CHANCE,
    DAILY_POWDER,
    MORE_BASE_MITHRIL_POWER,
    MORE_BASE_GEMSTONE_POWER,
    MORE_MITHRIL_POWER,
    MORE_GEMSTONE_POWER,
    COMBAT_STAT_BOOST,
    CHANCE_OF_TREASURE_CHEST,
    LOCKS_OF_TREASURE_CHEST,
    EXTRA_CHANCE_TRIGGER_RARE_OCCURRENCES,
    AVERAGE_BLOCK_BREAKS,
    CHANCE_EXTRA_XP_ORBS,
    MINING_SPEED_BOOST,
    ABILITY_DURATION,
    ABILITY_RADIUS,
    ABILITY_COOLDOWN,
    FOSSIL_DUST,
    UNKNOWN,
    COLD_RESISTANCE
}
