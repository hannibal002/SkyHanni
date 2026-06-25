package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ServerTimeMark

/**
 * Fired at the end of every client tick, as long as both a local player and local world are present.
 *
 * Fired on the main client thread via the Fabric `ClientTickEvents.END_CLIENT_TICK` callback.
 *
 * This is the standard event for client-side periodic logic. Use [isMod] to run code every
 * Nth tick, or [SecondPassedEvent] for per-second logic. Avoid [repeatSeconds] unless
 * absolutely necessary.
 *
 * Unlike [ServerTickEvent], this event fires on the main thread at the client tick rate,
 * which can differ from the server tick rate under lag. Do not use this for server-synchronized timing.
 *
 * @param tick total number of client ticks elapsed since the mod was loaded
 * @see ServerTickEvent
 * @see SecondPassedEvent
 */
@PrimaryFunction("onTick")
class SkyHanniTickEvent(private val tick: Int) : SkyHanniEvent() {

    fun isMod(i: Int, offset: Int = 0) = (tick + offset) % i == 0

    /**
     * Use of this method is discouraged, use [SecondPassedEvent] instead.
     * Only use if absolutely necessary.
     */
    fun repeatSeconds(i: Int, offset: Int = 0) = isMod(i * 20, offset)
}

/**
 * Fired once per server-side game tick, triggered by incoming `ClientboundPingPacket` packets.
 *
 * **Fired on the Netty network thread. This event is asynchronous.**
 * Handlers must not access or modify game state without proper thread synchronization.
 *
 * Unlike [SkyHanniTickEvent], this event tracks server-side timing and is independent of the
 * client tick rate. Use this when server-synchronized timing is required, for example to
 * count server ticks accurately regardless of client lag.
 * For most client-side periodic logic, prefer [SkyHanniTickEvent] or [SecondPassedEvent] instead.
 *
 * @param tick monotonically increasing counter of server ticks received since the session started
 * @see SkyHanniTickEvent
 * @see SecondPassedEvent
 */
@PrimaryFunction("onServerTick")
class ServerTickEvent(val tick: Long) : SkyHanniEvent() {
    val timeMark = ServerTimeMark(tick)
}
