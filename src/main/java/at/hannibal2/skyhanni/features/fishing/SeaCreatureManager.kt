package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.jsonobjects.SeaCreatures
import at.hannibal2.skyhanni.utils.jsonobjects.SeaCreatures.SeaCreature
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SeaCreatureManager {

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        seaCreatureMap.clear()
        var counter = 0

        try {
            seaCreatures = event.getConstant<Map<String, SeaCreatures>>("SeaCreatures") ?: return

            val fishingMobs = mutableMapOf<String, SeaCreature>()
            for ((_, variant) in seaCreatures) {
                val chatColor = variant.chat_color
                for ((displayName, seaCreature) in variant.sea_creatures) {
                    fishingMobs[displayName] = seaCreature
                    val chatMessage = seaCreature.chat_message
                    val fishingExperience = seaCreature.fishing_experience
                    val rare = seaCreature.rare

                    seaCreatureMap[chatMessage] = UsefulSeaCreature(displayName, fishingExperience, chatColor, rare)
                    counter++
                }
            }
            allFishingMobNames = allFishingMobs.map { it.value.displayName }
            LorenzUtils.debug("Loaded $counter sea creatures from repo")

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    companion object {
        var seaCreatures = emptyMap<String, SeaCreatures>()
        var allFishingMobs = mutableMapOf<String, UsefulSeaCreature>()
        private val seaCreatureMap = mutableMapOf<String, UsefulSeaCreature>()
        var allFishingMobNames = emptyList<String>()

        fun getSeaCreature(message: String): UsefulSeaCreature? {
            return seaCreatureMap.getOrDefault(message, null)
        }
    }
}