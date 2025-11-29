package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.client.util.SkinTextures
import net.minecraft.entity.player.PlayerModelPart
import net.minecraft.scoreboard.Team
import net.minecraft.util.Identifier
//#if MC > 1.21.8
//$$ import net.minecraft.entity.player.PlayerSkinType
//$$ import net.minecraft.util.AssetInfo
//#endif

class FakePlayer(val hannibal: Boolean = false) : OtherClientPlayerEntity(MinecraftCompat.localWorld, MinecraftCompat.localPlayer.gameProfile) {

    //#if MC < 1.21.9
    private val hannibalSkin = SkinTextures(Identifier.of("skyhanni:hannibal2.png"), null, null, null, null ,false)
    //#else
    //$$ private val hannibalSkin = SkinTextures(AssetInfo.SkinAssetInfo(Identifier.of("skyhanni:hannibal2.png"), ""), null, null ,PlayerSkinType.WIDE, false)
    //#endif

    override fun getSkinTextures(): SkinTextures {
        if (hannibal) return hannibalSkin
        return MinecraftCompat.localPlayer.skinTextures
            ?: DefaultSkinHelper.getSkinTextures(MinecraftCompat.localPlayer.uuid)
    }

    override fun getScoreboardTeam() = object : Team(null, "") {
        override fun getNameTagVisibilityRule() = VisibilityRule.NEVER
    }

    //#if MC < 1.21.9
    override fun isPartVisible(part: PlayerModelPart): Boolean =
        MinecraftCompat.localPlayer.isPartVisible(part) && part != PlayerModelPart.CAPE
    //#else
    //$$ override fun isModelPartVisible(part: PlayerModelPart): Boolean =
    //$$    MinecraftCompat.localPlayer.isModelPartVisible(part) && part != PlayerModelPart.CAPE
    //#endif
}
