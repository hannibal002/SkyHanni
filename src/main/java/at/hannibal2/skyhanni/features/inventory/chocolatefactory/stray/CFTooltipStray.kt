package at.hannibal2.skyhanni.features.inventory.chocolatefactory.stray

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object CFTooltipStray {
    private val config get() = CFApi.config

    /**
     * REGEX-TEST: You gained +2,465,018 Chocolate!
     * REGEX-TEST: gained +30,292 Chocolate!
     * REGEX-TEST: +36,330 Chocolate!
     * REGEX-TEST: Rabbit, so you received 55,935,257
     */
    private val chocolateGainedPattern by CFApi.patternGroup.pattern(
        "rabbit.stray.new",
        "(?:Rabbit, so )?(?:[Yy]ou )?(?:gained |received )?\\+?(?<amount>[\\d,]+)(?: Chocolate!)?",
    )

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onTooltip(event: ToolTipTextEvent) {
        if (!CFApi.inChocolateFactory) return
        if (!config.showStrayTime) return
        event.slot ?: return
        if (event.slot.index > 26 || event.slot.index == CFApi.infoIndex) return

        val tooltip = event.toolTip
        chocolateGainedPattern.firstMatcher(tooltip.map { it.string }) {
            val amount = group("amount").formatLong()
            val format = CFApi.timeUntilNeed(amount + 1).format(maxUnits = 2)
            tooltip.add("§a+§b$format §aof production".asComponent())
        }
    }
}
