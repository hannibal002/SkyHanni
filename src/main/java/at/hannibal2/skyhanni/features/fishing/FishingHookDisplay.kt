package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object FishingHookDisplay {

    private val config get() = SkyHanniMod.feature.fishing.fishingHookDisplay

    private data class TimerDisplay(
        val id: Int,
        val renderable: Renderable,
        val position: LorenzVec,
    )

    private var timerDisplay: TimerDisplay? = null

    /**
     * REGEX-TEST: §e§l3.0
     * REGEX-TEST: §e§l1.2
     * REGEX-TEST: §c§l!!!
     * REGEX-TEST: !!!
     * REGEX-FAIL: §736
     * REGEX-FAIL: §772
     */
    private val timerPattern by RepoPattern.pattern(
        "fishing.hook.timer",
        "§e§l(?<time>\\d+(?:\\.\\d+)?)|(?:§.)*(?<alert>!!!)",
    )

    private var isRendering = false

    @HandleEvent
    private fun onWorldChange() {
        reset()
    }

    @HandleEvent
    private fun onBobberCast() {
        reset()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onEntityCustomNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        if (!isEnabled()) return
        val newName = event.newNameFormatted ?: return
        val bobber = FishingApi.bobber ?: return

        val displayText = timerPattern.matchMatcher(newName) {
            if (groupOrNull("alert") != null) {
                config.customAlertText.replace("&", "§")
            } else {
                newName
            }
        } ?: return

        val position = event.entity.getLorenzVec()

        val current = timerDisplay
        if (current != null && current.id != event.entity.id) {
            // Prefer the closest entity to the bobber if there are multiple
            val bobberPosition = bobber.getLorenzVec()
            if (current.position.distance(bobberPosition) < position.distance(bobberPosition)) return
        }

        timerDisplay = TimerDisplay(
            event.entity.id,
            Renderable.text(displayText),
            position,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onEntityRemoved(event: EntityRemovedEvent<ArmorStand>) {
        if (event.entity.id == timerDisplay?.id) {
            reset()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onCheckRender(event: CheckRenderEntityEvent<ArmorStand>) {
        if (!isEnabled()) return
        if (!config.hideArmorStand) return
        if (!isRendering) return

        if (event.entity.id == timerDisplay?.id) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        isRendering = false
        val timerDisplay = timerDisplay ?: return
        isRendering = true
        config.position.renderRenderable(timerDisplay.renderable, posLabel = "Fishing Hook Display")
    }

    private fun reset() {
        timerDisplay = null
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(72, "fishing.fishingHookDisplay.position", Position::migrate)
    }

    fun isEnabled() = config.enabled && FishingApi.holdingRod
}
