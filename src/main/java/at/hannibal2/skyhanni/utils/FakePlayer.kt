package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.scoreboard.ScorePlayerTeam

class FakePlayer : EntityOtherPlayerMP(MinecraftCompat.localWorld, MinecraftCompat.localPlayer.gameProfile) {

    override fun getLocationSkin() =
        MinecraftCompat.localPlayer.locationSkin ?: DefaultPlayerSkin.getDefaultSkin(MinecraftCompat.localPlayer.uniqueID)

    override fun getTeam() = object : ScorePlayerTeam(null, null) {
        override fun getNameTagVisibility() = EnumVisible.NEVER
    }

    override fun isWearing(part: EnumPlayerModelParts): Boolean =
        MinecraftCompat.localPlayer.isWearing(part) && part != EnumPlayerModelParts.CAPE
}
