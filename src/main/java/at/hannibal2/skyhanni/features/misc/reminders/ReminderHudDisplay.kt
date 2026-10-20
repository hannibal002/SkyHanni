package at.hannibal2.skyhanni.features.misc.reminders

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object ReminderHudDisplay {

    private val config get() = SkyHanniMod.feature.misc.reminders
    private val storage get() = SkyHanniMod.feature.storage.reminders

    private var display: Renderable? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.showHud) return
        val reminders = storage.values.sortedBy { it.remindAt }
        if (reminders.isEmpty()) {
            display = null
            return
        }
        val lines = reminders.map { reminder ->
            val timeUntil = reminder.remindAt.timeUntil()
            val timeText = if (timeUntil.isNegative()) "§cNow!" else "§e${timeUntil.format(maxUnits = 2)}"
            Renderable.text("$timeText §7— §6${reminder.reason}")
        }
        display = Renderable.vertical(lines, spacing = 1)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.showHud) return
        val display = display ?: return
        config.hudPosition.renderRenderable(display, posLabel = "Reminders HUD")
    }
}
