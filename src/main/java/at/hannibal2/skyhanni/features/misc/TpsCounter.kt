package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.TimeAndSizeLimitedCache
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.roundedUpSeconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TpsCounter {

    private val config get() = SkyHanniMod.feature.gui

    private val WORLD_SWITCH_DELAY = 5.seconds

    private var lastServerTick = ServerTimeMark.farPast()
    private var lastError = SimpleTimeMark.farPast()
    private val tpsList = TimeAndSizeLimitedCache<Long, Long>(100, 5.seconds)
    val tps: Double?
        get() = when {
            timeSinceWorldSwitch < WORLD_SWITCH_DELAY -> null
            tpsList.isEmpty() -> 0.0
            else -> (1000.0 / tpsList.values.average()).coerceIn(0.0..20.0).also {
                if (!it.isFinite()) printError(it)
            }
        }

    private var display: Renderable? = null

    private val timeSinceWorldSwitch get() = SkyBlockUtils.lastWorldSwitch.passedSince()
    private var pendingTpsCommand = false

    @HandleEvent
    fun onServerTick(event: ServerTickEvent) {
        val now = event.timeMark
        if (!lastServerTick.isFarPast()) {
            tpsList[event.tick] = (now - lastServerTick).inWholeMilliseconds
        }
        lastServerTick = now
    }

    @HandleEvent
    fun onSecondPassed() {
        if (lastServerTick.passedSince() >= 1.seconds) {
            ChatUtils.debug("No server ticks detected for 1 second, clearing TPS data")
            tpsList.clear()
        }

        updateDisplay()

        if (pendingTpsCommand) {
            pendingTpsCommand = false
            tpsCommand()
        }
    }

    private fun getTpsString(compact: Boolean = false): String = buildString {
        append("§eTPS: ")
        when (val currentTps = tps) {
            null -> {
                val remaining = (WORLD_SWITCH_DELAY - timeSinceWorldSwitch).roundedUpSeconds
                if (!compact) append("§fCalculating... ")
                append("§7(${remaining}s)")
            }
            else -> {
                append("%s%.1f".format(getColor(currentTps), currentTps))
            }
        }
    }

    private fun updateDisplay() {
        display = Renderable.text(getTpsString(compact = true))
    }

    private fun tpsCommand() {
        val text = getTpsString()
        ChatUtils.chat(text)

        val remaining = (WORLD_SWITCH_DELAY - timeSinceWorldSwitch)
        if (remaining.isPositive()) {
            DelayedRun.runDelayed(remaining) {
                pendingTpsCommand = true
            }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        tpsList.clear()
        display = null
        lastServerTick = ServerTimeMark.farPast()
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isEnabled()) return
        display?.let { config.tpsDisplayPosition.renderRenderable(it, posLabel = "TPS Display") }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtps") {
            description = "Informs in chat about the server ticks per second (TPS)."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { tpsCommand() }
        }
    }

    private fun isEnabled() = SkyBlockUtils.onHypixel && config.tpsDisplay &&
        (SkyBlockUtils.inSkyBlock || OutsideSBFeature.TPS_DISPLAY.isSelected())

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.tpsDisplayEnabled", "gui.tpsDisplay")
        event.move(2, "misc.tpsDisplayPosition", "gui.tpsDisplayPosition")
    }

    private fun getColor(tps: Double) = when {
        tps > 19.8 -> "§2"
        tps > 19 -> "§a"
        tps > 17.5 -> "§6"
        tps > 12 -> "§c"

        else -> "§4"
    }

    private fun printError(tps: Double) {
        if (lastError.passedSince() < 5.seconds) return
        lastError = SimpleTimeMark.now()
        ErrorManager.logErrorStateWithData(
            "TPS calculation got an error",
            "tps is $tps",
            "tps" to tps,
            "tpsList" to tpsList,
            "timeSinceWorldSwitch" to timeSinceWorldSwitch,
        )
    }
}
