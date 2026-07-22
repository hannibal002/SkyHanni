package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.combat.DeployableConfig.WarningType
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
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DeployableReminder {
    private val config get() = SkyHanniMod.feature.combat.deployable
    private val warningDelay get() = config.warningDelay.seconds

    private data class ActiveWarning(
        val type: WarningType,
        val renderable: Renderable,
        val startTime: SimpleTimeMark = SimpleTimeMark.now(),
    )

    // Prevents duplicate warnings from being scheduled/shown.
    private val activeWarnings = mutableListOf<ActiveWarning>()
    private val scheduledWarnings = mutableSetOf<WarningType>()

    @HandleEvent
    fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        if (!isEnabled()) return
        if (event.state != SlayerApi.ActiveQuestState.BOSS_FIGHT) return

        val warningType = WarningType.SLAYER
        val deployableType = getActiveDeployableType(warningType) ?: return

        scheduleWarning(
            warningType = warningType,
            message = "Place Down Power Orb!",
            type = deployableType,
            condition = { SlayerApi.isInBossFight() },
        )
    }

    @HandleEvent(onlyOnIsland = MINESHAFT)
    fun onIslandJoin() {
        if (!isEnabled()) return

        val warningType = WarningType.MINESHAFT
        val deployableType = getActiveDeployableType(warningType) ?: return

        scheduleWarning(
            warningType = warningType,
            message = "Place Down Lantern!",
            type = deployableType,
            condition = { IslandType.MINESHAFT.isInIsland() },
        )
    }

    @HandleEvent
    fun onTrophyFishCaught() {
        if (!isEnabled()) return
        if (!FishingApi.isTrophyFishing()) return

        val warningType = WarningType.TROPHY_FISHING
        val deployableType = getActiveDeployableType(warningType) ?: return

        showWarning(
            warningType = warningType,
            message = "Place Down Umberella!",
            type = deployableType,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        activeWarnings.removeIf { it.startTime.passedSince() > 3.seconds }
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
        type: DeployableType,
    ) {
        if (activeWarnings.any { it.type == warningType }) return
        if (DeployableDisplay.getActiveDeployables().any { it.type == type }) return

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
        type: DeployableType,
        condition: () -> Boolean = { true },
    ) {
        if (!scheduledWarnings.add(warningType)) return

        DelayedRun.runDelayed(warningDelay) {
            scheduledWarnings.remove(warningType)

            if (!isEnabled()) return@runDelayed
            if (!condition()) return@runDelayed

            showWarning(
                warningType = warningType,
                message = message,
                type = type,
            )
        }
    }

    private fun getActiveDeployableType(type: WarningType): DeployableType? =
        type.deployableType.takeIf { config.warningTypes.contains(type) }

    private fun isEnabled() = config.warnMissingDeployable
}
