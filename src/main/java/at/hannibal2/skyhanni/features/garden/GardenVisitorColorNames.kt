package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenVisitorColorNames {

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        try {
            val map = mutableMapOf<String, String>()
            val garden = event.getConstant("Garden")!!
            for ((name, element) in garden["visitors"].asJsonObject.entrySet()) {
                val rarity = element.asJsonObject["rarity"].asString
                map[name] = getColor(rarity)
            }
            visitorColor = map

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    companion object{
        private var visitorColor = mapOf<String, String>() // name -> color code

        fun getColoredName(name: String): String {
            if (!SkyHanniMod.feature.garden.visitorColoredName) return name

            val cleanName = name.removeColor()
            val color = visitorColor[cleanName] ?: return name
            return color + cleanName
        }
    }


    private fun getColor(rarity: String) = when (rarity) {
        "uncommon" -> "§a"
        "rare" -> "§9"
        "legendary" -> "§6"
        "special" -> "§c"

        else -> throw RuntimeException("Unknown rarity for '$rarity'")
    }
}