package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CarrolynHelper {
    private val config get() = GardenApi.config

    private val patternGroup = RepoPattern.group("garden.carrolyn")

    /**
     * REGEX-TEST: §7Bring §63,000 §7of these to §5Carrolyn §7in
     */
    private val lorePattern by patternGroup.pattern(
        "lore",
        "§7Bring §63,000 §7of these to §5Carrolyn §7in",
    )

    @HandleEvent(priorityLevel = LOW)
    private fun onTooltip(event: ToolTipTextEvent) {
        if (!isEnabled()) return

        if (!event.itemStack.isCarrolynItem()) return

        event.toolTip.add("")
        event.toolTip.add("§eClick to navigate to Carrolyn!")
    }

    private fun SafeItemStack?.isCarrolynItem() = this?.getLore()?.any { lorePattern.matches(it) } ?: false

    @HandleEvent(onlyOnSkyblock = true)
    private fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled()) return

        if (!event.itemInHand.isCarrolynItem()) return

        if (IslandType.CRIMSON_ISLE.isInIsland()) {
            startPathfind()
        } else {
            ChatUtils.clickableChat(
                "Carrolyn is on the Crimson Isle. Click here to warp there!",
                onClick = {
                    HypixelCommands.warp("crimson")
                    EntityMovementData.onNextTeleport(CRIMSON_ISLE) {
                        startPathfind()
                    }
                },
                replaceSameMessage = true,
            )
        }
    }

    private fun startPathfind() {
        IslandGraphs.node("Carrolyn", NPC).pathFind("§5Carrolyn") { isEnabled() }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.helpCarrolyn
}
