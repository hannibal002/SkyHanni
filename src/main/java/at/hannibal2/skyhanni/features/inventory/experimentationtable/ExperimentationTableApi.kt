package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.ExperimentsJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.events.experiments.TableRareUncoverEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskCompletedEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskStartedEvent
import at.hannibal2.skyhanni.events.experiments.TableXPBottleUsedEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
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
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.subtract
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
    private val currentExperimentData = ExperimentationDataSet()

    val patternGroup = RepoPattern.group("enchanting.experiments")

    // <editor-fold desc="Patterns">
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
        "^(?: |§. ?)(?:§.)*\\+(?:§.)+(?:\\[Lvl 1] (?:§r)?)?(?<reward>.*)\$",
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

    val experimentationTableInventory = InventoryDetector(inventoriesPattern)
    val inTable get() = experimentationTableInventory.isInside()
    val isActive get() = currentExperimentData.tier != null
    val currentExperimentTier get() = currentExperimentData.tier
    val currentExperimentType get() = currentExperimentData.type

    val inSuperpairs get() = inTable && isActive && currentExperimentType == ExperimentationTaskType.SUPERPAIRS

    private var lastExpOverHash: Int = 0
    private var currentExpOverHash: Int = 0
    private var queuedCompleteEvent: TableTaskCompletedEvent? = null
    private var handleBottlesOnInvClose: Boolean = false
    private var currentBottlesInInventory: Map<NeuInternalName, Int> = mapOf()
    var miscRepoRewards: List<NeuInternalName> = emptyList()
        private set

    enum class ExperimentationMessages(private val displayName: String) {
        DONE("§eYou claimed the §dSuperpairs §erewards! §8(§7Claim§8)"),
        EXPERIENCE("§8 +§3141k Experience §8(§7Experience Drops§8)"),
        ENCHANTMENTS("§8 +§9Smite VII §8(§7Enchantment Drops§8)"),
        BOTTLES("§8 +§9Titanic Experience Bottle §8(§7Bottle Drops§8)"),
        MISC("§8 +§5Metaphysical Serum §8(§7Misc Drops§8)"),
        ;

        override fun toString() = displayName
    }

    enum class ExperimentationTaskType {
        CHRONOMATRON,
        ULTRASEQUENCER,
        SUPERPAIRS,
        ;

        companion object {
            fun fromStringOrNull(string: String) = entries.firstOrNull {
                it.name.equals(string, ignoreCase = true)
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
            // todo at some point make resettable storage set deal with this stuff
            //  ResettableStorageSet doesn't deal with nulls or clearing mutables
            //  It does (^ that) after #4244 gets merged, so I'll do that eventually -David
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

    fun inDistanceToTable(max: Double): Boolean {
        val vec = LorenzVec.getBlockBelowPlayer()
        return storage?.tablePos?.let { it.distance(vec) <= max } ?: false
    }

    private fun ExperimentationMessages.isSelected() = config.hideMessages.contains(this)

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        miscRepoRewards = event.getConstant<ExperimentsJson>("ExperimentationTable").miscRewards
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryClose() {
        if (currentExpOverHash != 0) {
            lastExpOverHash = currentExpOverHash
            currentExpOverHash = 0
        }
        if (handleBottlesOnInvClose) DelayedRun.runDelayed(100.milliseconds) {
            handleXpBottlesGained()
            handleBottlesOnInvClose = false
        } else refreshBottlesInInventory()
        DelayedRun.runDelayed(150.milliseconds) {
            // Catch early closes triggering the event before the inventory is fully opened
            if (expOverInventoryPattern.matches(InventoryUtils.openInventoryName())) return@runDelayed
            val queuedEvent = queuedCompleteEvent ?: return@runDelayed
            queuedEvent.post()
            queuedCompleteEvent = null
            currentExperimentData.reset()
        }
    }

    @HandleEvent(
        onlyOnIsland = IslandType.PRIVATE_ISLAND,
        eventTypes = [WorldChangeEvent::class, ItemAddInInventoryEvent::class],
    )
    fun refreshBottlesInInventory() {
        currentBottlesInInventory = getBottlesInOwnInventory().takeIf {
            it != currentBottlesInInventory
        } ?: return
        ChatUtils.debug("Updated bottles in inventory: $currentBottlesInInventory")
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

    private fun SkyHanniChatEvent.tryBlockChat(reward: String) {
        val rewardInternalName = NeuInternalName.fromItemNameOrNull(reward)
        blockedReason = when {
            enchantingExpPattern.matches(reward) && ExperimentationMessages.EXPERIENCE.isSelected() -> "EXPERIENCE_DROP"
            experienceBottleChatPattern.matches(reward) && ExperimentationMessages.BOTTLES.isSelected() -> "BOTTLE_DROP"
            rewardInternalName in miscRepoRewards && ExperimentationMessages.MISC.isSelected() -> "MISC_DROP"
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

    private fun handleXpBottlesUsed() {
        val applicableDeltas = getXpBottleDelta().filter { it.value < 0 }.takeIfNotEmpty() ?: return
        if (!inDistanceToTable(15.0)) return
        applicableDeltas.forEach { (bottleInternalName, delta) ->
            val absDelta = abs(delta)
            TableXPBottleUsedEvent(bottleInternalName, abs(absDelta)).post()
        }
    }

    private fun handleXpBottlesGained() {
        val applicableDeltas = getXpBottleDelta().filter { it.value > 0 }.takeIfNotEmpty() ?: return
        applicableDeltas.forEach { (bottleInternalName, delta) ->
            currentExperimentData.addReward(bottleInternalName, delta)
        }
    }

    private fun getXpBottleDelta(): Map<NeuInternalName, Int> {
        val lastBottlesInInventory = currentBottlesInInventory
        currentBottlesInInventory = getBottlesInOwnInventory()
        return currentBottlesInInventory.subtract(lastBottlesInInventory) {
            it.toInt()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onClick(event: WorldClickEvent) {
        if (!inDistanceToTable(15.0)) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        event.itemInHand?.getInternalNameOrNull()?.takeIf {
            experienceBottlePattern.matches(it.asString())
        } ?: return

        DelayedRun.runDelayed(200.milliseconds) {
            handleXpBottlesUsed()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inTable) return

        updateTablePos()
        event.tryFireRareBookUncovered()
        event.tryUpdateCurrentActivity()
        refreshBottlesInInventory()
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
        val slotIndex = when (currentExperimentData.type) {
            null -> ErrorManager.skyHanniError(
                "Found Experiment Over GUI without a set task type!",
                "inventoryName" to inventoryName,
                "inventoryItems" to inventoryItems,
                "currentData" to currentExperimentData,
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

        currentExperimentData.type = ExperimentationTaskType.fromStringOrNull(item.displayName.removeColor()) ?: return
        currentExperimentData.tier = expOverStakesLorePattern.firstMatcher(lore) {
            ExperimentationTier.byNameOrNull(group("stakes"))
        } ?: return

        val rewardsBeginIndex = lore.indexOfFirst { expOverRewardsStartLorePattern.matches(it) } + 1
        val rewardsEndIndex = lore.indexOfFirst { expOverRewardsEndLorePattern.matches(it) } - 1

        lore.subList(rewardsBeginIndex, rewardsEndIndex)
            .mapNotNull { expOverRewardsLorePattern.matchGroup(it, "reward") }
            .takeIfNotEmpty()?.toList().orEmpty()
            .forEach { it.processRewardOrNull() }

        queuedCompleteEvent = currentExperimentData.toCompletedTaskEventOrNull()
    }

    private fun String.processRewardOrNull() {
        if (this.endsWith("Superpairs clicks")) return

        guardianPetNamePattern.matchMatcher(this) {
            val rarity = LorenzRarity.getByColorCode(group("color")[0]) ?: return@matchMatcher
            val internalName = "GUARDIAN;${rarity.id}".toInternalName()
            currentExperimentData.addReward(internalName, 1)
            return
        }
        enchantingExpPattern.matchMatcher(this) {
            val amount = group("amount").formatLong().takeIf { it > 0 } ?: return@matchMatcher
            currentExperimentData.enchantingXpGained += amount
            return
        }
        if (experienceBottleChatPattern.matches(this)) {
            handleBottlesOnInvClose = true
            return
        }

        val internalName = NeuInternalName.fromItemNameOrNull(this)
            ?: return ChatUtils.debug("Could not read item name from $this")
        currentExperimentData.addReward(internalName, 1)
    }

    private fun InventoryOpenEvent.tryFireRareBookUncovered() {
        if (currentExperimentData.rareFoundFired) return
        for (lore in inventoryItems.map { it.value.getLore() }) {
            val firstLine = lore.firstOrNull() ?: continue
            if (!ultraRarePattern.matches(firstLine)) continue
            val bookNameLine = lore.getOrNull(2) ?: continue
            bookPattern.matchMatcher(bookNameLine) {
                TableRareUncoverEvent(group("enchant")).post()
                currentExperimentData.rareFoundFired = true
                return
            }
        }
    }

    private fun InventoryOpenEvent.tryUpdateCurrentActivity() = currentTypeAndTierPattern.matchMatcher(inventoryName) {
        if (inventoryName == "Experimentation Table") return@matchMatcher currentExperimentData.reset()

        val type = ExperimentationTaskType.fromStringOrNull(group("type")) ?: return@matchMatcher
        val tier = ExperimentationTier.byNameOrNull(group("tier")) ?: return@matchMatcher
        if (type == currentExperimentType && tier == currentExperimentTier) return@matchMatcher

        currentExperimentData.apply {
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

    fun hasGuardianPet(): Boolean = CurrentPetApi.isCurrentPet("Guardian")
}
