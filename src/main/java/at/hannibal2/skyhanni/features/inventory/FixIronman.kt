package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.replace
import net.minecraft.network.chat.Component

@SkyHanniModule
object FixIronman {
    private val selectModeInventory = InventoryDetector { name -> name == "Select a Special Mode" }
    private val profileManagementInventory = InventoryDetector { name -> name == "Profile Management" }
    private val visitInventory = InventoryDetector { name -> name.startsWith("Visit ") }
    private val sbLevelingInventory = InventoryDetector { name -> name == "SkyBlock Leveling" }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltipEvent(event: ToolTipTextEvent) {
        // We don't need to always fix this
        if (!TimeUtils.isAprilFoolsDay) return

        if (!profileManagementInventory.isInside() &&
            !selectModeInventory.isInside() &&
            !visitInventory.isInside() &&
            !sbLevelingInventory.isInside()
        ) return

        for ((index, line) in event.toolTip.withIndex()) {
            if (line.string.contains("Ironman")) {
                event.toolTip[index] = line.replace("Ironman", "Ironperson")
            }
        }

        if (selectModeInventory.isInside()) {
            for ((index, line) in event.toolTip.withIndex()) {
                if (line.string.contains("No Auction House!")) {
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

    fun fixScoreboard(component: Component): Component? {
        return if (TimeUtils.isAprilFoolsDay && component.string.contains("Ironman")) {
            component.replace("Ironman", "Ironperson")
        } else null
    }

    fun getIronmanName(): String {
        return if (TimeUtils.isAprilFoolsDay) {
            "Ironperson"
        } else "Ironman"
    }
}
