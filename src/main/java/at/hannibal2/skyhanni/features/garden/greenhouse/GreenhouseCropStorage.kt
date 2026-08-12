package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.LorenzVec

internal class GreenhouseCropStorage {

    private val storage get() = ProfileStorageData.profileSpecific?.garden?.greenhouse

    private val runtimeDetectedCropsByPlot = mutableMapOf<Int, MutableSet<String>>()
    private val runtimeDetectedCropPositionsByPlot = mutableMapOf<Int, MutableMap<String, LorenzVec>>()
    private val runtimeIgnoredReplacementsByPlot = mutableMapOf<Int, MutableSet<String>>()
    private var pendingPersistentSave = false

    fun syncRuntimeData() {
        val profileStorage = storage ?: return
        // Gson can deserialize fields explicitly stored as null without applying Kotlin's non-null
        // constructor defaults, so repair every legacy profile map before merging runtime data.
        var repairedNullMap = false
        val detectedPositions = profileStorage.detectedCropPositionsByPlot ?: mutableMapOf<Int, MutableMap<String, LorenzVec>>()
            .also {
                profileStorage.detectedCropPositionsByPlot = it
                repairedNullMap = true
            }
        val ignoredReplacements = profileStorage.ignoredCropReplacementsByPlot ?: mutableMapOf<Int, MutableSet<String>>()
            .also {
                profileStorage.ignoredCropReplacementsByPlot = it
                repairedNullMap = true
            }
        val changed = detectedPositions.merge(runtimeDetectedCropPositionsByPlot) or
            ignoredReplacements.mergeSets(runtimeIgnoredReplacementsByPlot)
        if (repairedNullMap || changed) pendingPersistentSave = true
        savePendingData()
    }

    fun rememberDetectedCrops(plotId: Int, positions: Map<CropCategory, LorenzVec>) {
        if (positions.isEmpty()) return
        val seenNames = positions.keys.mapTo(mutableSetOf()) { it.name }
        val runtimeChanged = runtimeDetectedCropsByPlot
            .getOrPut(plotId) { mutableSetOf() }
            .addAll(seenNames)
        val storageChanged = storage?.detectedCropsByPlot
            ?.getOrPut(plotId) { mutableSetOf() }
            ?.addAll(seenNames) == true
        saveDetectedCropPositions(plotId, positions)
        if (runtimeChanged || storageChanged) pendingPersistentSave = true
        savePendingData()
    }

    fun updateDetectedCrops(
        plotId: Int,
        presentNames: MutableSet<String>,
        positions: Map<CropCategory, LorenzVec>,
    ) {
        if (runtimeDetectedCropsByPlot[plotId] != presentNames) {
            runtimeDetectedCropsByPlot[plotId] = presentNames
            pendingPersistentSave = true
        }
        storage?.detectedCropsByPlot?.let {
            if (it[plotId] != presentNames) {
                it[plotId] = presentNames
                pendingPersistentSave = true
            }
        }
        saveDetectedCropPositions(plotId, positions)
        savePendingData()
    }

    fun clearAll() {
        runtimeDetectedCropsByPlot.clear()
        runtimeDetectedCropPositionsByPlot.clear()
        runtimeIgnoredReplacementsByPlot.clear()
        storage?.detectedCropsByPlot?.clear()
        storage?.detectedCropPositionsByPlot?.clear()
        storage?.ignoredCropReplacementsByPlot?.clear()
        pendingPersistentSave = true
        savePendingData()
    }

    fun moveCrop(category: CropCategory, newPlotId: Int, position: LorenzVec) {
        val name = category.name
        listOfNotNull(
            runtimeDetectedCropPositionsByPlot,
            storage?.detectedCropPositionsByPlot,
        ).forEach { positionsByPlot ->
            positionsByPlot.values.forEach { it.remove(name) }
        }
        runtimeDetectedCropsByPlot.values.forEach { it.remove(name) }
        storage?.detectedCropsByPlot?.values?.forEach { it.remove(name) }

        runtimeDetectedCropsByPlot.getOrPut(newPlotId) { mutableSetOf() }.add(name)
        storage?.detectedCropsByPlot?.getOrPut(newPlotId) { mutableSetOf() }?.add(name)
        runtimeIgnoredReplacementsByPlot.values.forEach { it.remove(name) }
        storage?.ignoredCropReplacementsByPlot?.values?.forEach { it.remove(name) }
        saveDetectedCropPositions(newPlotId, mapOf(category to position))
        pendingPersistentSave = true
        savePendingData()
    }

    fun rememberIgnoredCropReplacement(plotId: Int, category: CropCategory) {
        val name = category.name
        runtimeIgnoredReplacementsByPlot.getOrPut(plotId) { mutableSetOf() }.add(name)
        storage?.ignoredCropReplacementsByPlot?.getOrPut(plotId) { mutableSetOf() }?.add(name)
        pendingPersistentSave = true
        savePendingData()
    }

    fun ignoredCropReplacementsByPlot(): Map<Int, Set<String>> = buildMap {
        addStored(storage?.ignoredCropReplacementsByPlot.orEmpty())
        addStored(runtimeIgnoredReplacementsByPlot)
    }

    fun detectedCropsByPlot(): Map<Int, Set<String>> =
        storage?.detectedCropsByPlot.orEmpty() + runtimeDetectedCropsByPlot

    fun rememberedCropPositions(): Map<Int, Map<CropCategory, LorenzVec>> {
        val remembered = mutableMapOf<Int, MutableMap<CropCategory, LorenzVec>>()
        detectedCropPositionsByPlot().addCropPositionsTo(remembered)
        return remembered
    }

    fun saveDetectedCropPositions(plotId: Int, positions: Map<CropCategory, LorenzVec>) {
        val positionNames = positions.mapKeys { it.key.name }
        positionNames.keys.forEach {
            removeRememberedCategoryFromOtherPlots(it, plotId)
        }

        val runtimePositions = runtimeDetectedCropPositionsByPlot.getOrPut(plotId) { mutableMapOf() }
        val storedPositions = storage?.detectedCropPositionsByPlot?.getOrPut(plotId) { mutableMapOf() }
        var changed = false
        positionNames.forEach { (name, position) ->
            changed = runtimePositions.put(name, position) != position || changed
            changed = storedPositions?.put(name, position) != position || changed
        }
        if (changed) pendingPersistentSave = true
        savePendingData()
    }

    fun removeDetectedCropPositions(plotId: Int, names: Set<String>) {
        if (names.isEmpty()) return
        var changed = false
        listOfNotNull(
            runtimeDetectedCropPositionsByPlot,
            storage?.detectedCropPositionsByPlot,
        ).forEach { positionsByPlot ->
            val positions = positionsByPlot[plotId] ?: return@forEach
            names.forEach { changed = positions.remove(it) != null || changed }
        }
        runtimeDetectedCropsByPlot[plotId]?.let { changed = it.removeAll(names) || changed }
        storage?.detectedCropsByPlot?.get(plotId)?.let { changed = it.removeAll(names) || changed }
        if (changed) pendingPersistentSave = true
        savePendingData()
    }

    private fun removeRememberedCategoryFromOtherPlots(name: String, plotId: Int) {
        val runtime = runtimeDetectedCropPositionsByPlot
        val profile = storage?.detectedCropPositionsByPlot
        var changed = false
        listOfNotNull(runtime, profile).forEach { positionsByPlot ->
            positionsByPlot.filterKeys { it != plotId }.values.forEach {
                changed = it.remove(name) != null || changed
            }
        }
        if (changed) pendingPersistentSave = true
    }

    fun detectedCropPositionsByPlot(): Map<Int, Map<String, LorenzVec>> = buildMap {
        storage?.detectedCropPositionsByPlot.orEmpty().forEach { (plotId, positions) ->
            put(plotId, positions.toMutableMap())
        }
        runtimeDetectedCropPositionsByPlot.forEach { (plotId, positions) ->
            put(plotId, get(plotId).orEmpty() + positions)
        }
    }

    private fun savePendingData() {
        if (!pendingPersistentSave || storage == null) return
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "greenhouse-crop-detection")
        pendingPersistentSave = false
    }

}

private fun MutableMap<Int, MutableMap<String, LorenzVec>>.merge(
    source: Map<Int, Map<String, LorenzVec>>,
): Boolean {
    var changed = false
    source.forEach { (plotId, positions) ->
        val destination = getOrPut(plotId) { mutableMapOf() }
        positions.forEach { (name, position) ->
            changed = destination.put(name, position) != position || changed
        }
    }
    return changed
}

private fun MutableMap<Int, MutableSet<String>>.mergeSets(source: Map<Int, Set<String>>): Boolean {
    var changed = false
    source.forEach { (plotId, categories) ->
        changed = getOrPut(plotId) { mutableSetOf() }.addAll(categories) || changed
    }
    return changed
}

private fun MutableMap<Int, Set<String>>.addStored(stored: Map<Int, Set<String>>) {
    stored.forEach { (plotId, names) ->
        put(plotId, get(plotId).orEmpty() + names)
    }
}

private fun Map<Int, Map<String, LorenzVec>>.addCropPositionsTo(
    destination: MutableMap<Int, MutableMap<CropCategory, LorenzVec>>,
) {
    forEach { (plotId, positions) ->
        val remembered = destination.getOrPut(plotId) { mutableMapOf() }
        positions.forEach { (name, position) ->
            CropCategory.fromStorageName(name)?.let { remembered[it] = position }
        }
    }
}
