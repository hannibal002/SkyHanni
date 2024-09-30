package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object WarpMenuUniques {
    private val islandNamePattern by RepoPattern.pattern(
        "inventory.warpmenu.island.name",
        "^§[ab](?<name>[\\w ']+)(?:§7 - §b.*)?\$"
    )

    private val collectedEggStorage: MutableMap<IslandType, MutableSet<LorenzVec>>
        get() = ChocolateFactoryAPI.profileStorage?.collectedEggLocations ?: mutableMapOf()

    private val config get() = SkyHanniMod.feature.event.hoppityEggs

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!config.uniquesWarpMenu) return
        if (!HoppityAPI.isHoppityEvent()) return
        if (event.slot.inventory.name != "Fast Travel") return

        islandNamePattern.matchMatcher(event.slot.stack.displayName) {
            val island = when (val name = group("name")) {
                "SkyBlock Hub" -> IslandType.HUB
                "The Barn" -> IslandType.THE_FARMING_ISLANDS
                else -> IslandType.getByNameOrNull(name) ?: return
            }

            val maxEggs = HoppityEggLocations.apiEggLocations[island]?.size ?: return
            val collectedEggs = collectedEggStorage[island]?.size ?: 0

            event.toolTip.add("§7Collected: ${if (collectedEggs == maxEggs) "§a" else ""}$collectedEggs/$maxEggs")
        }
    }
}
