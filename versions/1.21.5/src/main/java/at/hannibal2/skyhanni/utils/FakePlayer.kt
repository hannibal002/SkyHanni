package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.client.util.SkinTextures
import net.minecraft.entity.player.PlayerModelPart
import net.minecraft.scoreboard.Team

class FakePlayer : OtherClientPlayerEntity(MinecraftCompat.localWorld, MinecraftCompat.localPlayer.gameProfile) {

    override fun getSkinTextures(): SkinTextures =
        MinecraftCompat.localPlayer.skinTextures ?: DefaultSkinHelper.getSkinTextures(MinecraftCompat.localPlayer.uuid)

    override fun getScoreboardTeam() = object : Team(null, "") {
        override fun getNameTagVisibilityRule() = VisibilityRule.NEVER
    }

    override fun isPartVisible(part: PlayerModelPart): Boolean =
        MinecraftCompat.localPlayer.isPartVisible(part) && part != PlayerModelPart.CAPE
}
