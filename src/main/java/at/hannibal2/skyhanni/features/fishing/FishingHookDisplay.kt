package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.Vec3

@SkyHanniModule
object FishingHookDisplay {

    private val config get() = SkyHanniMod.feature.fishing.fishingHookDisplay

    private data class TimerEntity(
        val id: Int,
        val display: Renderable,
        val position: Vec3,
    )

    private var timerEntity: TimerEntity? = null

    /**
     * REGEX-TEST: 3.0
     * REGEX-TEST: 1.2
     * REGEX-TEST: !!!
     */
    private val fishingHookPattern by RepoPattern.pattern(
        "fishing.hook-display",
        "(?:§.)*?(?:(?<time>\\d+(?:\\.\\d+)?)|(?<alert>!!!))"
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
    private fun onEntityTextUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        if (!isEnabled()) return
        val newName = event.newName ?: return
        val bobber = FishingApi.bobber ?: return

        val displayText = fishingHookPattern.matchMatcher(newName) {
            if (groupOrNull("alert") != null) {
                config.customAlertText.replace("&", "§")
            } else {
                newName
            }
        } ?: return

        val position = event.entity.position()

        val current = timerEntity
        if (current != null && current.id != event.entity.id) {
            // Prefer the closest entity to the bobber if there ar multiple
            if (current.position.distanceTo(bobber.position()) < position.distanceTo(bobber.position())) {
                return
            }
        }

        timerEntity = TimerEntity(
            event.entity.id,
            Renderable.text(displayText),
            position,
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onEntityTextRemoved(event: EntityRemovedEvent<ArmorStand>) {
        if (event.entity.id == timerEntity?.id) {
            reset()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onEntityTextCheckRender(event: CheckRenderEntityEvent<ArmorStand>) {
        if (!isEnabled()) return
        if (!config.hideArmorStand) return
        if (!isRendering) return

        if (event.entity.id == timerEntity?.id) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        isRendering = false
        val timerEntity = timerEntity ?: return
        isRendering = true
        config.position.renderRenderable(timerEntity.display, posLabel = "Fishing Hook Display")
    }

    private fun reset() {
        timerEntity = null
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(72, "fishing.fishingHookDisplay.position", Position::migrate)
    }

    fun isEnabled() = config.enabled && FishingApi.holdingRod
}
