package at.hannibal2.skyhanni.features.fishing.tracker

import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.jsonobjects.FishingProfitItemsJson
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FishingTrackerCategoryManager {

    var itemCategories = mapOf<String, List<NEUInternalName>>()

    private var shItemCategories = mapOf<String, List<NEUInternalName>>()
    private var neuItemCategories = mapOf<String, List<NEUInternalName>>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        shItemCategories = event.getConstant<FishingProfitItemsJson>("FishingProfitItems").categories
        updateItemCategories()
    }

    private fun updateItemCategories() {
        itemCategories = shItemCategories + neuItemCategories
    }

    @SubscribeEvent
    fun onNeuRepoReload(event: io.github.moulberry.notenoughupdates.events.RepositoryReloadEvent) {
        val totalDrops = mutableListOf<String>()
        val dropCategories = mutableMapOf<String, MutableList<NEUInternalName>>()
        for ((seaCreature, data) in NotEnoughUpdates.INSTANCE.manager.itemInformation.filter { it.key.endsWith("_SC") }) {
            val asJsonObject = data.getAsJsonArray("recipes")[0].asJsonObject
            val drops = asJsonObject.getAsJsonArray("drops")
                .map { it.asJsonObject.get("id").asString }.map { it.split(":").first() }
            val asJsonArray = asJsonObject.get("extra")
            val extra = asJsonArray?.let {
                asJsonArray.asJsonArray.toList()
                    .map { it.toString() }
                    .filter { !it.contains("Fishing Skill") && !it.contains("Requirements:") && !it.contains("Fished from water") }
                    .joinToString(" + ")
            } ?: "null"
            val category = if (extra.contains("Fishing Festival")) {
                "Fishing Festival"
            } else if (extra.contains("Spooky Festival")) {
                "Spooky Festival"
            } else if (extra.contains("Jerry's Workshop")) {
                "Jerry's Workshop"
            } else if (extra.contains("Oasis")) {
                "Oasis"
            } else if (extra.contains("Magma Fields") || extra.contains("Precursor Remnants") ||
                extra.contains("Goblin Holdout")
            ) {
                "Crystal Hollows"
            } else if (extra.contains("Crimson Isle Lava")) {
                "Crimson Isle Lava"
            } else {
                if (extra.isNotEmpty()) {
                    println("unknown extra: $extra = $seaCreature ($drops)")
                }
                "Water"
            } + " Sea Creatures"
            for (drop in drops) {
                if (drop !in totalDrops) {
                    totalDrops.add(drop)
                    dropCategories.getOrPut(category) { mutableListOf() }.add(drop.asInternalName())
                }
            }
        }
        neuItemCategories = dropCategories
        updateItemCategories()
    }
}
