package at.hannibal2.hanni.features.garden.inventory

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.EntityMovementData
import at.hannibal2.hanni.data.IslandGraphs
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.ItemClickEvent
import at.hannibal2.hanni.events.item.ItemHoverEvent
import at.hannibal2.hanni.features.garden.GardenApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@HanniModule
object CarrolynHelper {
    private val config get() = GardenApi.config

    private val carrolynLocation = LorenzVec(0.5, 103.1, -803.7)

    private val patternGroup = RepoPattern.group("garden.carrolyn")

    /**
     * REGEX-TEST: §7Bring §63,000 §7of these to §5Carrolyn §7in
     */
    private val lorePattern by patternGroup.pattern(
        "lore",
        "§7Bring §63,000 §7of these to §5Carrolyn §7in",
    )

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onTooltip(event: ItemHoverEvent) {
        if (!isEnabled()) return

        if (!event.itemStack.isCarrolynItem()) return

        event.toolTip.add("")
        event.toolTip.add("§eClick to navigate to Carrolyn!")
    }

    private fun ItemStack?.isCarrolynItem() = this?.getLore()?.any { lorePattern.matches(it) } ?: false

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemClick(event: ItemClickEvent) {
        if (!isEnabled()) return

        if (!event.itemInHand.isCarrolynItem()) return

        if (SkyBlockUtils.currentIsland == IslandType.CRIMSON_ISLE) {
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
        IslandGraphs.pathFind(carrolynLocation, "§5Carrolyn", condition = { isEnabled() })
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.helpCarrolyn

}
