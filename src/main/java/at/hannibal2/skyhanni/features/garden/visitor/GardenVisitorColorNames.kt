package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GardenVisitorColorNames {
    private var visitorColours = mutableMapOf<String, String>() // name -> color code
    var visitorItems = mutableMapOf<String, List<String>>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        visitorColours.clear()
        visitorItems.clear()
        for ((visitor, visitorData) in data.visitors) {
            val rarity = visitorData.new_rarity ?: visitorData.rarity
            visitorColours[visitor] = rarity.color.getChatColor()
            visitorItems[visitor] = visitorData.need_items
        }
    }

    fun getColoredName(name: String): String {
        if (!SkyHanniMod.feature.garden.visitors.coloredName) return name

        val cleanName = name.removeColor()
        val color = visitorColours[cleanName] ?: return name
        return color + cleanName
    }
}
