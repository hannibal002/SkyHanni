package at.hannibal2.skyhanni.features.misc.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.achievements.AchievementManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object EstimatedItemValueTooltip {

    private const val ITEM_VALUE_ACHIEVEMENT = "Hefty Item"
    private val config get() = SkyHanniMod.feature.inventory.estimatedItemValues.showTooltip

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Hefty Item".asComponent(),
            "Have an item worth more than 500mil".asComponent(),
            50f,
        )
        event.register(achievement, ITEM_VALUE_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltip(event: ToolTipTextEvent) {
        // this is a tad stupid but i couldn't think of a "faster" way to do it
        if (!config && AchievementManager.isCompleted(ITEM_VALUE_ACHIEVEMENT)) return
        event.itemStack.getInternalNameOrNull() ?: return

        val total = EstimatedItemValueCalculator.getTotalPrice(event.itemStack, ignoreBasePrice = true) ?: return
        if (total > 500_000_000) {
            AchievementManager.completeAchievement(ITEM_VALUE_ACHIEVEMENT)
        }

        if (!config) return
        event.toolTip.add("§e§lEstimated Value: §6§l${total.addSeparators()} coins")
    }
}
