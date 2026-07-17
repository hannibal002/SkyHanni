package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import net.minecraft.client.User
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.world.entity.Entity

/**
 * This is a compatibility layer that helps with multiple Minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
@SkyHanniModule
object MinecraftCompat {
    // <editor-fold desc="World">
    /**
     * Returns the active [ClientLevel] or throws an exception if it doesn't exist.
     *
     * Prefer [localWorldOrNull]. Only use this in situations where you're confident that the world has to exist.
     *
     * Do not use `if (localWorldExists) { localWorldOrThrow }`. Instead, use `localWorldOrNull?.let { ... }`.
     */
    val localWorldOrThrow get(): ClientLevel = localWorldOrNull ?: error("level is null")

    /**
     * Returns the active [ClientLevel] or null if it doesn't exist.
     */
    val localWorldOrNull get(): ClientLevel? = Minecraft.getInstance().level

    /**
     * Returns whether there is an active [ClientLevel].
     *
     * Do not use `if (localWorldExists) { localWorldOrThrow }`. Instead, use `localWorldOrNull?.let { ... }`.
     */
    @JvmStatic
    val localWorldExists get(): Boolean = localWorldOrNull != null
    // </editor-fold>


    // <editor-fold desc="User">
    /**
     * The local user's information, such as the username and UUID.
     * This is always non-null, even if the player is not in a world / singleplayer.
     */
    val localUser get(): User = Minecraft.getInstance().user
    // </editor-fold>


    // <editor-fold desc="Player">
    /**
     * Returns the active [LocalPlayer] or throws an exception if it doesn't exist.
     *
     * Prefer [localPlayerOrNull]. Only use this in situations where you're confident that the player has to exist.
     *
     * Do not use `if (localPlayerExists) { localPlayerOrThrow }`. Instead, use `localPlayerOrNull?.let { ... }`.
     */
    val localPlayerOrThrow get(): LocalPlayer = localPlayerOrNull ?: error("player is null")

    /**
     * Returns the active [LocalPlayer] or null if it doesn't exist.
     */
    val localPlayerOrNull get(): LocalPlayer? = Minecraft.getInstance().player

    /**
     * Returns whether there is an active [LocalPlayer].
     *
     * Do not use `if (localPlayerExists) { localPlayerOrThrow }`. Instead, use `localPlayerOrNull?.let { ... }`.
     */
    @JvmStatic
    val localPlayerExists get(): Boolean = localPlayerOrNull != null

    /**
     * Returns whether the specified [Entity] is a [LocalPlayer].
     */
    val Entity?.isLocalPlayer get(): Boolean = this is LocalPlayer
    // </editor-fold>


    // <editor-fold desc="World Time">
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
    // </editor-fold>


    val hideGui get(): Boolean = Minecraft.getInstance().options.hideGui

    val showDebugHud get(): Boolean = Minecraft.getInstance().debugEntries.isOverlayVisible
}
