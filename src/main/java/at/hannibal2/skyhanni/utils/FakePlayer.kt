package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.client.resources.PlayerSkin
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.resources.ResourceLocation
//#if MC > 1.21.8
//$$ import net.minecraft.entity.player.PlayerSkinType
//$$ import net.minecraft.util.AssetInfo
//#endif

class FakePlayer(val hannibal: Boolean = false) : RemotePlayer(MinecraftCompat.localWorld, MinecraftCompat.localPlayer.gameProfile) {

    //#if MC < 1.21.9
    private val hannibalSkin = PlayerSkin(ResourceLocation.parse("skyhanni:hannibal2.png"), null, null, null, null ,false)
    //#else
    //$$ private val hannibalSkin = SkinTextures(AssetInfo.SkinAssetInfo(Identifier.of("skyhanni:hannibal2.png"), ""), null, null ,PlayerSkinType.WIDE, false)
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
    //$$ override fun isModelPartVisible(part: PlayerModelPart): Boolean =
    //$$    MinecraftCompat.localPlayer.isModelPartVisible(part) && part != PlayerModelPart.CAPE
    //#endif
}
