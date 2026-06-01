package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.Entity

/**
 * This is a compatibility layer that helps with multiple minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
object MinecraftCompat {

    val localPlayer get(): LocalPlayer = localPlayerOrNull ?: ErrorManager.skyHanniError("player is null")

    val localPlayerOrNull get(): LocalPlayer? = Minecraft.getInstance().player

    val Entity?.isLocalPlayer get(): Boolean = this == localPlayerOrNull && this != null

    val localPlayerExists get(): Boolean = localPlayerOrNull != null

    val localWorld get(): ClientLevel = localWorldOrNull ?: ErrorManager.skyHanniError("level is null")

    val localWorldOrNull get(): ClientLevel? = Minecraft.getInstance().level

    val localWorldExists get(): Boolean = localWorldOrNull != null

    val showDebugHud get(): Boolean = Minecraft.getInstance().debugEntries.isOverlayVisible
}
