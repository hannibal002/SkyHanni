package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
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

class GeyserFishing {
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.geyserOptions

    private var bobber: EntityFishHook? = null
    private var geyser: LorenzVec? = null
    private var geyserBox: AxisAlignedBB? = null

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!shouldProcessParticles()) return
        if (event.type != EnumParticleTypes.CLOUD || event.count != 15 || event.speed != 0.05f || event.offset != LorenzVec(0.1f, 0.6f, 0.1f)) return

        geyser = event.location
        val potentialGeyser = geyser ?: return

        geyserBox = AxisAlignedBB(
            potentialGeyser.x - 2, 118.0 - 0.1, potentialGeyser.z - 2,
            potentialGeyser.x + 2, 118.0 - 0.09, potentialGeyser.z + 2
        )

        if (config.hideParticles && bobber != null) {
            hideGeyserParticles(event)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        bobber = null
        geyser = null
        geyserBox = null
    }

    @SubscribeEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        bobber = event.bobber
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!shouldProcessParticles()) return
        val bobber = bobber ?: return
        if (bobber.isDead) {
            this.bobber = null
            return
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!config.drawBox || !IslandType.CRIMSON_ISLE.isInIsland()) return
        val geyserBox = geyserBox ?: return
        val color = Color(SpecialColour.specialToChromaRGB(config.boxColor), true)
        event.drawFilledBoundingBox_nea(geyserBox, color)
    }

    private fun hideGeyserParticles(event: ReceiveParticleEvent) {
        val bobber = bobber ?: return
        val geyser = geyser ?: return

        if (bobber.distanceTo(event.location) < 3 && bobber.distanceTo(geyser) < 3) {
            event.isCanceled = true
        }
    }

    private fun shouldProcessParticles() = IslandType.CRIMSON_ISLE.isInIsland() && LorenzUtils.skyBlockArea == "Blazing Volcano" && (config.hideParticles || config.drawBox)
}
