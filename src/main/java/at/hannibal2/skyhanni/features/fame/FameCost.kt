package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FameCost {
    private val config get() = SkyHanniMod.feature.misc.fameCost

    private val cookieMenuPattern by RepoPattern.pattern("fame.cookiemenu", "Booster Cookie")
    private val fameItemNamePattern by RepoPattern.pattern("fame.fameitemname", "§eFame Rank")
    private val famePattern by RepoPattern.pattern(
        "fame.fameloreamount",
        "§7Your total: §e(?<fame>\\d{1,3}(?:,\\d{3})*|\\d+) Fame"
    )

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        if (!cookieMenuPattern.matches(event.slot.inventory.name)) return
        val name = event.itemStack.name ?: return
        if (!fameItemNamePattern.matches(name)) return
        var fameAmount = 0L
        event.itemStack.getLore().forEach {
            famePattern.matchMatcher(it) {
                fameAmount = group("fame").formatNumber()
            }
        }

        event.toolTip.add("§7Fame Cost: §a$${calculateDollarValue(fameAmount).round(2).addSeparators()}")
    }


    /**
     * Calculate dollar value. This is based on gems. The amount of fame, divided by 200 gems, multiplied against price (with creator discount) for 675 gems.
     *
     * @param fameAmount
     * @return
     */
    private fun calculateDollarValue(fameAmount: Long): Double {
        return (fameAmount / 200L) * (4.74 / 675)
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config
}
