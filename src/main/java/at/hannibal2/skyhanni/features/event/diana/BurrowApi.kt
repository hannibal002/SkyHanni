package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.world.phys.Vec3

object BurrowApi {

    var lastBurrowRelatedChatMessage = SimpleTimeMark.farPast()
    var lastBurrowInteracted: Vec3? = null
        private set

    fun setBurrowInteracted(interacted: Vec3?) {
        GriffinBurrowHelper.addDebug("set last interacted burrow to $interacted")
        lastBurrowInteracted = interacted
    }
}
