package de.hype.bingonet.environment.packetconfig

import de.hype.bingonet.client.common.client.BingoNet
import java.awt.Color

object EnvironmentPacketConfig {
    const val enviroment: String = "Client"
    const val notEnviroment: String = "Server"
    const val apiVersion: Int = 1

    @JvmStatic
    val defaultWaypointColor: Color
        get() = BingoNet.visualConfig.waypointDefaultColor

    @JvmStatic
    val waypointDefaultWithTracer: Boolean
        get() = BingoNet.visualConfig.waypointDefaultWithTracer

    @JvmStatic
    val selfUsername: String
        get() = BingoNet.generalConfig.getUsername()
}
