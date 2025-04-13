package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PetApi
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.experiments.TableRareUncoverEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskCompletedEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskStartedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object ExperimentationTableApi {

    private const val ADDONS_OVER_DATA_SLOT = 11
    private const val SUPERPAIRS_OVER_DATA_SLOT = 13

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.experimentsProfitTracker
    private val storage get() = ProfileStorageData.profileSpecific?.experimentation
    private val EXPERIMENTATION_TABLE_SKULL by lazy { SkullTextureHolder.getTexture("EXPERIMENTATION_TABLE") }
    private val lastBottlesInInventory = mutableMapOf<NeuInternalName, Int>()
    private val currentBottlesInInventory = mutableMapOf<NeuInternalName, Int>()
    private val currentData = ExperimentationDataSet()

    val patternGroup = RepoPattern.group("enchanting.experiments")
    val experimentationTableInventory = InventoryDetector { name -> inventoriesPattern.matches(name) }
    val inTable get() = experimentationTableInventory.isInside()
    val isActive get() = currentData.tier != null
    val currentExperimentTier get() = currentData.tier
    val currentExperimentType get() = currentData.type

    private var lastProcessedExperimentOverHash: Int = 0
    private var queuedAddonCompletion: TableTaskCompletedEvent? = null

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
        var type: ExperimentationTaskType? = null,
        var tier: ExperimentationTier? = null,
        var enchantingXpGained: Long? = null,
        var rareFoundFired: Boolean = false,
    ) : ResettableStorageSet() {
        @Transient private var lastUpdatedAt: SimpleTimeMark? = null
        @Transient private val otherRewards: MutableMap<NeuInternalName, Int> = mutableMapOf()

        fun addReward(internalName: NeuInternalName, amount: Int = 1) {
            otherRewards.addOrPut(internalName, amount)
            val timeInCall = SimpleTimeMark.now()
            lastUpdatedAt = timeInCall
            DelayedRun.runDelayed(500.milliseconds) {
                if (lastUpdatedAt != timeInCall) return@runDelayed
                createTaskCompleteEventOrNull()?.post()
            }
        }

        fun getRewards(): Map<NeuInternalName, Int> = otherRewards
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
     */
    private val experimentsDropPattern by patternGroup.pattern(
        "drop",
        "^(?: |§. ?)(?:§.)*\\+(?:§.)*(?<reward>.*)\$",
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
     */
    private val enchantingExpPattern by patternGroup.pattern(
        "exp",
        "(?:§.)*(?<amount>(?:\\d+|\\d+,\\d+)[MBk]?) Enchanting Exp",
    )

    /**
     * REGEX-TEST: Titanic Experience Bottle
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
     */
    private val guardianPetNamePattern by patternGroup.pattern(
        "guardianpet",
        "§[956d]Guardian.*",
    )

    /**
     * REGEX-TEST: §7Stakes: §dMetaphysical
     */
    private val experimentOverStakesLorePattern by patternGroup.pattern(
        "inventory.experiment-over.stakes",
        "§7Stakes: (?:§.)+(?<stakes>.*)",
    )

    private val experimentOverRewardsStartLorePattern by patternGroup.pattern(
        "inventory.experiment-over.rewards-start",
        "§7Rewards:",
    )

    private val experimentOverRewardsEndLorePattern by patternGroup.pattern(
        "inventory.experiment-over.rewards-end",
        "§eClick to claim rewards!",
    )

    /**
     * REGEX-TEST: §8 +§3400,000 Enchanting Exp (Stakes)
     * REGEX-TEST: §8 +§3352,000 Enchanting Exp (Pairs)
     * REGEX-TEST: §8 +§354,000 Enchanting Exp
     * REGEX-TEST: §8 +§342,000 Enchanting Exp
     */
    private val experimentOverRewardsEnchantingXpPattern by patternGroup.pattern(
        "inventory.experiment-over.rewards.enchanting-xp",
        "§8 \\+(?:§.)+(?<amount>[\\d,]+) Enchanting Exp(?: \\(.*\\))?",
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
    private val experimentOverInventoryPattern by patternGroup.pattern(
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
        lastProcessedExperimentOverHash = 0
        if (queuedAddonCompletion == null) return
        DelayedRun.runDelayed(50.milliseconds) {
            queuedAddonCompletion?.post()
            queuedAddonCompletion = null
        }
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
        blockedReason = when {
            enchantingExpPattern.matches(reward) && ExperimentationMessages.EXPERIENCE.isSelected() -> "EXPERIENCE_DROP"
            experienceBottleChatPattern.matches(reward) && ExperimentationMessages.BOTTLES.isSelected() -> "BOTTLE_DROP"
            listOf("Metaphysical Serum", "Experiment The Fish").contains(reward) && ExperimentationMessages.MISC.isSelected() -> "MISC_DROP"
            ExperimentationMessages.ENCHANTMENTS.isSelected() -> "ENCHANT_DROP"
            else -> ""
        }

        enchantingExpPattern.matchMatcher(reward) {
            try {
                val amount = group("amount").formatLong().takeIf { it > 0 } ?: return@matchMatcher
                currentData.enchantingXpGained = currentData.enchantingXpGained?.plus(amount)
            } catch (e: NumberFormatException) {
                ErrorManager.skyHanniError("Could not read enchanting exp from $reward")
            }
        }

        val internalName = NeuInternalName.fromItemNameOrNull(reward) ?: return
        if (!experienceBottleChatPattern.matches(reward)) {
            currentData.addReward(internalName, 1)
        } else DelayedRun.runDelayed(100.milliseconds) { handleExpBottles(true) }
    }

    private fun handleExpBottles(addToLoot: Boolean) {
        for (item in InventoryUtils.getItemsInOwnInventory()) {
            val internalName = item.getInternalNameOrNull() ?: continue
            if (internalName.asString() !in listOf("EXP_BOTTLE", "GRAND_EXP_BOTTLE", "TITANIC_EXP_BOTTLE")) continue
            currentBottlesInInventory.addOrPut(internalName, item.stackSize)
        }

        for ((internalName, amount) in currentBottlesInInventory) {
            val lastInInv = lastBottlesInInventory.getOrDefault(internalName, 0)
            if (lastInInv >= amount) {
                lastBottlesInInventory[internalName] = amount
                continue
            }

            if (lastInInv == 0) {
                lastBottlesInInventory[internalName] = amount
                if (addToLoot) currentData.addReward(internalName, amount)
                continue
            }

            lastBottlesInInventory[internalName] = amount
            if (addToLoot) currentData.addReward(internalName, amount - lastInInv)
        }
        currentBottlesInInventory.clear()
    }

    @HandleEvent
    fun onTableTaskCompleted(event: TableTaskCompletedEvent) {
        currentData.reset()
    }

    private fun createTaskCompleteEventOrNull(): TableTaskCompletedEvent? = when {
        currentData.tier == null || currentData.type == null -> null
        else -> TableTaskCompletedEvent(
            type = currentData.type ?: error("impossible"),
            tier = currentData.tier ?: error("impossible"),
            enchantingXpGained = currentData.enchantingXpGained ?: 0L,
            loot = currentData.getRewards(),
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inTable) return

        updateTablePos()
        event.tryFireRareBookUncovered()
        event.tryUpdateCurrentActivity()
        handleExpBottles(false)
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inTable || event.item?.displayName != "§cDecline") return
        queuedAddonCompletion = null
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!inTable) return
        event.tryReadExperimentOver()
    }

    private fun InventoryOpenEvent.tryReadExperimentOver() =
        experimentOverInventoryPattern.matchMatcher(this.inventoryName) {
            lastProcessedExperimentOverHash = inventoryItems.hashCode().takeIf {
                it != lastProcessedExperimentOverHash
            } ?: return@matchMatcher
            processExperimentOver()
        }

    private fun InventoryOpenEvent.processExperimentOver() {
        val item = when (currentExperimentType) {
            ExperimentationTaskType.SUPERPAIRS -> inventoryItems[SUPERPAIRS_OVER_DATA_SLOT]
            else -> inventoryItems[ADDONS_OVER_DATA_SLOT]
        } ?: return
        val lore = item.getLore().takeIfNotEmpty()?.toList() ?: return

        val taskType = ExperimentationTaskType.fromStringOrNull(item.displayName.removeColor()) ?: return
        val taskTier = experimentOverStakesLorePattern.firstMatcher(lore) {
            ExperimentationTier.byNameOrNull(group("stakes"))
        } ?: return

        val rewardsBeginIndex = lore.indexOfFirst { experimentOverRewardsStartLorePattern.matches(it) } + 1
        val rewardsEndIndex = lore.indexOfFirst { experimentOverRewardsEndLorePattern.matches(it) } - 2
        val rewards: List<String> = lore
            .subList(rewardsBeginIndex, rewardsEndIndex)
            .takeIfNotEmpty()?.toList() ?: return

        val enchantingXp = rewards.sumOf { reward ->
            experimentOverRewardsEnchantingXpPattern.matchMatcher(reward) {
                group("amount").formatIntOrNull()
            } ?: 0
        }.toLong()

        currentData.apply {
            type = taskType
            tier = taskTier
        }

        // TODO I'm leaving it like this because I'm not 100% sure if the
        //  list of rewards can get too long for the item lore at any point
        //  If it doesn't, we can read it here and get rid of chat reading
        // Superpairs rewards are added in onChat
        if (taskType == ExperimentationTaskType.SUPERPAIRS) return

        currentData.enchantingXpGained = enchantingXp
        queuedAddonCompletion = createTaskCompleteEventOrNull()
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
