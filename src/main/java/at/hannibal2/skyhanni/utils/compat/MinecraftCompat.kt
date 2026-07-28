package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import net.minecraft.client.Minecraft
import net.minecraft.client.User
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.world.entity.Entity

//? if >= 26.2 {
import net.minecraft.client.gui.Hud
//?} else {
/*import net.minecraft.client.gui.Gui
*///?}

/**
 * This is a compatibility layer that helps with multiple Minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
@SkyHanniModule
object MinecraftCompat {

    private val mc = Minecraft.getInstance()

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
    val localWorldOrNull get(): ClientLevel? = mc.level

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
    val localUser get(): User = mc.user
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
    val localPlayerOrNull get(): LocalPlayer? = mc.player

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

    // <editor-fold desc="Miscellaneous">
    @JvmStatic
    var screen: Screen?
        //~ if < 26.2 'gui.screen()' -> 'screen'
        get() = mc.gui.screen()
        set(value) {
            //~ if < 26.2 'gui.setScreen' -> 'setScreen'
            mc.gui.setScreen(value)
        }

    //? if >= 26.2 {
    val hud get(): Hud = mc.gui.hud
    //?} else {
    /*val hud get(): Gui = mc.gui
    *///?}

    val hideGui get(): Boolean =
        //? if >= 26.2 {
        hud.isHidden()
        //?} else {
        /*mc.options.hideGui
        *///?}

    val showDebugHud get(): Boolean = mc.debugEntries.isOverlayVisible

    fun reloadChunks() = DelayedRun.runOrNextTick {
        //~ if < 26.2 'levelExtractor' -> 'levelRenderer'
        mc.levelExtractor.allChanged()
    }
    // </editor-fold>
}
