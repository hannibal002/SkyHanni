package at.hannibal2.hanni.features.garden.visitor

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.StringUtils.removeColor

@HanniModule
object GardenVisitorColorNames {

    private val visitorColors = mutableMapOf<String, String>() // name -> color code
    var visitorItems = mutableMapOf<String, List<String>>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        visitorColors.clear()
        visitorItems.clear()
        for ((visitor, visitorData) in data.visitors) {
            visitorColors[visitor] = visitorData.rarity.color.getChatColor()
            visitorItems[visitor] = visitorData.needItems
        }
    }

    fun getColoredName(name: String): String {
        val cleanName = name.removeColor()
        val color = visitorColors[cleanName] ?: return name
        return color + cleanName
    }
}
