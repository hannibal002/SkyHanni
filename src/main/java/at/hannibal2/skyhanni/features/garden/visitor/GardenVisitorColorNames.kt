package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenVisitor
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
object GardenVisitorColorNames {

    private val visitorColors = mutableMapOf<String, String>() // name -> color code
    var visitorMap = mutableMapOf<String, GardenVisitor>() // why is this here

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        visitorColors.clear()
        visitorMap.clear()
        for ((visitor, visitorData) in data.visitors) {
            visitorColors[visitor] = visitorData.rarity.color.getChatColor()
            visitorMap[visitor] = visitorData
        }
    }

    fun getColoredName(name: String): String {
        val cleanName = name.removeColor()
        val color = visitorColors[cleanName] ?: return name
        return color + cleanName
    }
}
