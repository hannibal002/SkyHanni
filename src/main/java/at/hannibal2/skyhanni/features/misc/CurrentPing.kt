package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.hypixelapi.HypixelEventApi
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPingPacket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CurrentPing {

    private var lastPingRequested = SimpleTimeMark.farPast()
    private val previousPings = mutableListOf<Duration>()
    private var waitingForPacket = false
    var averagePing = Duration.ZERO

    fun onPingPacket(packet: ClientboundPingPacket) {
        waitingForPacket = false

        if (previousPings.size > 10) {
            previousPings.dropLast(1)
        }
        previousPings.add(SimpleTimeMark.now() - lastPingRequested)
        averagePing = (previousPings.sumOf { it.inWholeMilliseconds } / previousPings.size).milliseconds
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(10)) return
        if (lastPingRequested.passedSince() > 20.seconds) waitingForPacket = false
        requestPing()
    }

    private fun requestPing() {
        if (waitingForPacket || !LorenzUtils.onHypixel) return
        lastPingRequested = SimpleTimeMark.now()
        waitingForPacket = true
        HypixelEventApi.sendPacket(ServerboundPingPacket())
    }
}
