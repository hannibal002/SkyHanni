package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object PestHighlighter {

    private val config get() = PestApi.config.pestHighlights

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (!config.pestHighlight) return
        if (event.type !== RenderEntityOutlineEvent.Type.NO_XRAY) return
        val highlightColor = config.highlightColor.toColor()

        event.queueEntitiesToOutline { entity ->
            if (entity is ArmorStand && PestEntityResolver.getPestType(entity) != null) highlightColor else null
        }
    }
}
