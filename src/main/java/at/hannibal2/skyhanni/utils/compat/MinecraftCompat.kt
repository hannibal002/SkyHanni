package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.Entity

object MinecraftCompat {

    @Deprecated("Use localPlayerOrNull instead", ReplaceWith("localPlayerOrNull"))
    val localPlayer get(): LocalPlayer = localPlayerOrNull ?: error("player is null")

    val localPlayerOrNull get(): LocalPlayer? = Minecraft.getInstance().player

    val Entity?.isLocalPlayer get(): Boolean = this == localPlayerOrNull && this != null

    val localPlayerExists get(): Boolean = localPlayerOrNull != null

    @Deprecated("Use localWorldOrNull instead", ReplaceWith("localWorldOrNull"))
    val localWorld get(): ClientLevel = localWorldOrNull ?: error("level is null")

    val localWorldOrNull get(): ClientLevel? = Minecraft.getInstance().level

    val localWorldExists get(): Boolean = localWorldOrNull != null

    //~ if > 1.21.10 'isF3Visible' -> 'isOverlayVisible'
    val showDebugHud get(): Boolean = Minecraft.getInstance().debugEntries.isF3Visible
}
