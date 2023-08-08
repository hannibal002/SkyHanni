package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SeaCreatureManager {

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        seaCreatureMap.clear()
        var counter = 0

        try {
            val data = event.getConstant("SeaCreatures") ?: return

            val fishingMobNames = mutableListOf<String>()
            for (variant in data.entrySet().map { it.value.asJsonObject }) {
                val chatColor = variant["chat_color"].asString
                for ((displayName, value) in variant["sea_creatures"].asJsonObject.entrySet()) {
                    val seaCreature = value.asJsonObject
                    val chatMessage = seaCreature["chat_message"].asString
                    val fishingExperience = seaCreature["fishing_experience"].asInt

                    val rare = if (seaCreature.has("rare")) {
                         seaCreature["rare"].asBoolean
                    } else false

                    seaCreatureMap[chatMessage] = SeaCreature(displayName, fishingExperience, chatColor, rare)
                    fishingMobNames.add(displayName)
                    counter++
                }
            }
            allFishingMobNames = fishingMobNames
            LorenzUtils.debug("Loaded $counter sea creatures from repo")

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    companion object {
        private val seaCreatureMap = mutableMapOf<String, SeaCreature>()
        var allFishingMobNames = emptyList<String>()

        fun getSeaCreature(message: String): SeaCreature? {
            return seaCreatureMap.getOrDefault(message, null)
        }
    }
}