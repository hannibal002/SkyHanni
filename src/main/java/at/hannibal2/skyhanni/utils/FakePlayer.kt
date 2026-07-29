package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.ClientMannequin
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard

class FakePlayer(val player: AbstractClientPlayer) : ClientMannequin(
    player.level(),
    Minecraft.getInstance().playerSkinRenderCache(),
) {
    override fun getSkin(): PlayerSkin = player.skin

    override fun getTeam() = object : PlayerTeam(Scoreboard(), "") {
        override fun getNameTagVisibility() = Visibility.NEVER
    }

    override fun isModelPartShown(part: PlayerModelPart): Boolean =
        player.isModelPartShown(part) && part != PlayerModelPart.CAPE

    companion object {
        fun fromLocalPlayer(): FakePlayer? = MinecraftCompat.localPlayerOrNull?.let { FakePlayer(it) }

        fun fromLocalPlayerOrThrow(): FakePlayer = fromLocalPlayer()
            ?: error("cannot create fake player because local player is null")
    }
}
