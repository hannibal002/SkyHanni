package at.hannibal2.skyhanni.features.inventory.experimentationtable.superpairs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.TaskType
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EnumUtils.isAnyOf
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.DyeCompat
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.milliseconds

// TODO: Split reading into ExperimentationSuperpairApi, leaving display to just use the data
@SkyHanniModule
object SuperpairDataDisplay {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable

    private data class SuperpairItem(val slotId: Int, val reward: String, val damage: Int)
    private data class FoundData(
        val item: SuperpairItem? = null,
        val first: SuperpairItem? = null,
        val second: SuperpairItem? = null
    )

    private enum class FoundType {
        NORMAL,
        POWERUP,
        MATCH,
        PAIR
    }

    // <editor-fold desc="Patterns">
    private val instantFindNamePattern by ExperimentationTableApi.patternGroup.pattern(
        "powerups.instantfind.name",
        "Instant Find",
    )

    /**
     * REGEX-TEST: Remaining Clicks: 22
     */
    private val remainingClicksPattern by ExperimentationTableApi.patternGroup.pattern(
        "clicks",
        "Remaining Clicks: (?<clicks>\\d+)",
    )

    /**
     * REGEX-TEST: GUARDIAN;4
     */
    private val guardianPetInternalNamePattern by ExperimentationTableApi.patternGroup.pattern(
        "guardian.pet.internalname",
        "GUARDIAN;\\d"
    )

    /**
     * REGEX-TEST: 123k Enchanting Exp
     * REGEX-TEST: [Lvl 1] Guardian
     * REGEX-TEST: Enchanted Book
     * REGEX-TEST: Experience Bottle
     * REGEX-TEST: Grand Experience Bottle
     * REGEX-TEST: Titanic Experience Bottle
     */
    @Suppress("MaxLineLength")
    private val rewardPattern by ExperimentationTableApi.patternGroup.pattern(
        "rewards",
        "\\d{1,3}k Enchanting Exp|Enchanted Book|\\[Lvl \\d+] Guardian|(?:Grand |Titanic )?Experience Bottle",
    )

    /**
     * REGEX-TEST: Gained +3 Clicks
     */
    private val powerUpPattern by ExperimentationTableApi.patternGroup.pattern(
        "powerups",
        "Gained \\+\\d Clicks?|Instant Find|\\+\\S* XP",
    )

    /**
     * REGEX-TEST: Click any button!
     * REGEX-TEST: Next button is instantly rewarded!
     * REGEX-TEST: Click a second button!
     */
    private val waitingMessagesPattern by ExperimentationTableApi.patternGroup.pattern(
        "waiting.messages",
        "Click any button!|Click a second button!|Next button is instantly rewarded!",
    )
    // <editor-fold>

    private val emptySuperpairItem = SuperpairItem(-1, "", -1)

    private var display = emptyList<String>()
    private var uncoveredItems = mapOf<Int, SuperpairItem>()
    private val currentFoundData = mutableMapOf<FoundType, MutableList<FoundData>>()

    @HandleEvent
    fun onInventoryClose() {
        display = emptyList()
        uncoveredItems = emptyMap()
        currentFoundData.clear()
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.superpairs.display || !ExperimentationTableApi.inTable) return

        display = display.takeIfNotEmpty()
            ?: drawDisplay().takeIfNotEmpty()
            ?: return

        config.superpairs.displayPosition.renderStrings(
            display,
            posLabel = "Superpair Experimentation Data",
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.superpairs.display) return
        val currentTier = ExperimentationTableApi.currentExperimentTier ?: return

        val item = event.item ?: return
        if (isOutOfBounds(event.slotId, currentTier) || item.displayName.removeColor() == "?") return

        val items = uncoveredItems.toMutableMap()
        val itemExistsInData = items.any { it.value.slotId == event.slotId && it.key == items.keys.max() }
        val clicksItem = InventoryUtils.getItemAtSlotIndex(4)
        val hasRemainingClicks = remainingClicksPattern.matchMatcher(clicksItem?.displayName?.removeColor().orEmpty()) {
            group("clicks").toInt() > 0
        } ?: false

        if (!itemExistsInData && hasRemainingClicks) handleItem(items, event.slotId)
        uncoveredItems = items
    }

    private fun handleItem(items: MutableMap<Int, SuperpairItem>, slot: Int) = DelayedRun.runDelayed(200.milliseconds) {
        val itemNow = InventoryUtils.getItemAtSlotIndex(slot) ?: return@runDelayed
        val itemName = itemNow.displayName.removeColor()
        val reward = itemNow.convertToReward()
        val itemData = SuperpairItem(slot, reward, DyeCompat.toDamage(itemNow))
        val uncovered = items.keys.maxOrNull() ?: -1

        if (isWaiting(itemName)) return@runDelayed
        if (items.none { it.key == uncovered && it.value.slotId == slot }) items[uncovered + 1] = itemData

        when {
            isPowerUp(reward) -> handlePowerUp(items, itemData, uncovered + 1)
            isReward(itemName) || isMiscReward(itemNow) -> handleReward(items, itemData, uncovered + 1)
        }

        val since = clicksSinceSeparator(items)

        val lastReward = items.entries.lastOrNull()?.value?.reward.orEmpty()
        val isLastInstantFind = instantFindNamePattern.matches(lastReward)
        if ((since >= 2 || (since == -1 && items.size >= 2)) && !isLastInstantFind)
            items[uncovered + 2] = emptySuperpairItem

        display = drawDisplay()
    }

    private fun handlePowerUp(items: MutableMap<Int, SuperpairItem>, item: SuperpairItem, uncovered: Int) {
        if (!instantFindNamePattern.matches(item.reward)) items.remove(uncovered)

        val itemData = FoundData(item = item)
        currentFoundData.getOrPut(FoundType.POWERUP) { mutableListOf(itemData) }.apply { if (!contains(itemData)) add(itemData) }
    }

    private fun handleReward(items: MutableMap<Int, SuperpairItem>, item: SuperpairItem, uncovered: Int) {
        val last = items.getOrDefault(uncovered - 1, item)

        if (isWaiting(last.reward)) return

        when {
            instantFindNamePattern.matches(last.reward) -> handleInstantFind(items, item, uncovered)
            hasFoundPair(item, last) -> handleFoundPair(item, last)
            hasFoundMatch(items, item) -> handleFoundMatch(items, item)
            else -> handleNormalReward(item)
        }
    }

    private fun handleInstantFind(items: MutableMap<Int, SuperpairItem>, item: SuperpairItem, uncovered: Int) {
        items[uncovered - 1] = item
        items[uncovered] = emptySuperpairItem

        handleFoundPair(item, emptySuperpairItem)
    }

    private fun handleFoundPair(
        foundFirst: SuperpairItem,
        foundSecond: SuperpairItem,
    ) {
        // Remove from matched & normal, since it's now found
        currentFoundData[FoundType.MATCH]?.removeIf { data ->
            listOf(data.first, data.second).any {
                it.sameAs(foundFirst)
            }
        }
        currentFoundData[FoundType.NORMAL]?.removeIf { data ->
            val dataItem = data.item ?: return@removeIf false
            listOf(foundFirst, foundSecond).any {
                it.sameAs(dataItem)
            }
        }

        val pairData = FoundData(first = foundFirst, second = foundSecond)
        currentFoundData.getOrPut(FoundType.PAIR) { mutableListOf() }
            .apply { if (pairData !in this) add(pairData) }
    }

    private fun handleFoundMatch(
        items: MutableMap<Int, SuperpairItem>,
        item: SuperpairItem
    ) {
        val matchingItem = items.values.find { it.slotId != item.slotId && it.sameAs(item) } ?: return
        val slotIds = listOf(item.slotId, matchingItem.slotId)

        for ((foundType, dataList) in currentFoundData.entries) {
            when (foundType) {
                FoundType.MATCH, FoundType.PAIR -> {
                    // If any data already contains one of the slot IDs in either first or second, exit.
                    val alreadyExists = dataList.any { it.first?.slotId in slotIds || it.second?.slotId in slotIds }
                    if (alreadyExists) return
                }
                // Remove data where the associated item's slotId is one of the found IDs.
                FoundType.NORMAL -> dataList.removeIf { data -> data.item?.slotId in slotIds }
                else -> {}
            }
        }

        val pairData = FoundData(first = item, second = matchingItem)
        currentFoundData.getOrPut(FoundType.MATCH) { mutableListOf(pairData) }.apply { if (!contains(pairData)) add(pairData) }
    }

    private fun handleNormalReward(item: SuperpairItem) {
        val slotIds = listOf(item.slotId)

        for ((foundType, dataList) in currentFoundData.entries) {
            when (foundType) {
                FoundType.MATCH, FoundType.PAIR -> {
                    // If any data already contains one of the slot IDs in either first or second, exit.
                    val alreadyExists = dataList.any { it.first?.slotId in slotIds || it.second?.slotId in slotIds }
                    if (alreadyExists) return
                }
                else -> {
                    val exists = dataList.any { it.item != null && it.item.slotId == item.slotId && it.item.sameAs(item) }
                    if (exists) return
                }
            }
        }

        val itemData = FoundData(item = item)
        currentFoundData.getOrPut(FoundType.NORMAL) { mutableListOf(itemData) }.apply { if (!contains(itemData)) add(itemData) }
    }

    private val disallowedTypes = listOf(
        TaskType.ULTRASEQUENCER,
        TaskType.CHRONOMATRON,
    )

    private fun drawDisplay() = buildList {
        val currentExperimentType = ExperimentationTableApi.currentExperimentType
        val isValid = currentExperimentType == null || currentExperimentType !in disallowedTypes
        if (!isValid) return@buildList
        add("§6Superpair Experimentation Data")
        if (currentExperimentType == null) return@buildList

        val currentTier = ExperimentationTableApi.currentExperimentTier ?: return@buildList
        add("")

        val normals = currentFoundData.entries.firstOrNull { it.key == FoundType.NORMAL }?.value.orEmpty()
        val pairs = currentFoundData.entries.firstOrNull { it.key == FoundType.PAIR }?.value.orEmpty()
        val matches = currentFoundData.entries.firstOrNull { it.key == FoundType.MATCH }?.value.orEmpty()
        val powerups = currentFoundData.entries.firstOrNull { it.key == FoundType.POWERUP }?.value.orEmpty()
        val possiblePairs = calculatePossiblePairs(currentTier)
        val notCollected = buildList {
            if (possiblePairs >= 1) add("§ePairs - $possiblePairs")
            if (2 - powerups.size >= 1) add("§bPowerUps - ${2 - powerups.size}")
            if (normals.isNotEmpty()) add("§7Normals - ${normals.size}")
        }

        addFoundData(pairs, "§2Collected", LorenzColor.GREEN)
        addFoundData(matches, "§eMatched", LorenzColor.YELLOW)
        addFoundData(powerups, "§bPowerUp", LorenzColor.BLUE) { it.item?.reward.orEmpty() }
        addDataStrings(notCollected, "§4Not Collected")
    }

    private fun MutableList<String>.addDataStrings(dataList: List<String>, header: String) {
        if (dataList.isEmpty()) return
        this.add("")
        this.add(header)
        val lastIndex = dataList.lastIndex
        for ((index, entry) in dataList.withIndex()) {
            val prefix = determinePrefix(index, lastIndex)
            this.add(" $prefix $entry")
        }
    }

    private fun MutableList<String>.addFoundData(
        sourceList: List<FoundData>,
        header: String,
        color: LorenzColor,
        displayAccessor: (FoundData) -> String = { it.first?.reward.orEmpty() }
    ) = addDataStrings(sourceList.map { "${color.getChatColor()}${displayAccessor.invoke(it)}" }, header)

    private fun calculatePossiblePairs(currentExperiment: ExperimentationTableApi.ExperimentationTier) =
        ((currentExperiment.gridSize - 2) / 2) - currentFoundData.filter { it.key != FoundType.POWERUP }.values.sumOf { it.size }

    private fun ItemStack.convertToReward() = when {
        guardianPetInternalNamePattern.matches(getInternalNameOrNull()?.asString().orEmpty()) -> displayName.split("] ")[1]
        displayName.removeColor() == "Enchanted Book" -> getLore()[2].removeColor()
        else -> displayName.removeColor()
    }

    private fun determinePrefix(index: Int, lastIndex: Int) = if (index == lastIndex) "└" else "├"

    private fun hasFoundPair(
        first: SuperpairItem,
        second: SuperpairItem,
    ) = first.slotId != second.slotId && first.sameAs(second)

    private fun existsMatchingItem(
        items: Map<Int, SuperpairItem>,
        targetItem: SuperpairItem
    ): Boolean = items.any { (_, item) ->
        item.slotId != targetItem.slotId && item.sameAs(targetItem)
    }

    private fun isItemAlreadyFound(targetItem: SuperpairItem): Boolean = currentFoundData.any { (type, list) ->
        type.isAnyOf(FoundType.PAIR, FoundType.MATCH) && list.any { data ->
            targetItem.slotId.equalsOneOf(data.first?.slotId ?: -1, data.second?.slotId ?: -1)
        }
    }

    private fun hasFoundMatch(items: Map<Int, SuperpairItem>, firstItem: SuperpairItem): Boolean =
        existsMatchingItem(items, firstItem) && !isItemAlreadyFound(firstItem)

    private fun isPowerUp(reward: String) = powerUpPattern.matches(reward)

    private fun isReward(reward: String) = rewardPattern.matches(reward) || isPowerUp(reward)

    private fun isMiscReward(item: ItemStack) = item.getInternalNameOrNull() in ExperimentationTableApi.miscRepoRewards

    private fun isWaiting(itemName: String) = waitingMessagesPattern.matches(itemName)

    private fun clicksSinceSeparator(list: MutableMap<Int, SuperpairItem>): Int {
        val lastIndex = list.entries.indexOfLast { it.value == emptySuperpairItem }
        return if (lastIndex != -1) list.size - 1 - lastIndex else -1
    }

    private fun isOutOfBounds(slot: Int, experiment: ExperimentationTableApi.ExperimentationTier): Boolean = slot !in experiment.slotRange

    private fun SuperpairItem?.sameAs(other: SuperpairItem) = this?.reward == other.reward && this.damage == other.damage

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val pathBase = "inventory.experimentationTable"
        event.move(92, "$pathBase.superpairDisplay", "$pathBase.superpairs.display")
        event.move(92, "$pathBase.superpairDisplayPosition", "$pathBase.superpairs.displayPosition")
    }
}
