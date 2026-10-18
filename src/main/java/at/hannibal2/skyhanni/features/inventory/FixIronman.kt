package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component

@SkyHanniModule
object FixIronman {
    private val patternGroup = RepoPattern.group("data.fixironman")

    /**
     * REGEX-TEST: Select a Special Mode
     */
    private val selectModePattern by patternGroup.pattern(
        "selectmode",
        "Select a Special Mode",
    )

    /**
     * REGEX-TEST: Profile Management
     */
    private val profileManagementPattern by patternGroup.pattern(
        "profilemanagement",
        "Profile Management",
    )

    /**
     * REGEX-TEST: Visit liron150
     */
    private val visitPattern by patternGroup.pattern(
        "visit",
        "Visit .*",
    )

    /**
     * REGEX-TEST: SkyBlock Leveling
     */
    private val sbLevelingPattern by patternGroup.pattern(
        "sbleveling",
        "SkyBlock Leveling",
    )

    private val selectModeInventory = InventoryDetector { selectModePattern }
    private val profileManagementInventory = InventoryDetector { profileManagementPattern }
    private val visitInventory = InventoryDetector { visitPattern }
    private val sbLevelingInventory = InventoryDetector { sbLevelingPattern }

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
                event.toolTip[index] = line.replace("Ironman", "Ironperson") ?: line
            }
        }

        if (selectModeInventory.isInside()) {
            for ((index, line) in event.toolTip.withIndex()) {
                if (line.string.contains("No Auction House!")) {
                    event.toolTip[index] = line.replace("No Auction House!", "Ironperson-Only Auction House!") ?: line
                }
            }
        }
    }

    @HandleEvent
    fun onChat(event: SystemMessageEvent.Modify) {
        // We don't need to always fix this
        if (!TimeUtils.isAprilFoolsDay) return

        if (event.message.contains("Ironman")) {
            val newComponent = event.chatComponent.replace("Ironman", "Ironperson") ?: return
            event.replaceComponent(newComponent, "fix_ironman")
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
