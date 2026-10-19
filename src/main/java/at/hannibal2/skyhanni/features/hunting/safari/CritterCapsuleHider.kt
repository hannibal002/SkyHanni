package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity

@SkyHanniModule
object CritterCapsuleHider {

    private val config get() = SkyHanniMod.feature.hunting.safari.critterCapsules

    private val CRITTER_CAPSULE = "CRITTER_CAPSULE".toInternalName()
    private val MASTERFUL_CRITTER_CAPSULE = "MASTERFUL_CRITTER_CAPSULE".toInternalName()

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onCheckRenderFlying(event: CheckRenderEntityEvent<Display.ItemDisplay>) {
        val mode = config.flyingMode
        if (mode == NEVER) return
        val isClose = event.isCloseToCamera()
        if (!isClose && mode != ALWAYS) return
        if (!event.entity.itemStack.canHideWhileFlying(isClose)) return
        event.cancel()
    }

    @HandleEvent(onlyOnIsland = SAFARI)
    private fun onCheckRenderOnGround(event: CheckRenderEntityEvent<ItemEntity>) {
        if (!config.hideOnGround) return
        if (event.entity.item.getInternalName() != CRITTER_CAPSULE) return
        event.cancel()
    }

    // The camera position is used instead of the player position because the capsule blocks the view at eye height.
    private fun CheckRenderEntityEvent<out Entity>.isCloseToCamera(): Boolean {
        val distance = config.closeDistance.toDouble()
        return entity.distanceToSqr(camX, camY, camZ) <= distance * distance
    }

    // Masterful capsules are rare enough that players want to watch them, so they only ever get hidden up close.
    private fun SafeItemStack.canHideWhileFlying(isClose: Boolean): Boolean = when (getInternalName()) {
        CRITTER_CAPSULE -> true
        MASTERFUL_CRITTER_CAPSULE -> isClose
        else -> false
    }
}
