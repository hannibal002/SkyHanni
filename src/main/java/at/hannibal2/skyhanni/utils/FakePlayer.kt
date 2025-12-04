package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.ResourceLocation

class FakePlayer(val hannibal: Boolean = false) : EntityOtherPlayerMP(MinecraftCompat.localWorld, MinecraftCompat.localPlayer.gameProfile) {

    private val hannibalSkin = ResourceLocation("skyhanni:hannibal2.png")

    override fun getLocationSkin(): ResourceLocation? {
        if (hannibal) return hannibalSkin
        return MinecraftCompat.localPlayer.locationSkin
            ?: DefaultPlayerSkin.getDefaultSkin(MinecraftCompat.localPlayer.uniqueID)
    }

    override fun getTeam() = object : ScorePlayerTeam(null, null) {
        override fun getNameTagVisibility() = EnumVisible.NEVER
    }

    override fun isWearing(part: EnumPlayerModelParts): Boolean =
        MinecraftCompat.localPlayer.isWearing(part) && part != EnumPlayerModelParts.CAPE
}
