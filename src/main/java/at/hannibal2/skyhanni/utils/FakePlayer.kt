package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.client.resources.PlayerSkin
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.scores.PlayerTeam
//? < 1.21.9 {
import net.minecraft.client.player.RemotePlayer
//?} else {
/*import net.minecraft.client.Minecraft
import net.minecraft.client.entity.ClientMannequin
*///?}

//? < 1.21.9 {
class FakePlayer : RemotePlayer(MinecraftCompat.localWorld, MinecraftCompat.localPlayer.gameProfile) {
//?} else {
/*class FakePlayer : ClientMannequin(
    MinecraftCompat.localWorld,
    Minecraft.getInstance().playerSkinRenderCache(),
) {
*///?}
    override fun getSkin(): PlayerSkin {
        return MinecraftCompat.localPlayer.skin ?: DefaultPlayerSkin.get(MinecraftCompat.localPlayer.uuid)
    }

    override fun getTeam() = object : PlayerTeam(null, "") {
        override fun getNameTagVisibility() = Visibility.NEVER
    }

    //? < 1.21.9 {
    override fun isModelPartShown(part: PlayerModelPart): Boolean =
        MinecraftCompat.localPlayer.isModelPartShown(part) && part != PlayerModelPart.CAPE
    //?} else {
    /*override fun isModelPartShown(part: PlayerModelPart): Boolean =
        MinecraftCompat.localPlayer.isModelPartShown(part) && part != PlayerModelPart.CAPE
    *///?}
}
