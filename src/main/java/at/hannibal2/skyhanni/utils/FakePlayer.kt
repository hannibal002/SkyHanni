package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.ClientMannequin
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard

class FakePlayer : ClientMannequin(
    MinecraftCompat.localWorld,
    Minecraft.getInstance().playerSkinRenderCache(),
) {
    override fun getSkin(): PlayerSkin = MinecraftCompat.localPlayer.skin

    override fun getTeam() = object : PlayerTeam(Scoreboard(), "") {
        override fun getNameTagVisibility() = Visibility.NEVER
    }

    override fun isModelPartShown(part: PlayerModelPart): Boolean =
        MinecraftCompat.localPlayer.isModelPartShown(part) && part != PlayerModelPart.CAPE
}
