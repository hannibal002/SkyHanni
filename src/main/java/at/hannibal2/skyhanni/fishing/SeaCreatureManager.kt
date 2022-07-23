package at.hannibal2.skyhanni.fishing

import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SeaCreatureManager {

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        seaCreatureMap.clear()
        var counter = 0

        try {
            val data = event.getConstant("SeaCreatures")!!

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
                    counter++
                }
            }
            LorenzUtils.debug("loaded $counter sea creatures from repo")

//            seaCreatures.asJsonArray.map { it.asJsonObject }.forEach {
//                val displayName = it["display_name"].asString
//                val chatMessage = it["chat_message"].asString
//                val fishingExperience = it["fishing_experience"].asInt
//                val variantName = it["variant"].asString
//                val special = it["special"].asBoolean
//
//                val variant = try {
//                    FishingVariant.fromString(variantName)
//                } catch (e: FishingVariantNotFoundException) {
//                    LorenzUtils.error("Error loading Sea Creature '$displayName': " + e.message)
//                    return
//                }
//
//                seaCreatureMap[chatMessage] = SeaCreature(displayName, fishingExperience, variant, special)
//            }

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    companion object {
        val seaCreatureMap = mutableMapOf<String, SeaCreature>()

        fun getSeaCreature(message: String): SeaCreature? {
            return seaCreatureMap.getOrDefault(message, null)
        }
    }
}