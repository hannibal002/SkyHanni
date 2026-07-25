package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color

@SkyHanniModule
object PestHighlighter {

    private val config get() = PestApi.config
    private val highlightColor = Color.RED

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (!config.pestHighlight) return
        if (event.type !== RenderEntityOutlineEvent.Type.NO_XRAY) return

        event.queueEntitiesToOutline { entity ->
            if (entity is ArmorStand && PestEntityResolver.getPestType(entity) != null) highlightColor else null
        }
    }
}
