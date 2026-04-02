package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object MovementSpeedDisplay {

    private val config get() = SkyHanniMod.feature.misc
    private var display: Renderable? = null

    /**
     * This speed value represents the movement speed in blocks per second.
     * This has nothing to do with the speed stat.
     */
    var bpsMoveSpeed = 0.0

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return

        bpsMoveSpeed = with(MinecraftCompat.localPlayer) {
            val oldPos = LorenzVec(xOld, yOld, zOld)
            val newPos = LorenzVec(position().x, position().y, position().z)

            // Distance from previous tick, multiplied by TPS
            oldPos.distance(newPos) * 20
        }

        if (!isEnabled()) return
        display = Renderable.text("Movement Speed: ${bpsMoveSpeed.roundTo(2)}")
    }

    @HandleEvent
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val display = display ?: return
        config.playerMovementSpeedPos.renderRenderable(display, posLabel = "Movement Speed")
    }

    fun sbEnabled() = SkyBlockUtils.inSkyBlock || OutsideSBFeature.MOVEMENT_SPEED.isSelected()
    fun isEnabled() = SkyBlockUtils.onHypixel && sbEnabled() && config.playerMovementSpeed
}
