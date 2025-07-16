package at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.environment.packetconfig

import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.BNConnection
import com.google.gson.Gson
import de.hype.bingonet.environment.packetconfig.AbstractPacket
import java.lang.Error
import java.lang.Exception
import java.lang.RuntimeException
import java.util.ArrayList

object PacketUtils {
    val gson: Gson = CustomGson.createNotPrettyPrinting()

    fun parsePacketToJson(packet: AbstractPacket?): String {
        return gson.toJson(packet).replace("\n", "/n")
    }

    fun <T : AbstractPacket?> tryToProcessPacket(
        packet: Packet<T?>,
        rawJson: String
    ) {
        val clazz = packet.getClazz()
        val consumer = packet.getConsumer()
        val abstractPacket = gson.fromJson<T?>(rawJson.replace("/n", "\n"), clazz)
        if (handleIntercept<T?>(abstractPacket)) consumer.accept(abstractPacket)
    }

    private fun showError(t: Throwable, errorMessage: String?) {
        println(errorMessage + " because of: " + t.javaClass.getSimpleName() + ":  " + t.message)
        Error(errorMessage, t).printStackTrace()
    }

    fun <T : AbstractPacket?> getAsPacket(message: String, clazz: Class<T?>): T? {
        if (!message.contains(".")) return null
        val packetName = message.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        val rawJson = message.substring(packetName.length + 1)
        if (packetName != clazz.getSimpleName()) {
            try {
                val parsedPacket = gson.fromJson<T?>(rawJson.replace("/n", "\n"), clazz)
                return parsedPacket
            } catch (t: Throwable) {
                showError(
                    t,
                    "Could not process packet '" + packetName + "' from " + EnvironmentPacketConfig.notEnviroment
                )
            }
        }
        val errorMessage = "Could not process packet '" + packetName + "' from " + EnvironmentPacketConfig.notEnviroment

        showError(APIException("Found unknown packet: " + packetName + "'"), errorMessage)
        return null
    }

    fun isPacket(message: String, clazz: Class<out AbstractPacket?>): Boolean {
        if (!message.contains(".")) return false
        val packetName = message.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        if (packetName == clazz.getSimpleName()) {
            return true
        }
        return false
    }

    fun isPacket(message: String): Boolean {
        if (!message.contains(".")) return false
        val packetName = message.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        for (packetClass in BNPacketManager.Companion.getAllPacketClasses()) {
            if (packetName != packetClass.getSimpleName()) {
                return true
            }
        }
        return false
    }

    fun <T : AbstractPacket?> handleIfPacket(connection: BNConnection?, message: String): Boolean {
        //Return = is Packet
        if (!message.contains(".")) return false
        val packetName = message.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        val rawJson = message.substring(packetName.length + 1)
        val manager = BNPacketManager(connection)
        for (packet in manager.getPackets()) {
            if (packetName != packet.getClazz().getSimpleName()) continue
            try {
                if (BingoNet.developerConfig.isDetailedDevModeEnabled()) Chat.sendPrivateMessageToSelfDebug(packetName + ":" + rawJson)
                PacketUtils.tryToProcessPacket(packet, rawJson)
                return true
            } catch (e: RuntimeException) {
                throw e
            } catch (t: Exception) {
                showError(
                    t,
                    "Could not process packet '" + packetName + "' from " + EnvironmentPacketConfig.notEnviroment
                )
            }
        }
        val errorMessage = "Could not process packet '" + packetName + "' from " + EnvironmentPacketConfig.notEnviroment

        showError(APIException("Found unknown packet: " + packetName + "'"), errorMessage)
        return false
    }

    @Synchronized
    fun <T : AbstractPacket?> handleIntercept(packet: T?): Boolean {
        val packetClass: Class<T?>?
        try {
            packetClass = packet.javaClass as Class<T?>
        } catch (e: Exception) {
            return true
        }
        val intercepts: MutableList<InterceptPacketInfo<*>> = BingoNet.connection.packetIntercepts
        val indexes: MutableList<Int> = ArrayList<Int>()
        var cancelIntercept = false
        var cancelMainExec = false
        var found = false
        for (i in intercepts.indices) {
            val intercept: InterceptPacketInfo = intercepts.get(i)
            if (intercept.matches(packetClass)) {
                if (packet is ReplyPacket && packet.replyDate == intercept.getReplyId()) {
                    if (!intercept.getIgnoreIfIntercepted() || !found) {
                        found = true
                        if (intercept.getCancelPacket()) {
                            cancelMainExec = true
                        }
                        if (intercept.getBlockIntercepts()) {
                            cancelIntercept = true
                        }
                        if (intercept.getBlockExecutionForCompletion()) {
                            intercepts.remove(intercept)
                            intercept.run(packet)
                        } else {
                            intercepts.remove(intercept)
                            BingoNet.executionService.execute({ intercept.run(packet) })
                        }
                    }
                    if (cancelIntercept) break
                } else {
                    indexes.add(i)
                }
            }
        }
        if (!cancelIntercept) {
            for (index in indexes) {
                val intercept: InterceptPacketInfo = intercepts.get(index)
                if (intercept.matches(packetClass)) {
                    if (!intercept.getIgnoreIfIntercepted() || !found) {
                        found = true
                        if (intercept.getCancelPacket()) {
                            cancelMainExec = true
                        }
                        if (intercept.getBlockIntercepts()) {
                            cancelIntercept = true
                        }
                        if (intercept.getBlockExecutionForCompletion()) {
                            intercept.run(packet)
                        } else {
                            BingoNet.executionService.execute({ intercept.run(packet) })
                        }
                    }
                    if (cancelIntercept) break
                }
            }
        }

        return !cancelMainExec
    }

}
