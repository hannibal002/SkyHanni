package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

// Note: Each particle is copied anywhere between 1-3 times. Different each time. Shouldn't affect using this for debugging or developing
object CopyNearbyParticlesCommand {
    private var searchRadius = 0
    private var saveNextTick = false
    private var searchTime: Long = 0
    private val resultList = mutableListOf<String>()
    private var tickTime: Long = 0
    private var counter = 0

    fun command(args: Array<String>) {
        searchRadius = 10
        if (args.size == 1) {
            searchRadius = args[0].toInt()
        }
        saveNextTick = true
        searchTime = System.currentTimeMillis()
        resultList.clear()
        counter = 0
        tickTime = 0L
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!saveNextTick) return
        // command was sent in or around a tick so skipping the tick
        if (System.currentTimeMillis() <= searchTime + 5) return

        if (resultList.isEmpty() && tickTime == 0L) tickTime = System.currentTimeMillis()

        if (System.currentTimeMillis() > tickTime + 30) {
            if (counter == 0) LorenzUtils.chat("No particles found nearby, try a larger search radius") else {
                val string = resultList.joinToString("\n")
                OSUtils.copyToClipboard(string)
                LorenzUtils.chat("$counter particles copied into the clipboard!")
            }
            saveNextTick = false
            return
        }

        val packet = event.packet
        if (packet is S2APacketParticles) {
            val location = packet.toLorenzVec().round(2)
            if (LocationUtils.playerLocation().distance(location) > searchRadius) return
            val offset = LorenzVec(packet.xOffset, packet.yOffset, packet.zOffset).round(2)
            resultList.add("particle type: ${packet.particleType}")
            resultList.add("particle location: $location")
            resultList.add("distance from player: ${LocationUtils.playerLocation().distance(location).round(2)}")
            resultList.add("particle offset: $offset")
            resultList.add("is long distance: ${packet.isLongDistance}")
            resultList.add("particle count: ${packet.particleCount}")
            resultList.add("particle speed: ${packet.particleSpeed}")
            resultList.add("particle arguments: ${packet.particleArgs.asList()}")
            resultList.add("")
            resultList.add("")
            counter ++
        }
    }
}
