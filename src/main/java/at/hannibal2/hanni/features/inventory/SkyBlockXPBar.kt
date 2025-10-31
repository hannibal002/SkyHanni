package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.SkyBlockXPApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.api.minecraftevents.RenderLayer
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.hanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.compat.MinecraftCompat

@HanniModule
object SkyBlockXPBar {
    private val config get() = HanniMod.feature.misc
    private var cache: OriginalValues? = null

    private class OriginalValues(val currentXP: Float, val maxXP: Int, val level: Int)

    @HandleEvent
    fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) {
        if (!isEnabled()) return
        if (event.type != RenderLayer.EXPERIENCE_BAR) return
        val (level, xp) = SkyBlockXPApi.levelXPPair ?: return

        with(MinecraftCompat.localPlayer) {
            cache = OriginalValues(experience, experienceTotal, experienceLevel)
            setXPStats(xp / 100f, 100, level)
        }
    }

    @HandleEvent
    fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type != RenderLayer.EXPERIENCE_BAR) return
        with(cache ?: return) {
            MinecraftCompat.localPlayer.setXPStats(currentXP, maxXP, level)
            cache = null
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(95, "misc.skyblockXpBar", "misc.skyblockXPBar")
    }

    private fun isEnabled() =
        SkyBlockUtils.inSkyBlock && !SkyBlockUtils.inAnyIsland(setOf(IslandType.THE_RIFT, IslandType.CATACOMBS)) && config.skyblockXPBar
}
