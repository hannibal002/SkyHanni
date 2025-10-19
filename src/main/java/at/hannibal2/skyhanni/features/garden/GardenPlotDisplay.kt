package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.getLowestIndexStatus
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.getPlotStatuses
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.sendTeleportTo
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import java.awt.Color

@SkyHanniModule
object GardenPlotDisplay {

    private val config get() = SkyHanniMod.feature.garden.plotDisplay

    private var display: Renderable? = null

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { config.enabled },
            onlyOnIsland = IslandType.GARDEN,
            onRender = {
                config.displayPos.renderRenderable(display, "Garden Plot Display")
            },
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.enabled) return

        display = Renderable.drawInsideRoundedRect(
            Renderable.vertical(spacing = 2) {
                GardenPlotApi.plots.chunked(5).forEach { plotGroup ->
                    val renderables = plotGroup.map { plot ->
                        val color = plot.getPlotStatuses().getLowestIndexStatus(config.displayedStatusTypes)?.highlightColor?.toColor()
                            ?: Color.decode("#202020")

                        Renderable.drawInsideRoundedRect(
                            Renderable.clickable(
                                Renderable.placeholder(25, 25),
                                tips = listOf("§eClick here to warp!"),
                                onLeftClick = { plot.sendTeleportTo() },
                            ),
                            color,
                            radius = 0,
                        )
                    }

                    add(Renderable.horizontal(renderables, spacing = 2))
                }
            },
            Color.WHITE,
            radius = 5,
        )
    }
}
