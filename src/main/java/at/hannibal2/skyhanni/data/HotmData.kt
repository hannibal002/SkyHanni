package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.jsonobjects.local.HotmTree
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.ceil
import kotlin.math.pow

private val repoGroup = RepoPattern.group("mining.hotm")

enum class HotmData(
    guiName: String,
    val maxLevel: Int,
    val costFun: ((Int) -> (Double?)),
    val rewardFun: ((Int) -> (Pair<Double, Double?>)) // TODO use a result class as retrun
) {
    MINING_SPEED("Mining Speed",
        50,
        { currentLevel -> (currentLevel + 2.0).pow(3) },
        { level -> level * 20.0 to null }),
    MINING_FORTUNE("Mining Fortune",
        50,
        { currentLevel -> (currentLevel + 1.0).pow(3.5) },
        { level -> level * 5.0 to null }),
    QUICK_FORGE("Quick Forge",
        20,
        { currentLevel -> (currentLevel + 2.0).pow(4) },
        { level -> 10.0 + (level * 0.5) to null }),
    TITANIUM_INSANIUM("Titanium Insanium",
        50,
        { currentLevel -> (currentLevel + 2.0).pow(3.1) },
        { level -> 2.0 + (level * 0.1) to null }),
    DAILY_POWDER("Daily Powder",
        100,
        { currentLevel -> 200.0 + (currentLevel * 18.0) },
        { level -> (200.0 + ((level - 1.0) * 18.0)) * 2.0 to null }),
    LUCK_OF_THE_CAVE("Luck of the Cave",
        45,
        { currentLevel -> (currentLevel + 2.0).pow(3.07) },
        { level -> 5.0 + level to null }),
    CRYSTALLIZED("Crystallized",
        30,
        { currentLevel -> (currentLevel + 2.0).pow(3.4) },
        { level -> 20.0 + ((level - 1.0) * 6.0) to 20.0 + ((level - 1.0) * 5.0) }),
    EFFICIENT_MINER("Efficient Miner",
        100,
        { currentLevel -> (currentLevel + 2.0).pow(2.6) },
        { level -> 10.0 + (level * 0.4) to 1.0 + (level * 0.05) }),
    ORBITER("Orbiter",
        80,
        { currentLevel -> (currentLevel + 1.0) * 70.0 },
        { level -> 0.2 + (level * 0.01) to null }),
    SEASONED_MINEMAN("Seasoned Mineman",
        100,
        { currentLevel -> (currentLevel + 2.0).pow(2.3) },
        { level -> 5.0 + (level * 0.1) to null }),
    MOLE("Mole",
        190,
        { currentLevel -> (currentLevel + 2.0).pow(2.2) },
        { level -> 1.0 + ((level + 9.0) * 0.05 * ((level + 8) % 20)) to null }),
    PROFESSIONAL("Professional",
        140,
        { currentLevel -> (currentLevel + 2.0).pow(2.3) },
        { level -> 50.0 + (level * 5.0) to null }),
    LONESOME_MINER("Lonesome Miner",
        45,
        { currentLevel -> (currentLevel + 2.0).pow(3.07) },
        { level -> 5.0 + ((level - 1.0) * 0.5) to null }),
    GREAT_EXPLORER("Great Explorer",
        20,
        { currentLevel -> (currentLevel + 2.0).pow(4.0) },
        { level -> (0.2 * (0.2 + 0.04 * (level - 1.0))) to 1 + level * 0.2 }),
    FORTUNATE("Fortunate",
        20,
        { currentLevel -> (currentLevel + 1.0).pow(3.05) },
        { level -> 20.0 + (level * 4.0) to null }),
    POWDER_BUFF("Powder Buff",
        50,
        { currentLevel -> (currentLevel + 1.0).pow(3.2) },
        { level -> level.toDouble() to null }),
    MINING_SPEED_II("Mining Speed II",
        50,
        { currentLevel -> (currentLevel + 2.0).pow(3.2) },
        { level -> level * 40.0 to null }),
    MINING_FORTUNE_II("Mining Fortune II",
        50,
        { currentLevel -> (currentLevel + 2.0).pow(3.2) },
        { level -> level * 5.0 to null }),

    // Static
    MINING_MADNESS("Mining Madness", 1, { null }, { 50.0 to 50.0 }), SKY_MALL("Sky Mall",
        1,
        { null },
        { 0.0 to null }),
    PRECISION_MINING("Precision Mining", 1, { null }, { 30.0 to null }), FRONT_LOADED("Front Loaded",
        1,
        { null },
        { 100.0 to 2.0 }),
    STAR_POWDER("Star Powder", 1, { null }, { 3.0 to null }), GOBLIN_KILLER("Goblin Killer",
        1,
        { null },
        { 0.0 to null }),

    // Abilities
    PICKOBULUS("Pickobulus",
        3,
        { null },
        { level -> ceil(level * 0.5) + 1.0 to 130.0 - 10.0 * level }),
    MINING_SPEED_BOOST("Mining Speed Boost",
        3,
        { null },
        { level -> level + 1.0 to 10.0 + 5.0 * level }),
    VEIN_SEEKER("Vein Seeker",
        3,
        { null },
        { level -> level + 1.0 to 10.0 + 2.0 * level }),
    MANIAC_MINER("Maniac Miner", 3, { null }, { level -> 5.0 + level * 5.0 to 60.0 - level }),

    PEAK_OF_THE_MOUNTAIN("Peak of the Mountain", 7, { null }, { 0.0 to null }), ;

    val guiNamePattern by repoGroup.pattern("perk.name.${name.lowercase().replace("_", "")}", "§.$guiName")

    var activeLevel: Int
        get() = storage?.perks?.get(this.name)?.level ?: 0
        private set(value) {
            storage?.perks?.computeIfAbsent(this.name) { HotmTree.HotmPerk() }?.level = value
        }

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

    companion object {

        val storage get() = ProfileStorageData.profileSpecific?.mining?.hotmTree

        val abilities = listOf(PICKOBULUS, MINING_SPEED_BOOST, VEIN_SEEKER, MANIAC_MINER)

        private val inventoryPattern by repoGroup.pattern("inventory", "Heart of the Mountain")

        private val levelPattern by repoGroup.pattern("perk.level", "§7Level (?<level>\\d+).*")

        private val notUnlockedPattern by repoGroup.pattern(
            "perk.notunlocked", "§7§cRequires.*|§cMountain!|§7§eClick to unlock!"
        )

        private val enabledPattern by repoGroup.pattern("perk.enable", "§a§lENABLED|§7§a§lSELECTED")
        private val disabledPattern by repoGroup.pattern(
            "perk.disabled",
            "§c§lDISABLED|§7§eClick to select!"
        ) // unused for now since the assumption is whe enable not found it is disabled, but the value might be useful in the future or for debugging

        private val resetChatPattern by repoGroup.pattern(
            "reset.chat", "§aReset your §r§5Heart of the Mountain§r§a! Your Perks and Abilities have been reset."
        )

        private val heartItemPattern by repoGroup.pattern("invetory.heart", "§5Heart of the Mountain")
        private val resetItemPattern by repoGroup.pattern("invetory.reset", "§cReset Heart of the Mountain")

        private val heartMithrilPattern by repoGroup.pattern(
            "invetory.heart.mithril",
            "§7Mithril Powder: §a§2(?<powder>[\\d,]+)"
        )
        private val heartGemstonePattern by repoGroup.pattern(
            "invetory.heart.gemstone",
            "§7Gemstone Powder: §a§d(?<powder>[\\d,]+)"
        )

        private val heartTokensPattern by repoGroup.pattern(
            "invetory.heart.token",
            "§7Token of the Mountain: §5(?<token>\\d+)"
        )

        private val resetMithrilPattern by repoGroup.pattern(
            "invetory.reset.mithril",
            "\\s+§8- §2(?<powder>[\\d,]+) Mithril Powder"
        )
        private val resetGemstonePattern by repoGroup.pattern(
            "invetory.reset.gemstone",
            "\\s+§8- §d(?<powder>[\\d,]+) Gemstone Powder"
        )

        private val resetTokensPattern by repoGroup.pattern(
            "invetory.reset.token",
            "\\s+§8- §5(?<token>\\d+) Token of the Mountain"
        )

        var inInventory = false

        var mithrilPowder: Long
            get() = ProfileStorageData.profileSpecific?.mining?.mithrilPowder ?: 0L
            private set(value) {
                ProfileStorageData.profileSpecific?.mining?.mithrilPowder = value
            }

        var gemstonePowder: Long
            get() = ProfileStorageData.profileSpecific?.mining?.gemstonePowder ?: 0L
            private set(value) {
                ProfileStorageData.profileSpecific?.mining?.gemstonePowder = value
            }

        var tokens: Int
            get() = ProfileStorageData.profileSpecific?.mining?.tokens ?: 0
            private set(value) {
                ProfileStorageData.profileSpecific?.mining?.tokens = value
            }

        var availableMithrilPowder: Long
            get() = ProfileStorageData.profileSpecific?.mining?.availableMithrilPowder ?: 0L
            private set(value) {
                ProfileStorageData.profileSpecific?.mining?.availableMithrilPowder = value
            }

        var availableGemstonePowder: Long
            get() = ProfileStorageData.profileSpecific?.mining?.availableGemstonePowder ?: 0L
            private set(value) {
                ProfileStorageData.profileSpecific?.mining?.availableGemstonePowder = value
            }

        var availableTokens: Int
            get() = ProfileStorageData.profileSpecific?.mining?.availableTokens ?: 0
            private set(value) {
                ProfileStorageData.profileSpecific?.mining?.availableTokens = value
            }

        init {
            entries.forEach { it.guiNamePattern }
        }

        private fun resetTree() = entries.forEach {
            it.activeLevel = 0
            it.enabled = false
            it.isUnlocked = false
        }

        private fun Slot.parse() {
            val item = this.stack ?: return

            if (item.handlePowder()) return

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
                group("level").toInt()
            } ?: 0

            if (entry.activeLevel > entry.maxLevel) {
                throw IllegalStateException("Hotm Perk '${entry.name}' over max level")
            }

            if (entry == PEAK_OF_THE_MOUNTAIN) {
                entry.enabled = entry.activeLevel != 0
                return
            }
            entry.enabled = lore.any { enabledPattern.matches(it) }

        }

        private fun ItemStack.handlePowder(): Boolean {
            val isHeartItem =
                when {
                    heartItemPattern.matches(this.name) -> true
                    resetItemPattern.matches(this.name) -> false
                    else -> return false
                }

            if (isHeartItem) { // Reset on the heart Item to remove duplication
                mithrilPowder = 0
                gemstonePowder = 0
                tokens = 0
                availableGemstonePowder = 0
                availableMithrilPowder = 0
                availableTokens = 0
            }

            val lore = this.getLore()

            val mithrilPattern = if (isHeartItem) heartMithrilPattern else resetMithrilPattern
            val gemstonePattern = if (isHeartItem) heartGemstonePattern else resetGemstonePattern
            val tokenPattern = if (isHeartItem) heartTokensPattern else resetTokensPattern

            lore@ for (line in lore) {

                mithrilPattern.matchMatcher(line) {
                    val powder = group("powder").replace(",", "").toLong()
                    if (isHeartItem) {
                        availableMithrilPowder = powder
                    }
                    mithrilPowder += powder
                    continue@lore
                }

                gemstonePattern.matchMatcher(line) {
                    val powder = group("powder").replace(",", "").toLong()
                    if (isHeartItem) {
                        availableGemstonePowder = powder
                    }
                    gemstonePowder += powder
                    continue@lore
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

        @SubscribeEvent
        fun onInventoryClose(event: InventoryCloseEvent) {
            if (!inInventory) return
            inInventory = false
            entries.forEach { it.slot = null }
        }

        @SubscribeEvent
        fun onInventoryFullyOpen(event: InventoryFullyOpenedEvent) {
            if (!LorenzUtils.inSkyBlock) return
            inInventory = inventoryPattern.matches(event.inventoryName)
            DelayedRun.runNextTick {
                InventoryUtils.getItemsInOpenChest().forEach { it.parse() }
            }
            inventoryPattern.matcher(event.inventoryName)
        }

        @SubscribeEvent
        fun onChat(event: LorenzChatEvent) {
            if (!LorenzUtils.inSkyBlock) return
            if (!resetChatPattern.matches(event.message)) return
            resetTree()
        }

        // Feature

        @SubscribeEvent
        fun onRender(event: GuiContainerEvent.BackgroundDrawnEvent) {
            entries.forEach { entry ->
                val color = if (!entry.isUnlocked) LorenzColor.GRAY
                else if (entry.enabled) LorenzColor.GREEN else LorenzColor.RED
                entry.slot?.highlight(color)
            }
        }

        @SubscribeEvent
        fun onRenderTip(event: RenderItemTipEvent) {
            entries.firstOrNull() {
                event.stack == it.slot?.stack
            }?.let {
                event.stackTip = it.activeLevel.takeIf { it != 0 }?.toString() ?: ""
            }
        }

    }
}
