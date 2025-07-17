package at.hannibal2.skyhanni.features.bingo.bingobrewers

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.utils.PlayerUtils
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import de.hype.bingonet.environment.packetconfig.PacketUtils.gson
import java.io.IOException

object BingoBrewersClient {
    private var client: Client? = null
    private var listener: Listener? = null

    init {
        if (SkyHanniMod.feature.event.bingo.bingoBrewers.useBB) init()
    }

    @Throws(IOException::class)
    private fun init() {
        client?.stop()
        client = Client(16384, 16384)
        listener = getListener()
        BingoBrewersPackets.registerPackets(client)
        client?.addListener(listener)
        client?.start()
        client?.connect(10000, "bingobrewers.com", 8282, 7070)

        val response = BingoBrewersPackets.ConnectionIgn()
        //IDK your server side indigo. I wanted to avoid issues on your side if I change anything since I dont have your code to look at. Otherwise I would have said sth like v0.3.7-compatible or sth.
        response.hello = "${PlayerUtils.getName()}|v0.3.7|Beta|${PlayerUtils.getUuid()}"
        println("Sending BingoBrewers Hello " + response.hello)
        client?.sendTCP(response)
    }


    private fun getListener(): Listener {
        return object : Listener() {
            override fun received(connection: Connection?, `object`: Any) {
                SkyHanniMod.launchCoroutine {
                    if (`object`.javaClass.`package`.name.contains("com.esotericsoftware.kryonet")) return@Runnable
                    if (`object` is BingoBrewersPackets.BingoBrewersPacket<*>) {
                        if (SkyHanniMod.feature.event.bingo.bingoBrewers.showPacketTraffic) println("BN Bingobrewrs: ${gson.toJson(`object`)}")
                        try {
                            val packet = `object`
                            packet.executeUnparsed(packet, client)
                        } catch (e: Exception) {
                            Chat.sendPrivateMessageToSelfError("Error handling a Packet from Bingo brewers. Please report this to BINGO NET")
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun disconnected(connection: Connection?) {
                reconnect()
            }
        }
    }

    fun stop() {
        client?.stop()
        client?.close()
    }

    fun reconnect() {
        var waitTime: Float
        var repeat: Boolean

        waitTime = ((3000 * Math.random()).toInt() + 2000).toFloat()

        repeat = true
        while (repeat) {
            try {
                println("Reconnecting to Bingo Brewers server...")
                init()
                repeat = false
            } catch (e: Exception) {
                client?.close()
                client?.removeListener(listener)
                try {
                    println("Reconnect failed. Trying again in $waitTime milliseconds.")
                    Thread.sleep(waitTime.toInt().toLong())
                } catch (ex: InterruptedException) {
                    throw RuntimeException(ex)
                }
                // keep reconnects under 45s between
                if (waitTime * 1.5 < 45000) {
                    waitTime *= 1.5f
                } else {
                    waitTime = (45000 - (5000 * Math.random() + 1000).toInt()).toFloat() // slightly vary time
                }
            }
        }
    }

    fun sendTCP(data: Any) {
        client?.sendTCP(data)
    }

    @JvmStatic
    fun load() {
        // This will call the init method to set up the client as needed.
    }
}
