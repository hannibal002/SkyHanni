package at.hannibal2.hanni.utils.compat

import at.hannibal2.hanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity

object MinecraftCompat {

    val localPlayer get(): EntityPlayerSP = localPlayerOrNull ?: ErrorManager.hanniError("thePlayer is null")

    val localPlayerOrNull get(): EntityPlayerSP? = Minecraft.getMinecraft().thePlayer

    val Entity?.isLocalPlayer get(): Boolean = this == localPlayerOrNull && this != null

    val localPlayerExists get(): Boolean = localPlayerOrNull != null

    val localWorld get(): WorldClient = localWorldOrNull ?: ErrorManager.hanniError("theWorld is null")

    val localWorldOrNull get(): WorldClient? = Minecraft.getMinecraft().theWorld

    val localWorldExists get(): Boolean = localWorldOrNull != null

    //#if MC < 1.16
    val showDebugHud get(): Boolean = Minecraft.getMinecraft().gameSettings.showDebugInfo
    //#else
    //$$ val showDebugHud get(): Boolean = MinecraftClient.getInstance().debugHud.shouldShowDebugHud()
    //#endif
}
