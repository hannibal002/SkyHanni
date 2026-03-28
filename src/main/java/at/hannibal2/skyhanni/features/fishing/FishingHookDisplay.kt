package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberCastEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object FishingHookDisplay {

    private val config get() = SkyHanniMod.feature.fishing.fishingHookDisplay
    private var armorStand: ArmorStand? = null
    private val potentialArmorStands = mutableListOf<ArmorStand>()
    // Todo repo pattern?
    private val pattern = "§e§l(\\d+(\\.\\d+)?)".toPattern()
    private var isRendering = false

    @HandleEvent
    fun onWorldChange() = reset()

    @HandleEvent
    fun onBobberThrow(event: FishingBobberCastEvent) = reset()

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!isEnabled()) return

        if (armorStand == null) {
            val filter = potentialArmorStands.filter { it.hasCustomName() && it.hasCorrectName() }
            if (filter.size == 1) {
                armorStand = filter[0]
            }
        }
    }

    private fun reset() {
        potentialArmorStands.clear()
        armorStand = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onJoinWorld(event: EntityEnterWorldEvent<ArmorStand>) {
        if (!isEnabled()) return
        potentialArmorStands.add(event.entity)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<ArmorStand>) {
        if (!isEnabled()) return
        if (!config.hideArmorStand) return
        if (!isRendering) return

        if (event.entity == armorStand) {
            event.cancel()
        }
    }

    // TODO add a cache instead of re-calculating every frame
    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        isRendering = false

        val armorStand = armorStand ?: return
        if (armorStand.deceased) {
            reset()
            return
        }
        if (!armorStand.hasCustomName() || !armorStand.isCustomNameVisible) return
        val alertText = Renderable.text(
            if (armorStand.name.string == "!!!") config.customAlertText.replace("&", "§")
            else armorStand.name.formattedTextCompatLessResets(),
        )

        isRendering = true
        config.position.renderRenderable(alertText, posLabel = "Fishing Hook Display")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(72, "fishing.fishingHookDisplay.position", Position::migrate)
    }

    private fun ArmorStand.hasCorrectName(): Boolean =
        (name.string == "!!!") || pattern.matcher(name.formattedTextCompatLessResets()).matches()

    fun isEnabled() = config.enabled && FishingApi.holdingRod
}
