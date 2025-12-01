package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.mojang.authlib.GameProfile
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.client.util.SkinTextures
import net.minecraft.entity.player.PlayerModelPart
import net.minecraft.scoreboard.Team

class FakePlayer(uuid: String? = null) : OtherClientPlayerEntity(MinecraftCompat.localWorld, MinecraftCompat.localPlayer.gameProfile) {

    init {
        if (uuid != null) setPlayerUUID(uuid)
    }

    private var customSkinTextures: SkinTextures? = null

    override fun getSkinTextures(): SkinTextures {
        return customSkinTextures
            ?: MinecraftCompat.localPlayer.skinTextures
            ?: DefaultSkinHelper.getSkinTextures(MinecraftCompat.localPlayer.uuid)
    }

    override fun getScoreboardTeam() = object : Team(null, "") {
        override fun getNameTagVisibilityRule() = VisibilityRule.NEVER
    }

    fun setSkinTextures(skinTextures: SkinTextures?) {
        customSkinTextures = skinTextures
    }

    private var customGameProfile: GameProfile? = null
    fun setGameProfile(gameProfile: GameProfile?) {
        customGameProfile = gameProfile
    }
    override fun getGameProfile(): GameProfile? {
        return customGameProfile ?: super.gameProfile
    }

    //#if MC < 1.21.9
    override fun isPartVisible(part: PlayerModelPart): Boolean =
        MinecraftCompat.localPlayer.isPartVisible(part) && part != PlayerModelPart.CAPE
    //#else
    //$$ override fun isModelPartVisible(part: PlayerModelPart): Boolean =
    //$$    MinecraftCompat.localPlayer.isModelPartVisible(part) && part != PlayerModelPart.CAPE
    //#endif
}
