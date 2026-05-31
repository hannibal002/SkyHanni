package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.world.entity.Entity

/**
 * This is a compatibility layer that helps with multiple minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
@SkyHanniModule
object MinecraftCompat {

    val localPlayer get(): LocalPlayer = localPlayerOrNull ?: ErrorManager.skyHanniError("player is null")

    val localPlayerOrNull get(): LocalPlayer? = Minecraft.getInstance().player

    val Entity?.isLocalPlayer get(): Boolean = this == localPlayerOrNull && this != null

    @JvmStatic
    val localPlayerExists get(): Boolean = localPlayerOrNull != null

    val localWorld get(): ClientLevel = localWorldOrNull ?: ErrorManager.skyHanniError("level is null")

    val localWorldOrNull get(): ClientLevel? = Minecraft.getInstance().level

    @JvmStatic
    val localWorldExists get(): Boolean = localWorldOrNull != null

    val showDebugHud get(): Boolean = Minecraft.getInstance().debugEntries.isOverlayVisible

    //~ if < 26.1 'defaultClockTime' -> 'dayTime'
    val clientTime get(): Long = localWorldOrNull?.defaultClockTime ?: 0L

    @JvmStatic
    var serverTime: Long = 0L
        private set

    @HandleEvent
    internal fun onPacketReceived(event: PacketReceivedEvent) {
        val packet = event.packet as? ClientboundSetTimePacket ?: return
        //? if >= 26.1 {
        val defaultClock = localWorldOrNull?.dimensionType()?.defaultClock()?.orElse(null) ?: return
        serverTime = packet.clockUpdates[defaultClock]?.totalTicks() ?: serverTime
        //?} else {
        /*serverTime = packet.dayTime
        *///?}
    }
}
