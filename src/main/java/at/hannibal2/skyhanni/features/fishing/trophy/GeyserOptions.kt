package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.SpecialColour
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class GeyserOptions {
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.geyserOptions

    private var bobber: EntityFishHook? = null
    private var geyser: LorenzVec? = null

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onPacket(event: ReceiveParticleEvent) {
        if (!shouldProcessParticles()) return

        if (event.type != EnumParticleTypes.CLOUD) return
        geyser = event.location

        if (isHideParticlesEnabled() && geyser != null && bobber != null) {
            hideGeyserParticles(event)
        }
    }

    @SubscribeEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        bobber = event.bobber
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (shouldDrawBoundingBox()) {
            val newGeyser = geyser ?: return
            val geyserBox = AxisAlignedBB(
                newGeyser.x - 1, 118.0 - 0.1, newGeyser.z - 1,
                newGeyser.x + 1, 118.0 - 0.09, newGeyser.z + 1
            )
            val special = SkyHanniMod.feature.fishing.trophyFishing.geyserOptions.geyserBoxColor
            val color = Color(SpecialColour.specialToChromaRGB(special), true)
            event.drawFilledBoundingBox_nea(geyserBox, color)
        }
    }

    private fun hideGeyserParticles(event: ReceiveParticleEvent) {
        val newBobber = bobber ?: return
        val newGeyser = geyser ?: return

        if (newBobber.distanceTo(event.location) < 3 && newBobber.distanceTo(newGeyser) < 3) {
            event.isCanceled = true
        }
    }

    private fun shouldProcessParticles() = IslandType.CRIMSON_ISLE.isInIsland() && (isHideParticlesEnabled() || shouldDrawBoundingBox())

    private fun shouldDrawBoundingBox() = config.drawGeyserBoundingBox

    private fun isHideParticlesEnabled() = config.hideGeyserParticles
}
