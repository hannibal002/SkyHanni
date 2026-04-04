package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
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

    private var display: Renderable? = null
    private var entityId: Int = 0

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onEntityCustomNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        if (!config.firePillarDisplay) return
        val seconds = entityNamePattern.matchGroup(event.newName ?: return, "seconds") ?: return
        entityId = event.entity.id
        display = Renderable.text("§cFire Pillar: §b${seconds}s")
    }

    @HandleEvent
    fun onEntityRemoved(event: EntityRemovedEvent<ArmorStand>) {
        if (event.entity.id == entityId) display = null
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onGuiRender(event: GuiRenderEvent) {
        if (!config.firePillarDisplay) return

        val display = display ?: return
        config.firePillarDisplayPosition.renderRenderable(display, posLabel = "Fire Pillar")
    }
}
