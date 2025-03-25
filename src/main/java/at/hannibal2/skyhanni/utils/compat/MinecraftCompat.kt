package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity

object MinecraftCompat {

    val localPlayer get(): EntityPlayerSP = localPlayerOrNull ?: ErrorManager.skyHanniError("thePlayer is null")

    val localPlayerOrNull get(): EntityPlayerSP? = Minecraft.getMinecraft().thePlayer

    val Entity?.isLocalPlayer get(): Boolean = this == localPlayerOrNull && this != null

    val localPlayerExists get(): Boolean = localPlayerOrNull != null

    val localWorld get(): WorldClient = localWorldOrNull ?: ErrorManager.skyHanniError("theWorld is null")

    val localWorldOrNull get(): WorldClient? = Minecraft.getMinecraft().theWorld

    val localWorldExists get(): Boolean = localWorldOrNull != null
}
