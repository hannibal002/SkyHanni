package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.client.resources.PlayerSkin
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.scores.PlayerTeam

//#if MC > 1.21.8
//$$ import net.minecraft.core.ClientAsset
//$$ import net.minecraft.world.entity.player.PlayerModelType
//#endif

class FakePlayer(val hannibal: Boolean = false) : RemotePlayer(MinecraftCompat.localWorld, MinecraftCompat.localPlayer.gameProfile) {

    //#if MC < 1.21.9
    private val hannibalSkin = PlayerSkin(ResourceLocation.parse("skyhanni:hannibal2.png"), null, null, null, null, false)
    //#else
    //$$ private val hannibalSkin = PlayerSkin(ClientAsset.DownloadedTexture(ResourceLocation.parse("skyhanni:hannibal2.png"), ""), null, null , PlayerModelType.WIDE, false)
    //#endif

    override fun getSkin(): PlayerSkin {
        if (hannibal) return hannibalSkin
        return MinecraftCompat.localPlayer.skin
            ?: DefaultPlayerSkin.get(MinecraftCompat.localPlayer.uuid)
    }

    override fun getTeam() = object : PlayerTeam(null, "") {
        override fun getNameTagVisibility() = Visibility.NEVER
    }

    //#if MC < 1.21.9
    override fun isModelPartShown(part: PlayerModelPart): Boolean =
        MinecraftCompat.localPlayer.isModelPartShown(part) && part != PlayerModelPart.CAPE
    //#else
    //$$ override fun isModelPartShown(part: PlayerModelPart): Boolean =
    //$$    MinecraftCompat.localPlayer.isModelPartShown(part) && part != PlayerModelPart.CAPE
    //#endif
}
