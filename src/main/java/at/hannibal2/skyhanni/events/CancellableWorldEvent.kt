package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.world.phys.Vec3

/**
 * This class is a placeholder for world-related events.
 */
abstract class CancellableWorldEvent : CancellableSkyHanniEvent() {
    abstract val location: Vec3
}
