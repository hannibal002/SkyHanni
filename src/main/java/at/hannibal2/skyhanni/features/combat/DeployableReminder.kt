package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.combat.DeployableConfig.WarningType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
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

    private var warningActiveTime = SimpleTimeMark.farPast()
    private var display: Renderable? = null

    @HandleEvent
    fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        if (!isEnabled()) return
        if (event.state != SlayerApi.ActiveQuestState.BOSS_FIGHT) return
        val deployableType = getActiveDeployableType(WarningType.SLAYER) ?: return
        DelayedRun.runDelayed(config.warningDelay.seconds) {
            showWarning("Place Down Power Orb!", deployableType)
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (!isEnabled()) return
        if (event.newIsland != IslandType.MINESHAFT) return
        val deployableType = getActiveDeployableType(WarningType.MINESHAFT) ?: return
        DelayedRun.runDelayed(config.warningDelay.seconds) {
            if (!IslandType.MINESHAFT.isInIsland()) return@runDelayed
            showWarning("Place Down Power Orb!", deployableType)
        }
    }

    @HandleEvent
    fun onTrophyFishCaught() {
        if (!isEnabled()) return
        val deployableType = getActiveDeployableType(WarningType.TROPHY_FISHING) ?: return
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
        config.warningPosition.renderRenderable(activeDisplay, posLabel = "Gummy Warning")
    }

    private fun showWarning(message: String, type: DeployableType) {
        if (DeployableDisplay.getActiveDeployables().any { it.type == type }) return
        SoundUtils.playErrorSound()
        display = Renderable.text("§4§l$message", scale = 2.0)
        warningActiveTime = SimpleTimeMark.now()
    }

    private fun getActiveDeployableType(type: WarningType): DeployableType? {
        if (!config.warningTypes.contains(type)) return null
        return type.deployableType
    }

    private fun isEnabled() = config.warnMissingDeployable
}
