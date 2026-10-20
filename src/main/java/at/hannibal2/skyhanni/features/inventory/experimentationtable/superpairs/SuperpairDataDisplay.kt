package at.hannibal2.skyhanni.features.inventory.experimentationtable.superpairs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationSuperpairApi
import at.hannibal2.skyhanni.api.ExperimentationSuperpairApi.FoundData
import at.hannibal2.skyhanni.api.ExperimentationSuperpairApi.FoundType
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.TaskType
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.experiments.TableSuperpairDataUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty

@SkyHanniModule
object SuperpairDataDisplay {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable

    private var display = emptyList<String>()

    @HandleEvent
    fun onInventoryClose() {
        display = emptyList()
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onSuperpairDataUpdated(event: TableSuperpairDataUpdatedEvent) {
        if (!config.superpairs.display) return
        display = drawDisplay()
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onChestGuiRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.superpairs.display || !ExperimentationTableApi.inTable) return

        display = display.takeIfNotEmpty()
            ?: drawDisplay().takeIfNotEmpty()
            ?: return

        config.superpairs.displayPosition.renderStrings(
            display,
            posLabel = "Superpair Experimentation Data",
        )
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

        val normals = ExperimentationSuperpairApi.found(FoundType.NORMAL)
        val pairs = ExperimentationSuperpairApi.found(FoundType.PAIR)
        val matches = ExperimentationSuperpairApi.found(FoundType.MATCH)
        val powerups = ExperimentationSuperpairApi.found(FoundType.POWERUP)
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
        ((currentExperiment.gridSize - 2) / 2) - ExperimentationSuperpairApi.foundData.filter {
            it.key != FoundType.POWERUP
        }.values.sumOf { it.size }

    private fun determinePrefix(index: Int, lastIndex: Int) = if (index == lastIndex) "└" else "├"

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val pathBase = "inventory.experimentationTable"
        event.move(92, "$pathBase.superpairDisplay", "$pathBase.superpairs.display")
        event.move(92, "$pathBase.superpairDisplayPosition", "$pathBase.superpairs.displayPosition")
    }
}
