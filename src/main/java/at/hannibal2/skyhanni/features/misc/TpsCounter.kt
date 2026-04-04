package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.features.misc.limbo.LimboTimeTracker
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.collection.SizeLimitedCache
import at.hannibal2.skyhanni.utils.inPartialMilliseconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.roundedUpSeconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TpsCounter {

    private val WORLD_SWITCH_DELAY = 5.seconds

    private val config get() = SkyHanniMod.feature.gui

    private val msPerTickList = SizeLimitedCache<Long, Double>(100)
    val rawTps: Double?
        get() = when {
            timeSinceWorldSwitch < WORLD_SWITCH_DELAY -> null
            msPerTickList.isEmpty() || lastServerTick.passedSince() >= 1.seconds -> 0.0
            else -> (1000.0 / msPerTickList.values.average()).coerceIn(0.0..20.0).also {
                if (!it.isFinite()) printError(it)
            }
        }
    val tps get() = rawTps?.let { if (TimeUtils.isAprilFoolsDay) it / 2 else it }

    private val timeSinceWorldSwitch get() = SkyBlockUtils.lastWorldSwitch.passedSince()

    private var display: Renderable? = null
    private var lastServerTick = SimpleTimeMark.farPast()
    private var lastError = SimpleTimeMark.farPast()
    private var pendingTpsCommand = false

    @HandleEvent
    fun onServerTick(event: ServerTickEvent) {
        val now = SimpleTimeMark.now()
        if (!lastServerTick.isFarPast()) {
            msPerTickList[event.tick] = (now - lastServerTick).inPartialMilliseconds
        }
        lastServerTick = now
    }

    @HandleEvent
    fun onSecondPassed() {
        display = Renderable.text(getTpsString(compact = true))

        if (pendingTpsCommand) {
            pendingTpsCommand = false
            tpsCommand()
        }
    }

    private fun getTpsString(compact: Boolean = false): String = buildString {
        append("§eTPS: ")
        val currentTps = tps
        when {
            LimboTimeTracker.inLimbo -> {
                append("§4N/A §7(Limbo)")
            }

            currentTps == null -> {
                val remaining = (WORLD_SWITCH_DELAY - timeSinceWorldSwitch).roundedUpSeconds
                if (!compact) append("§fCalculating... ")
                append("§7(${remaining}s)")
            }

            else -> {
                append("%s%.1f".format(getColor(currentTps), currentTps))
            }
        }
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
        msPerTickList.clear()
        display = null
        lastServerTick = SimpleTimeMark.farPast()
    }

    @HandleEvent
    fun onGuiRenderOverlay() {
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
            "msPerTickList" to msPerTickList,
            "timeSinceWorldSwitch" to timeSinceWorldSwitch,
        )
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("TPS Counter")
        event.addIrrelevant {
            add("TPS: %.1f".format(rawTps))
            add("Milliseconds Per Tick: ${msPerTickList.values.joinToString(", ") { "%.1f".format(it) }}")
            add("Time Since World Switch: $timeSinceWorldSwitch")
        }
    }
}
