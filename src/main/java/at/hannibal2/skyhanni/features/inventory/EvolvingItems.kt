package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getSecondsHeld
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.collection.CollectionUtils.addOrInsert
import kotlin.time.Duration.Companion.seconds

@HanniModule
object EvolvingItems {
    private val config get() = HanniMod.feature.inventory.evolvingItems

    @HandleEvent(onlyOnSkyblock = true)
    fun onToolTip(event: ToolTipEvent) {
        if (!config.timeHeldInLore && !config.timeLeftInLore) return

        val secondsHeld = event.itemStack.getSecondsHeld() ?: return
        val timeHeldFormatted = secondsHeld.seconds.format(maxUnits = 3)
        val timeLeftFormatted = (300 * 60 * 60 - secondsHeld).seconds.format(maxUnits = 3)
        // All the current ones take 300 hours. If any in the future need a different amount, this will need to be changed.

        if (config.timeLeftInLore) event.toolTip.addOrInsert(10, "§7Time Left: §b$timeLeftFormatted")
        if (config.timeHeldInLore) event.toolTip.addOrInsert(10, "§7Time Held: §b$timeHeldFormatted")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(86, "inventory.timeHeldInLore", "inventory.evolvingItems.timeHeldInLore")
    }
}
