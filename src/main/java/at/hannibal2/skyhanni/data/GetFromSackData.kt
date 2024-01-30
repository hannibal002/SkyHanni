package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import java.util.Queue
import kotlin.time.Duration.Companion.seconds

object GetFromSackData {

    private val minimumDelay = 1.0.seconds

    private val queue: Queue<PrimitiveItemStack> = LinkedList()

    private var lastTimeOfCommand = SimpleTimeMark.farPast()

    /** Do not use this use [GetFromSackAPI.get] instead*/
    fun addToQueue(items: List<PrimitiveItemStack>) = queue.addAll(items)

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (queue.isNotEmpty() && lastTimeOfCommand.passedSince() >= minimumDelay) {
            val item = queue.poll()
            lastTimeOfCommand = LorenzUtils.timeWhenNewQueuedUpCommandExecutes
            LorenzUtils.sendCommandToServer("gfs ${item.name.asString()} ${item.amount}")
        }
    }
}
