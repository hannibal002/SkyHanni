package at.hannibal2.hanni.features.slayer.blaze

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object FirePillarDisplay {

    private val config get() = SlayerApi.config.blazes

    /**
     * REGEX-TEST: §6§l2s §c§l8 hits
     */
    private val entityNamePattern by RepoPattern.pattern(
        "slayer.blaze.firepillar.entityname",
        "§6§l(?<seconds>.*)s §c§l8 hits",
    )

    private var display = ""

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return

        val entityNames = EntityUtils.getEntities<EntityArmorStand>().map { it.name }
        val seconds = entityNamePattern.firstMatcher(entityNames) { group("seconds") }
        display = seconds?.let { "§cFire Pillar: §b${it}s" }.orEmpty()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        config.firePillarDisplayPosition.renderString(display, posLabel = "Fire Pillar")
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isCurrent() && config.firePillarDisplay
}
