package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity

object MinecraftCompat {

    val localPlayer get(): ClientPlayerEntity = localPlayerOrNull ?: ErrorManager.skyHanniError("thePlayer is null")

    val localPlayerOrNull get(): ClientPlayerEntity? = MinecraftClient.getInstance().player

    val Entity?.isLocalPlayer get(): Boolean = this == localPlayerOrNull && this != null

    val localPlayerExists get(): Boolean = localPlayerOrNull != null

    val localWorld get(): ClientWorld = localWorldOrNull ?: ErrorManager.skyHanniError("theWorld is null")

    val localWorldOrNull get(): ClientWorld? = MinecraftClient.getInstance().world

    val localWorldExists get(): Boolean = localWorldOrNull != null

    //#if MC < 1.16
    //$$ val showDebugHud get(): Boolean = Minecraft.getMinecraft().gameSettings.showDebugInfo
    //#else
    val showDebugHud get(): Boolean = MinecraftClient.getInstance().debugHud.shouldShowDebugHud()
    //#endif
}
