package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity

@SkyHanniModule
object CritterCapsuleHider {

    private val config get() = SkyHanniMod.feature.hunting.safari.critterCapsules

    private val capsuleInternalNames = setOf("CRITTER_CAPSULE", "MASTERFUL_CRITTER_CAPSULE").toInternalNames()

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onCheckRenderFlying(event: CheckRenderEntityEvent<Display.ItemDisplay>) {
        val mode = config.flyingMode
        if (mode == NEVER) return
        if (mode == WHEN_CLOSE && !event.isCloseToCamera()) return
        if (event.entity.itemStack.isCritterCapsule()) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onCheckRenderOnGround(event: CheckRenderEntityEvent<ItemEntity>) {
        if (!config.hideOnGround) return
        if (event.entity.item.isCritterCapsule()) {
            event.cancel()
        }
    }

    // The camera position is used instead of the player position because the capsule blocks the view at eye height.
    private fun CheckRenderEntityEvent<out Entity>.isCloseToCamera(): Boolean {
        val distance = config.closeDistance.toDouble()
        return entity.distanceToSqr(camX, camY, camZ) <= distance * distance
    }

    private fun SafeItemStack.isCritterCapsule(): Boolean = getInternalName() in capsuleInternalNames
}
