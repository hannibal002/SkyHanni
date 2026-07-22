package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.combat.DeployableReminderConfig.WarningType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DeployableReminder {
    private val config get() = SkyHanniMod.feature.combat.deployable.deployableReminder
    private val warningDelay get() = config.warningDelay.seconds

    private data class ActiveWarning(
        val type: WarningType,
        val renderable: Renderable,
        val startTime: SimpleTimeMark = SimpleTimeMark.now(),
    )

    // Prevents duplicate warnings from being scheduled/shown.
    private val activeWarnings = mutableListOf<ActiveWarning>()
    private val scheduledWarnings = ConcurrentHashMap.newKeySet<WarningType>()

    @HandleEvent
    fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        if (!isActive(SLAYER)) return
        if (event.state != SlayerApi.ActiveQuestState.BOSS_FIGHT) return

        scheduleWarning(
            warningType = SLAYER,
            message = "Place Down Power Orb!",
            condition = { SlayerApi.isInBossFight() },
        )
    }

    @HandleEvent(onlyOnIsland = MINESHAFT)
    fun onIslandJoin() {
        if (!isActive(MINESHAFT)) return

        scheduleWarning(
            warningType = MINESHAFT,
            message = "Place Down Lantern!",
            condition = { IslandType.MINESHAFT.isInIsland() },
        )
    }

    @HandleEvent
    fun onTrophyFishCaught() {
        if (!isActive(TROPHY_FISHING)) return
        if (!FishingApi.isTrophyFishing()) return

        showWarning(
            warningType = TROPHY_FISHING,
            message = "Place Down Umberella!",
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        activeWarnings.removeIf { it.startTime.passedSince() > config.warningDuration.seconds }

        val renderables = activeWarnings.map { it.renderable }
        config.warningPosition.renderRenderables(
            renderables,
            posLabel = "Deployable Warning",
        )
    }

    @HandleEvent
    fun onWorldChange() {
        activeWarnings.clear()
        scheduledWarnings.clear()
    }

    private fun showWarning(
        warningType: WarningType,
        message: String,
    ) = DelayedRun.runOrNextTick {
        if (activeWarnings.any { it.type == warningType }) return@runOrNextTick
        if (DeployableDisplay.getActiveDeployables().any { it.type == warningType.deployableType }) return@runOrNextTick

        SoundUtils.playErrorSound()
        activeWarnings.add(
            ActiveWarning(
                type = warningType,
                renderable = Renderable.text("§4§l$message", scale = 2.0),
            )
        )
    }

    private fun scheduleWarning(
        warningType: WarningType,
        message: String,
        condition: () -> Boolean = { true },
    ) {
        if (!scheduledWarnings.add(warningType)) return

        DelayedRun.runDelayed(warningDelay) {
            scheduledWarnings.remove(warningType)

            if (!isActive(warningType)) return@runDelayed
            if (!condition()) return@runDelayed

            showWarning(
                warningType = warningType,
                message = message,
            )
        }
    }

    private fun isActive(warningType: WarningType) = isEnabled() && config.warningTypes.contains(warningType)
    private fun isEnabled() = config.enabled
}
