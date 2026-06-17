package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.combat.DeployableConfig.WarningType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DeployableReminder {
    private val config get() = SkyHanniMod.feature.combat.deployable
    private val warningDelay get() = config.warningDelay.seconds

    private var warningActiveTime = SimpleTimeMark.farPast()
    private var display: Renderable? = null

    @HandleEvent
    fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        if (!isEnabled()) return
        if (event.state != SlayerApi.ActiveQuestState.BOSS_FIGHT) return
        val deployableType = getActiveDeployableType(WarningType.SLAYER) ?: return
        scheduleWarning(
            message = "Place Down Power Orb!",
            type = deployableType,
            condition = { SlayerApi.isInBossFight() }
        )
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (!isEnabled()) return
        if (event.newIsland != IslandType.MINESHAFT) return
        val deployableType = getActiveDeployableType(WarningType.MINESHAFT) ?: return
        scheduleWarning(
            message = "Place Down Lantern!",
            type = deployableType,
            condition = { IslandType.MINESHAFT.isInIsland() }
        )
    }

    @HandleEvent
    fun onTrophyFishCaught() {
        if (!isEnabled()) return
        val deployableType = getActiveDeployableType(WarningType.TROPHY_FISHING) ?: return
        if (!FishingApi.isTrophyFishing()) return
        showWarning("Place Down Umbrella!", deployableType)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val activeDisplay = display ?: return
        if (warningActiveTime.passedSince() > 3.seconds) {
            display = null
            return
        }
        config.warningPosition.renderRenderable(activeDisplay, posLabel = "Deployable Warning")
    }

    @HandleEvent
    fun onWorldChange() {
        warningActiveTime = SimpleTimeMark.farPast()
        display = null
    }

    private fun showWarning(message: String, type: DeployableType) {
        if (DeployableDisplay.getActiveDeployables().any { it.type == type }) return
        SoundUtils.playErrorSound()
        display = Renderable.text("§4§l$message", scale = 2.0)
        warningActiveTime = SimpleTimeMark.now()
    }

    private fun scheduleWarning(
        message: String,
        type: DeployableType,
        condition: () -> Boolean = { true },
    ) {
        DelayedRun.runDelayed(warningDelay) {
            if (!isEnabled()) return@runDelayed
            if (!condition()) return@runDelayed
            showWarning(message, type)
        }
    }

    private fun getActiveDeployableType(type: WarningType): DeployableType? =
        type.deployableType.takeIf { config.warningTypes.contains(type) }

    private fun isEnabled() = config.warnMissingDeployable
}
