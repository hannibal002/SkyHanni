package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkyBlockXPApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat

@SkyHanniModule
object SkyBlockXPBar {
    private val config get() = SkyHanniMod.feature.misc
    private var cache: OriginalValues? = null

    private class OriginalValues(val currentXP: Float, val maxXP: Int, val level: Int)

    @HandleEvent
    fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) {
        if (!isEnabled()) return
        if (event.type != RenderLayer.EXPERIENCE) return
        val (level, xp) = SkyBlockXPApi.levelXPPair ?: return

        with(MinecraftCompat.localPlayer) {
            cache = OriginalValues(experience, experienceTotal, experienceLevel)
            setXPStats(xp / 100f, 100, level)
        }
    }

    @HandleEvent
    fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type != RenderLayer.EXPERIENCE) return
        with(cache ?: return) {
            MinecraftCompat.localPlayer.setXPStats(currentXP, maxXP, level)
            cache = null
        }
    }

    private fun isEnabled() =
        SkyBlockUtils.inSkyBlock && !SkyBlockUtils.inAnyIsland(setOf(IslandType.THE_RIFT, IslandType.CATACOMBS)) && config.skyblockXpBar
}
