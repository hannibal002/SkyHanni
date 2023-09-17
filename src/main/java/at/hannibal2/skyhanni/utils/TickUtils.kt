package at.hannibal2.skyhanni.utils

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * Used for consistent tick rates for use in [ChromaShader][at.hannibal2.skyhanni.features.chroma.ChromaShader]
 * so chroma speed is consistent
 */
object TickUtils {

    private val anchor = Any()

    @Volatile
    private var totalTicks = 0

    @Synchronized
    fun getTotalTicks() = totalTicks

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) {
            synchronized (anchor) {
                totalTicks++
            }
        }
    }
}