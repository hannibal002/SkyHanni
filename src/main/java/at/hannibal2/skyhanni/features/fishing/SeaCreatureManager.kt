package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.jsonobjects.SeaCreatureJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SeaCreatureManager {

    private var doubleHook = false

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (doubleHookMessages.contains(event.message)) {
            if (SkyHanniMod.feature.fishing.compactDoubleHook) {
                event.blockedReason = "double_hook"
            }
            doubleHook = true
        } else {
            val seaCreature = getSeaCreature(event.message)
            if (seaCreature != null) {
                SeaCreatureFishEvent(seaCreature, event, doubleHook).postAndCatch()
            }
            doubleHook = false
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        seaCreatureMap.clear()
        var counter = 0

        try {
            val data = event.getConstant<Map<String, SeaCreatureJson.Variant>>("SeaCreatures", SeaCreatureJson.TYPE) ?: return
            val allFishingMobs = mutableMapOf<String,SeaCreature>()

            for (variant in data.values) {
                val chatColor = variant.chat_color
                for ((displayName, seaCreature) in variant.sea_creatures) {
                    val chatMessage = seaCreature.chat_message
                    val fishingExperience = seaCreature.fishing_experience
                    val rarity = seaCreature.rarity
                    val rare = seaCreature.rare ?: false

                    val creature = SeaCreature(displayName, fishingExperience, chatColor, rare, rarity)
                    seaCreatureMap[chatMessage] = creature
                    allFishingMobs[displayName] = creature
                    counter++
                }
            }
            LorenzUtils.debug("Loaded $counter sea creatures from repo")

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    companion object {
        private val seaCreatureMap = mutableMapOf<String, SeaCreature>()
        var allFishingMobs = mutableMapOf<String, SeaCreature>()

        private val doubleHookMessages = setOf(
            "§eIt's a §r§aDouble Hook§r§e! Woot woot!",
            "§eIt's a §r§aDouble Hook§r§e!"
        )

        fun getSeaCreature(message: String): SeaCreature? {
            return seaCreatureMap.getOrDefault(message, null)
        }
    }
}