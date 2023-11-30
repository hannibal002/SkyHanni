package at.hannibal2.skyhanni.features.fishing.tracker

import at.hannibal2.skyhanni.events.EntityMoveEvent
import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FishingProfitPlayerMoving {

    private val lastSteps = mutableListOf<Double>()
    var isMoving = true

    @SubscribeEvent
    fun onEntityMove(event: EntityMoveEvent) {
        if (!FishingProfitTracker.isEnabled() || !FishingProfitTracker.config.hideMoving) return
        if (event.entity != Minecraft.getMinecraft().thePlayer) return

        val distance = event.newLocation.distanceIgnoreY(event.oldLocation)
        if (distance < 0.1) {
            lastSteps.clear()
            return
        }
        lastSteps.add(distance)
        if (lastSteps.size > 20) {
            lastSteps.removeAt(0)
        }
        val total = lastSteps.sum()
        if (total > 3) {
            isMoving = true
        }
    }

    @SubscribeEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        isMoving = false
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        isMoving = true
    }
}
