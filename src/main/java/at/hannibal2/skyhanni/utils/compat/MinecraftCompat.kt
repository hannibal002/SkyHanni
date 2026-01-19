package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.Entity

object MinecraftCompat {

    val localPlayer get(): LocalPlayer = localPlayerOrNull ?: ErrorManager.skyHanniError("thePlayer is null")

    val localPlayerOrNull get(): LocalPlayer? = Minecraft.getInstance().player

    val Entity?.isLocalPlayer get(): Boolean = this == localPlayerOrNull && this != null

    val localPlayerExists get(): Boolean = localPlayerOrNull != null

    val localWorld get(): ClientLevel = localWorldOrNull ?: ErrorManager.skyHanniError("theWorld is null")

    val localWorldOrNull get(): ClientLevel? = Minecraft.getInstance().level

    val localWorldExists get(): Boolean = localWorldOrNull != null

    //? < 1.21.9 {
    val showDebugHud get(): Boolean = Minecraft.getInstance().debugOverlay.showDebugScreen()
    //?} else {
    /*val showDebugHud get(): Boolean = Minecraft.getInstance().debugEntries.isF3Visible
    *///?}
}
