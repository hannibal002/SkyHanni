package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object BonusPestChanceDisplay {

    private val config get() = GardenAPI.config.bonusPestChanceDisplay

    private val patternGroup = RepoPattern.group("garden.pestbonuschancedisplay")
    private val bonusPestChancePattern by patternGroup.pattern(
        "tablist.bonus",
        " Bonus Pest Chance: §r§2ൠ(?<bonus>\\d+)"
    )
    private val bonusPestChanceStatusPattern by patternGroup.pattern(
        "tablist.status",
        " §r§7§mBonus Pest Chance: ൠ(?<bonus>\\d+)"
    )

    private var display = emptyList<List<Any>>()

    private var tabPestBonus: Int = 0
    private var tabPestBonusStatus: Boolean = false

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        event.tabList.firstNotNullOfOrNull {
            bonusPestChanceStatusPattern.matchMatcher(it) {
                tabPestBonusStatus = true
                tabPestBonus = group("bonus").toInt()
            }
            bonusPestChancePattern.matchMatcher(it) {
                tabPestBonusStatus = false
                tabPestBonus = group("bonus").toInt()
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (GardenAPI.hideExtraGuis()) return
        if (GardenAPI.toolInHand == null) return
        config.pos.renderStringsAndItems(display, posLabel = "Bonus Pest Chance")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(5)) return

        val updatedDisplay = mutableListOf<List<Any>>()
        updatedDisplay.addAsSingletonList("§fBonus Pest Chance§7: §2ൠ$tabPestBonus")
        if (tabPestBonusStatus) {
            updatedDisplay.addAsSingletonList("§fEnabled: §cFalse")
        } else {
            updatedDisplay.addAsSingletonList("§fEnabled: §aTrue")
        }

        display = updatedDisplay
    }

    private fun isEnabled(): Boolean = GardenAPI.inGarden() && config.display
}
