package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
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

    private var entityId: Int = 0

    @HandleEvent
    fun onEntityCustomNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        if (!isEnabled()) return
        val seconds = entityNamePattern.matchGroup(event.newName ?: return, "seconds") ?: return
        entityId = event.entity.id
        display = "§cFire Pillar: §b${seconds}s"
    }

    @HandleEvent
    fun onEntityRemoved(event: EntityRemovedEvent<ArmorStand>) {
        if (event.entity.id == entityId) display = ""
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        config.firePillarDisplayPosition.renderString(display, posLabel = "Fire Pillar")
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isCurrent() && config.firePillarDisplay
}
