package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrInsert
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getSecondsHeld
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HeldTimeInLore {
    private val config get() = SkyHanniMod.feature.inventory

    @HandleEvent(onlyOnSkyblock = true)
    fun onToolTip(event: ToolTipEvent) {
        if (!config.timeHeldInLore && !config.timeLeftInLore) return

        val secondsHeld = event.itemStack.getSecondsHeld() ?: return
        val timeHeldFormatted = secondsHeld.seconds.format(maxUnits = 3)
        val timeLeftFormatted = (300 * 60 * 60 - secondsHeld).seconds.format(maxUnits = 3)
        // All the current ones take 300 hours. If any in the future need a different amount, this will need to be changed.

        if (config.timeHeldInLore) event.toolTip.addOrInsert(10, "§7Time Held: §b$timeHeldFormatted")
        if (config.timeLeftInLore) event.toolTip.addOrInsert(10, "§7Time Left: §b$timeLeftFormatted")
    }
}
