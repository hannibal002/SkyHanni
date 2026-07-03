package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.User
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.world.entity.Entity

//? if >= 26.2
import net.minecraft.client.gui.Hud
//? else
//import net.minecraft.client.gui.Gui

/**
 * This is a compatibility layer that helps with multiple Minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
@SkyHanniModule
object MinecraftCompat {

    val localPlayer get(): LocalPlayer = localPlayerOrNull ?: ErrorManager.skyHanniError("player is null")

    val localPlayerOrNull get(): LocalPlayer? = Minecraft.getInstance().player

    /**
     * The local user's information, such as the username and UUID.
     * This is always non-null, even if the player is not in a world / singleplayer.
     */
    val localUser get(): User = Minecraft.getInstance().user

    val Entity?.isLocalPlayer get(): Boolean = this == localPlayerOrNull && this != null

    @JvmStatic
    val localPlayerExists get(): Boolean = localPlayerOrNull != null

    val localWorld get(): ClientLevel = localWorldOrNull ?: ErrorManager.skyHanniError("level is null")

    val localWorldOrNull get(): ClientLevel? = Minecraft.getInstance().level

    @JvmStatic
    val localWorldExists get(): Boolean = localWorldOrNull != null

    //? if >= 26.2
    val hud get(): Hud = Minecraft.getInstance().gui.hud
    //? else
    //val hud get(): Gui = Minecraft.getInstance().gui

    val hideGui get(): Boolean =
        //? if >= 26.2
        hud.isHidden()
        //? else
        //Minecraft.getInstance().options.hideGui

    val showDebugHud get(): Boolean = Minecraft.getInstance().debugEntries.isOverlayVisible

    //~ if < 26.1 'defaultClockTime' -> 'dayTime'
    val clientTime get(): Long = localWorldOrNull?.defaultClockTime ?: 0L

    @JvmStatic
    var serverTime: Long = 0L
        private set

    @JvmStatic
    var screen: Screen?
        //~ if >= 26.2 'screen' -> 'gui.screen()'
        get() = Minecraft.getInstance().gui.screen()
        set(value) {
            //~ if >= 26.2 'setScreen' -> 'gui.setScreen'
            Minecraft.getInstance().gui.setScreen(value)
        }

    @HandleEvent
    internal fun onPacketReceived(event: PacketReceivedEvent) {
        val packet = event.packet as? ClientboundSetTimePacket ?: return

        val defaultClock = localWorldOrNull?.dimensionType()?.defaultClock()?.orElse(null) ?: return
        serverTime = packet.clockUpdates[defaultClock]?.totalTicks() ?: serverTime
    }
}
