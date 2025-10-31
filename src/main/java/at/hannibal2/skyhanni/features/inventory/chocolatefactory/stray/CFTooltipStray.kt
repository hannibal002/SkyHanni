package at.hannibal2.hanni.features.inventory.chocolatefactory.stray

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.formatLong
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.TimeUtils.format

@HanniModule
object CFTooltipStray {
    private val config get() = CFApi.config

    /**
     * REGEX-TEST: §5§o§7You gained §6+2,465,018 Chocolate§7!
     * REGEX-TEST: §5§o§7gained §6+30,292 Chocolate§7!
     * REGEX-TEST: §5§o§7§6+36,330 Chocolate§7!
     * REGEX-TEST: §5§o§9Rabbit§7, so you received §655,935,257
     */
    private val chocolateGainedPattern by CFApi.patternGroup.pattern(
        "rabbit.stray",
        "(?:§.)*(?:Rabbit§7, so )?(?:[Yy]ou )?(?:gained |received )?§6\\+?(?<amount>[\\d,]+)(?: Chocolate§7!)?",
    )

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onTooltip(event: ToolTipEvent) {
        if (!CFApi.inChocolateFactory) return
        if (!config.showStrayTime) return
        if (event.slot.slotNumber > 26 || event.slot.slotNumber == CFApi.infoIndex) return

        val tooltip = event.toolTip
        chocolateGainedPattern.firstMatcher(tooltip) {
            val amount = group("amount").formatLong()
            val format = CFApi.timeUntilNeed(amount + 1).format(maxUnits = 2)
            tooltip[tooltip.lastIndex] += " §7(§a+§b$format §aof production§7)"
        }
    }
}
