package at.hannibal2.skyhanni.features.misc.reminders

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object ReminderHudDisplay {

    private val config get() = SkyHanniMod.feature.misc.reminders
    private val storage get() = SkyHanniMod.feature.storage.reminders

    private var display: Renderable? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        val reminders = storage.values.sortedBy { it.remindAt }
        if (reminders.isEmpty()) {
            display = null
            return
        }
        val lines = reminders.map { reminder ->
            val timeUntil = reminder.remindAt.timeUntil()
            val timeText = if (timeUntil.isNegative()) "§cNow!" else "§e${timeUntil.formatForHud()}"
            Renderable.text("$timeText §7— §6${reminder.reason}")
        }
        display = Renderable.vertical(lines, spacing = 1)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val display = display ?: return
        config.hudPosition.renderRenderable(display, posLabel = "Reminders HUD")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.showHud

    private fun Duration.formatForHud(): String = when {
        this >= TimeUnit.YEAR.factor.milliseconds -> format(biggestUnit = TimeUnit.YEAR, maxUnits = 2)
        this >= 7.days -> format(biggestUnit = TimeUnit.DAY, maxUnits = 1)
        this >= 1.days -> format(biggestUnit = TimeUnit.DAY, maxUnits = 2)
        this >= 1.hours -> format(biggestUnit = TimeUnit.HOUR, maxUnits = 2)
        this >= 1.minutes -> format(biggestUnit = TimeUnit.MINUTE, maxUnits = 2)
        else -> format(biggestUnit = TimeUnit.SECOND, maxUnits = 1)
    }
}
