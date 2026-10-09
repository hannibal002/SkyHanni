package at.hannibal2.skyhanni.features.garden.inventory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CarrolynHelper {
    private val config get() = GardenApi.config

    private val patternGroup = RepoPattern.group("garden.carrolyn")

    /**
     * REGEX-TEST: Bring 3,000 of these to Carrolyn in
     */
    private val lorePattern by patternGroup.pattern(
        "lore.colorless",
        "Bring 3,000 of these to Carrolyn in",
    )

    @HandleEvent(priority = HandleEvent.LOWEST)
    private fun onTooltip(event: ToolTipTextEvent) {
        if (!isEnabled()) return

        if (!event.itemStack.isCarrolynItem()) return

        event.toolTip.add("")
        event.toolTip.add("§eClick to navigate to Carrolyn!")
    }

    private fun SafeItemStack?.isCarrolynItem() = this?.let { lorePattern.anyMatches(it.getCleanLore()) } ?: false

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
                    EntityMovementData.onNextTeleport(IslandType.CRIMSON_ISLE) {
                        startPathfind()
                    }
                },
                replaceSameMessage = true,
            )
        }
    }

    private fun startPathfind() {
        IslandGraphs.node("Carrolyn", GraphNodeTag.NPC).pathFind("§5Carrolyn") { isEnabled() }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.helpCarrolyn

}
