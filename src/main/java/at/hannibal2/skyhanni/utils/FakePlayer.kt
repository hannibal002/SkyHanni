package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.mojang.authlib.GameProfile
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.ResourceLocation

class FakePlayer(uuid: String? = null) : EntityOtherPlayerMP(MinecraftCompat.localWorld, MinecraftCompat.localPlayer.gameProfile) {

    init {
        if (uuid != null) setPlayerUUID(uuid)
    }

    override fun getLocationSkin(): ResourceLocation? {
        return customSkin
            ?: MinecraftCompat.localPlayer.locationSkin
            ?: DefaultPlayerSkin.getDefaultSkin(MinecraftCompat.localPlayer.uniqueID)
    }

    override fun getLocationCape(): ResourceLocation? {
        return customCape ?: super.locationCape
    }

    private var customSkin: ResourceLocation? = null
    fun setSkin(texture: ResourceLocation?) {
        customSkin = texture
    }

    private var customCape: ResourceLocation? = null
    fun setCape(texture: ResourceLocation?) {
        customCape = texture
    }

    private var customGameProfile: GameProfile? = null
    fun setGameProfile(gameProfile: GameProfile?) {
        customGameProfile = gameProfile
    }
    override fun getGameProfile(): GameProfile? {
        return customGameProfile ?: super.gameProfile
    }

    private var customSkinType: String? = null
    fun setSkinType(type: String?) {
        customSkinType = type ?: "default"
    }

    override fun getSkinType(): String? {
        return customSkinType ?: super.skinType
    }

    // its the 1.21 name so itll work fineeee
    fun getNameForScoreboard(): String? = customGameProfile?.name ?: super.name

    override fun getTeam() = object : ScorePlayerTeam(null, null) {
        override fun getNameTagVisibility() = EnumVisible.NEVER
    }

    override fun isWearing(part: EnumPlayerModelParts): Boolean {
        if (customSkin == null) MinecraftCompat.localPlayer.isWearing(part) && part != EnumPlayerModelParts.CAPE
        return true
    }

}
