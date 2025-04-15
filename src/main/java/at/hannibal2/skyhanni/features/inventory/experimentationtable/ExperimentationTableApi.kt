package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PetApi
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.experiments.TableRareUncoverEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskCompletedEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskStartedEvent
import at.hannibal2.skyhanni.events.experiments.TableXPBottleUsedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object ExperimentationTableApi {

    private const val ADDONS_OVER_DATA_SLOT = 11
    private const val SUPERPAIRS_OVER_DATA_SLOT = 13

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.experimentsProfitTracker
    private val storage get() = ProfileStorageData.profileSpecific?.experimentation
    private val EXPERIMENTATION_TABLE_SKULL by lazy { SkullTextureHolder.getTexture("EXPERIMENTATION_TABLE") }
    private val currentData = ExperimentationDataSet()

    val patternGroup = RepoPattern.group("enchanting.experiments")
    val experimentationTableInventory = InventoryDetector { name -> inventoriesPattern.matches(name) }
    val inTable get() = experimentationTableInventory.isInside()
    val isActive get() = currentData.tier != null
    val currentExperimentTier get() = currentData.tier
    val currentExperimentType get() = currentData.type

    private var lastExpOverHash: Int = 0
    private var currentExpOverHash: Int = 0
    private var queuedCompleteEvent: TableTaskCompletedEvent? = null
    private var handleBottlesOnInvClose: Boolean = false
    private var lastBottlesInInventory: Map<NeuInternalName, Int> = getBottlesInOwnInventory()
    private var currentBottlesInInventory: Map<NeuInternalName, Int> = mapOf()

    enum class ExperimentationMessages(private val displayName: String) {
        DONE("§eYou claimed the §dSuperpairs §erewards! §8(§7Claim§8)"),
        EXPERIENCE("§8 +§3141k Experience §8(§7Experience Drops§8)"),
        ENCHANTMENTS("§8 +§9Smite VII §8(§7Enchantment Drops§8)"),
        BOTTLES("§8 +§9Titanic Experience Bottle §8(§7Bottle Drops§8)"),
        MISC("§8 +§5Metaphysical Serum §8(§7Misc Drops§8)");

        override fun toString() = displayName
    }

    enum class ExperimentationTaskType(private val displayName: String) {
        CHRONOMATRON("Chronomatron"),
        ULTRASEQUENCER("Ultrasequencer"),
        SUPERPAIRS("Superpairs"),
        ;

        override fun toString() = displayName

        companion object {
            fun fromStringOrNull(string: String) = entries.firstOrNull {
                it.displayName.equals(string, ignoreCase = true) || it.name.equals(string, ignoreCase = true)
            }
        }
    }

    enum class ExperimentationTier(
        private val displayName: String,
        overInclusiveSlotRange: IntRange, // Filtered 'later' to remove side spaces
        private val sideSpace: Int = 1,
    ) {
        NONE("", 0..0, sideSpace = 0),
        BEGINNER("Beginner", 18..35),
        HIGH("High", 10..43, sideSpace = 2),
        GRAND("Grand", 10..43, sideSpace = 2),
        SUPREME("Supreme", 9..44),
        TRANSCENDENT("Transcendent", 9..44),
        METAPHYSICAL("Metaphysical", 9..44),
        ;

        val slotRange = overInclusiveSlotRange.filter {
            (it % 9) !in when (sideSpace) {
                1 -> listOf(0, 8)
                2 -> listOf(0, 1, 7, 8)
                else -> emptyList()
            }
        }

        val gridSize: Int = slotRange.size

        override fun toString() = displayName

        companion object {
            fun byNameOrNull(name: String): ExperimentationTier? = entries.firstOrNull {
                it.displayName.equals(name, ignoreCase = true)
            }
        }
    }

    data class ExperimentationDataSet(
        @Transient var type: ExperimentationTaskType? = null,
        @Transient var tier: ExperimentationTier? = null,
        var enchantingXpGained: Long = 0L,
        var rareFoundFired: Boolean = false,
    ) : ResettableStorageSet() {
        @Transient private val otherRewards: MutableMap<NeuInternalName, Int> = mutableMapOf()

        override fun reset() {
            super.reset()
            otherRewards.clear()
            type = null
            tier = null
        }

        fun addReward(internalName: NeuInternalName, amount: Int = 1) {
            otherRewards.addOrPut(internalName, amount)
        }

        fun toCompletedTaskEventOrNull(): TableTaskCompletedEvent? = when {
            type == null || tier == null -> null
            else -> TableTaskCompletedEvent(
                type = type ?: error("impossible"),
                tier = tier ?: error("impossible"),
                enchantingXpGained = enchantingXpGained,
                loot = otherRewards,
            )
        }
    }

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: Click any button!
     * REGEX-TEST: Next button is instantly rewarded!
     * REGEX-TEST: Click a second button!
     */
    val waitingMessagesPattern by patternGroup.pattern(
        "waiting.messages",
        "Click any button!|Click a second button!|Next button is instantly rewarded!",
    )

    /**
     * REGEX-TEST: Gained +3 Clicks
     */
    val powerUpPattern by patternGroup.pattern(
        "powerups",
        "Gained \\+\\d Clicks?|Instant Find|\\+\\S* XP",
    )

    /**
     * REGEX-TEST: 123k Enchanting Exp
     * REGEX-TEST: Titanic Experience Bottle
     */
    val rewardPattern by patternGroup.pattern(
        "rewards",
        "\\d{1,3}k Enchanting Exp|Enchanted Book|(?:Titanic |Grand |\\b)Experience Bottle|Metaphysical Serum|Experiment the Fish",
    )

    /**
     * REGEX-TEST: Superpairs (Metaphysical)
     * REGEX-TEST: Chronomatron (Metaphysical)
     * REGEX-TEST: Ultrasequencer (Metaphysical)
     * REGEX-TEST: Experimentation Table
     * REGEX-TEST: Experiment Over
     * REGEX-TEST: Superpairs Rewards
     */
    private val inventoriesPattern by patternGroup.pattern(
        "inventories",
        "(?:Superpairs|Chronomatron|Ultrasequencer) ?(?:\\(.+\\)|➜ Stakes|Rewards)|Experiment(?:ation Tabl| [Oo]v)er?",
    )

    /**
     * REGEX-TEST:  §r§8+§r§5Metaphysical Serum
     * REGEX-TEST:  §r§8+§r§3149k Enchanting Exp
     * REGEX-TEST: §8 +§r§3400k Enchanting Exp
     * REGEX-TEST:  §r§8+§r§327k Enchanting Exp
     * REGEX-TEST: §r§8+§r§7[Lvl 1] §r§5Guardian
     */
    private val experimentsDropPattern by patternGroup.pattern(
        "drop",
        "^(?: |§. ?)(?:§.)*\\+(?:§.|\\[Lvl 1] (?:§r)?)*(?<reward>.*)\$",
    )

    /**
     * REGEX-TEST: You claimed the Superpairs rewards!
     */
    private val claimMessagePattern by patternGroup.pattern(
        "claim",
        "You claimed the \\S+ rewards!",
    )

    /**
     * REGEX-TEST: 131k Enchanting Exp
     * REGEX-TEST: 42,000 Enchanting Exp
     * REGEX-TEST: 300,000 Enchanting Exp
     * REGEX-TEST: 151,000 Enchanting Exp
     */
    private val enchantingExpPattern by patternGroup.pattern(
        "exp",
        "(?:§.)*(?<amount>(?:\\d+|\\d+,\\d+)[MBk]?) Enchanting Exp",
    )

    /**
     * REGEX-TEST: Titanic Experience Bottle
     * REGEX-TEST: Grand Experience Bottle
     */
    private val experienceBottleChatPattern by patternGroup.pattern(
        "chat.xpbottle",
        "(?:Colossal |Titanic |Grand |\\b)Experience Bottle",
    )

    /**
     * REGEX-TEST: TITANIC_EXP_BOTTLE
     */
    val experienceBottlePattern by patternGroup.pattern(
        "xpbottle",
        "(?:COLOSSAL_|TITANIC_|GRAND_|\\b)EXP_BOTTLE",
    )

    /**
     * REGEX-TEST: ☕ You bought a bonus charge for the Experimentation Table! (1/3)
     */
    val experimentRenewPattern by patternGroup.pattern(
        "renew",
        "☕ You bought a bonus charge for the Experimentation Table! \\((?<current>\\d)/3\\)",
    )

    /**
     * REGEX-TEST: §d§kXX§5 ULTRA-RARE BOOK! §d§kXX
     */
    private val ultraRarePattern by patternGroup.pattern(
        "ultrarare",
        "§d§kXX§5 ULTRA-RARE BOOK! §d§kXX",
    )

    /**
     * REGEX-TEST: §9Smite VII
     */
    private val bookPattern by patternGroup.pattern(
        "book",
        "§9(?<enchant>.*)",
    )

    /**
     * REGEX-TEST: §dGuardian
     * REGEX-TEST: §9Guardian§e
     * REGEX-TEST: §5Guardian
     */
    private val guardianPetNamePattern by patternGroup.pattern(
        "guardianpet",
        "§(?<color>[956d])Guardian.*",
    )

    /**
     * REGEX-TEST: §7Stakes: §dMetaphysical
     */
    private val expOverStakesLorePattern by patternGroup.pattern(
        "inventory.experiment-over.stakes",
        "§7Stakes: (?:§.)+(?<stakes>.*)",
    )

    private val expOverRewardsStartLorePattern by patternGroup.pattern(
        "inventory.experiment-over.rewards-start",
        "§7Rewards:",
    )

    private val expOverRewardsEndLorePattern by patternGroup.pattern(
        "inventory.experiment-over.rewards-end",
        "§eClick to claim rewards!",
    )

    /**
     * REGEX-TEST: §8 +§8 §7[Lvl 1] §5Guardian
     * REGEX-TEST: §8 +§3300,000 Enchanting Exp (Stakes)
     * REGEX-TEST: §8 +§3151,000 Enchanting Exp (Pairs)
     * REGEX-TEST: §8 +§8 §9Growth VI
     * REGEX-TEST: §8 +§3300,000 Enchanting Exp (Stakes)
     * REGEX-TEST: §8 +§3172,000 Enchanting Exp (Pairs)
     * REGEX-TEST: §8 +§3300,000 Enchanting Exp (Stakes)
     * REGEX-TEST: §8 +§8 §9Titanic Experience Bottle
     * REGEX-TEST: §8 +§8 §7[Lvl 1] §6Guardian
     * REGEX-TEST: §8 +§8 §aGrand Experience Bottle
     * REGEX-TEST: §8 +§8 §9Blast Protection VI
     */
    private val expOverRewardsLorePattern by patternGroup.pattern(
        "inventory.experiment-over.rewards",
        "§8 \\+(?:§.| )*(?:\\[Lvl \\d+] )?(?<reward>.*?)(?=\\s\\((?:Stakes|Pairs)\\)|\$)(?:\\s\\((?:Stakes|Pairs)\\))?",
    )

    /**
     * REGEX-TEST: Superpairs (Metaphysical)
     * REGEX-TEST: Chronomatron (Metaphysical)
     * REGEX-TEST: Ultrasequencer (Metaphysical)
     */
    private val currentTypeAndTierPattern by patternGroup.pattern(
        "inventory.experiment.current-type-and-tier",
        "(?<type>Superpairs|Chronomatron|Ultrasequencer) \\((?<tier>.*)\\)",
    )

    /**
     * REGEX-TEST: Superpairs Rewards
     * REGEX-TEST: Experiment Over
     */
    private val expOverInventoryPattern by patternGroup.pattern(
        "inventory.experiment-over",
        "Experiment [Oo]ver|Superpairs Rewards",
    )
    // </editor-fold>

    fun inDistanceToTable(max: Double): Boolean {
        val vec = LorenzVec.getBlockBelowPlayer()
        return storage?.tablePos?.let { it.distance(vec) <= max } ?: false
    }

    private fun ExperimentationMessages.isSelected() = config.hideMessages.contains(this)

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryClose() {
        if (currentExpOverHash != 0) {
            lastExpOverHash = currentExpOverHash
            currentExpOverHash = 0
        }
        if (handleBottlesOnInvClose) DelayedRun.runDelayed(100.milliseconds) {
            handleExpBottles(true)
            handleBottlesOnInvClose = false
        }
        DelayedRun.runDelayed(150.milliseconds) {
            // Catch early closes triggering the event before the inventory is fully opened
            if (expOverInventoryPattern.matches(InventoryUtils.openInventoryName())) return@runDelayed
            val queuedEvent = queuedCompleteEvent ?: return@runDelayed
            queuedEvent.post()
            queuedCompleteEvent = null
            currentData.reset()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        lastBottlesInInventory = getBottlesInOwnInventory()
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onChat(event: SkyHanniChatEvent) {
        if (claimMessagePattern.matches(event.message) && ExperimentationMessages.DONE.isSelected()) {
            event.blockedReason = "CLAIM_MESSAGE"
            return
        }

        experimentsDropPattern.matchMatcher(event.message) {
            event.tryBlockChat(group("reward"))
        }
    }

    private fun String.isMiscReward() =
        this == "Metaphysical Serum" || this == "Experiment The Fish" || this.endsWith("Guardian")

    private fun SkyHanniChatEvent.tryBlockChat(reward: String) {
        blockedReason = when {
            enchantingExpPattern.matches(reward) && ExperimentationMessages.EXPERIENCE.isSelected() -> "EXPERIENCE_DROP"
            experienceBottleChatPattern.matches(reward) && ExperimentationMessages.BOTTLES.isSelected() -> "BOTTLE_DROP"
            reward.isMiscReward() && ExperimentationMessages.MISC.isSelected() -> "MISC_DROP"
            ExperimentationMessages.ENCHANTMENTS.isSelected() -> "ENCHANT_DROP"
            else -> ""
        }
    }

    private fun getBottlesInOwnInventory(): Map<NeuInternalName, Int> = buildMap {
        InventoryUtils.getItemsInOwnInventory().forEach { itemStack ->
            val internalName = itemStack.getInternalNameOrNull()?.takeIf { internalName ->
                experienceBottlePattern.matches(internalName.asString())
            } ?: return@forEach
            addOrPut(internalName, itemStack.stackSize)
        }
    }

    private fun handleExpBottles(addToLoot: Boolean = false, fireUsageEvent: Boolean = false) {
        lastBottlesInInventory = currentBottlesInInventory
        currentBottlesInInventory = getBottlesInOwnInventory()
        for ((internalName, currentInInv) in currentBottlesInInventory) {
            val lastInInv = lastBottlesInInventory.getOrDefault(internalName, 0).takeIf {
                it != currentInInv
            } ?: continue

            val absChange = currentInInv - lastInInv
            if (absChange < 0 && inDistanceToTable(10.0) && fireUsageEvent) {
                TableXPBottleUsedEvent(internalName, abs(absChange)).post()
            } else if (addToLoot) currentData.addReward(internalName, absChange)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onClick(event: WorldClickEvent) {
        if (!inDistanceToTable(10.0)) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        event.itemInHand?.getInternalNameOrNull()?.takeIf {
            experienceBottlePattern.matches(it.asString())
        } ?: return

        DelayedRun.runDelayed(200.milliseconds) {
            handleExpBottles(false, fireUsageEvent = true)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inTable) return

        updateTablePos()
        event.tryFireRareBookUncovered()
        event.tryUpdateCurrentActivity()
        handleExpBottles(false)
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inTable || event.item?.displayName != "§cDecline") return
        queuedCompleteEvent = null
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!inTable) return
        event.tryProcessExperimentOver()
    }

    private fun InventoryOpenEvent.tryProcessExperimentOver() {
        if (!expOverInventoryPattern.matches(inventoryName) || currentExpOverHash != 0) return
        tryUpdateCurrentActivity()
        val slotIndex = when (currentData.type) {
            null -> ErrorManager.skyHanniError(
                "Found Experiment Over GUI without a set task type!",
                "inventoryName" to inventoryName,
                "inventoryItems" to inventoryItems,
                "currentData" to currentData,
                "currentType" to currentExperimentType,
                "currentTier" to currentExperimentTier,
            )
            ExperimentationTaskType.SUPERPAIRS -> SUPERPAIRS_OVER_DATA_SLOT
            else -> ADDONS_OVER_DATA_SLOT
        }
        val item = inventoryItems[slotIndex]
        val lore = item?.getLore()?.takeIfNotEmpty()?.toList() ?: return

        currentExpOverHash = lore.hashCode().takeIf {
            it != lastExpOverHash && it != currentExpOverHash && it != 0
        } ?: return

        currentData.type = ExperimentationTaskType.fromStringOrNull(item.displayName.removeColor()) ?: return
        currentData.tier = expOverStakesLorePattern.firstMatcher(lore) {
            ExperimentationTier.byNameOrNull(group("stakes"))
        } ?: return

        val rewardsBeginIndex = lore.indexOfFirst { expOverRewardsStartLorePattern.matches(it) } + 1
        val rewardsEndIndex = lore.indexOfFirst { expOverRewardsEndLorePattern.matches(it) } - 1

        lore.subList(rewardsBeginIndex, rewardsEndIndex)
            .mapNotNull { expOverRewardsLorePattern.matchGroup(it, "reward") }
            .takeIfNotEmpty()?.toList().orEmpty()
            .forEach { it.processRewardOrNull() }

        queuedCompleteEvent = currentData.toCompletedTaskEventOrNull()
    }

    private fun String.processRewardOrNull() {
        if (this.endsWith("Superpairs clicks")) return

        guardianPetNamePattern.matchMatcher(this) {
            val color = group("color")[0].toLorenzColor() ?: return@matchMatcher
            val rarity = LorenzRarity.getByColor(color) ?: return@matchMatcher
            val internalName = "GUARDIAN;${rarity.id}".toInternalName()
            currentData.addReward(internalName, 1)
            return
        }
        enchantingExpPattern.matchMatcher(this) {
            val amount = group("amount").formatLong().takeIf { it > 0 } ?: return@matchMatcher
            currentData.enchantingXpGained += amount
            return
        }
        if (experienceBottleChatPattern.matches(this)) {
            handleBottlesOnInvClose = true
            return
        }

        val internalName = NeuInternalName.fromItemNameOrNull(this) ?: run {
            ChatUtils.debug("Could not read item name from $this")
            return
        }
        currentData.addReward(internalName, 1)
    }

    private fun InventoryOpenEvent.tryFireRareBookUncovered() {
        if (currentData.rareFoundFired) return
        for (lore in inventoryItems.map { it.value.getLore() }) {
            val firstLine = lore.firstOrNull() ?: continue
            if (!ultraRarePattern.matches(firstLine)) continue
            val bookNameLine = lore.getOrNull(2) ?: continue
            bookPattern.matchMatcher(bookNameLine) {
                TableRareUncoverEvent(group("enchant")).post()
                currentData.rareFoundFired = true
                return
            }
        }
    }

    private fun InventoryOpenEvent.tryUpdateCurrentActivity() = currentTypeAndTierPattern.matchMatcher(inventoryName) {
        val type = ExperimentationTaskType.fromStringOrNull(group("type")) ?: return@matchMatcher
        val tier = ExperimentationTier.byNameOrNull(group("tier")) ?: return@matchMatcher
        if (type == currentExperimentType && tier == currentExperimentTier) return@matchMatcher

        currentData.apply {
            this.type = type
            this.tier = tier
        }

        TableTaskStartedEvent(type, tier).post()
    }

    private fun updateTablePos() {
        storage?.tablePos = EntityUtils.getEntities<EntityArmorStand>().find {
            it.wearingSkullTexture(EXPERIMENTATION_TABLE_SKULL)
        }?.getLorenzVec().takeIf { it != storage?.tablePos } ?: return
    }

    fun hasGuardianPet(): Boolean = guardianPetNamePattern.matches(PetApi.currentPet)
}
