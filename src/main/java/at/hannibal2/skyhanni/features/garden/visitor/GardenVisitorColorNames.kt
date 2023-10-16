package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenVisitorColorNames {

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        try {
            val mapColor = mutableMapOf<String, String>()
            val mapItems = mutableMapOf<String, List<String>>()
            event.getConstant("Garden")?.let { garden ->
                for ((name, element) in garden["visitors"].asJsonObject.entrySet()) {
                    val jsonObject = element.asJsonObject
                    val rarity = jsonObject["rarity"].asString
                    mapColor[name] = getColor(rarity)
                    mapItems[name] = jsonObject["need_items"].asJsonArray.map { it.asString }
                }
            }
            visitorColor = mapColor
            visitorItems = mapItems

        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    companion object {
        private var visitorColor = mapOf<String, String>() // name -> color code
        var visitorItems = mapOf<String, List<String>>()

        fun getColoredName(name: String): String {
            if (!SkyHanniMod.feature.garden.visitors.coloredName) return name

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