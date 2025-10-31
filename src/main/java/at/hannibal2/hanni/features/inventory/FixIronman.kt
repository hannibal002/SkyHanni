package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.hanni.events.item.ItemHoverEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryDetector
import at.hannibal2.hanni.utils.TimeUtils
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent

@HanniModule
object FixIronman {
    private val selectModeInventory = InventoryDetector { name -> name == "Select a Special Mode" }
    private val profileManagementInventory = InventoryDetector { name -> name == "Profile Management" }
    private val visitInventory = InventoryDetector { name -> name.startsWith("Visit ") }
    private val sbLevelingInventory = InventoryDetector { name -> name == "SkyBlock Leveling" }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltipEvent(event: ItemHoverEvent) {
        // We don't need to always fix this
        if (!TimeUtils.isAprilFoolsDay) return

        if (!profileManagementInventory.isInside() &&
            !selectModeInventory.isInside() &&
            !visitInventory.isInside() &&
            !sbLevelingInventory.isInside()
        ) return

        for ((index, line) in event.toolTip.withIndex()) {
            if (line.contains("Ironman")) {
                event.toolTip[index] = line.replace("Ironman", "Ironperson")
            }
        }

        if (selectModeInventory.isInside()) {
            for ((index, line) in event.toolTip.withIndex()) {
                if (line.contains("No Auction House!")) {
                    event.toolTip[index] = line.replace("No Auction House!", "Ironperson-Only Auction House!")
                }
            }
        }
    }

    @HandleEvent
    fun onChat(event: SystemMessageEvent) {
        // We don't need to always fix this
        if (!TimeUtils.isAprilFoolsDay) return

        if (event.message.contains("Ironman")) {
            event.chatComponent = event.message.replace("Ironman", "Ironperson").asComponent()
        }
    }

    fun fixScoreboard(text: String): String? {
        return if (TimeUtils.isAprilFoolsDay && text.contains("Ironman")) {
            text.replace("Ironman", "Ironperson")
        } else null
    }

    fun getIronmanName(): String {
        return if (TimeUtils.isAprilFoolsDay) {
            "Ironperson"
        } else "Ironman"
    }
}
