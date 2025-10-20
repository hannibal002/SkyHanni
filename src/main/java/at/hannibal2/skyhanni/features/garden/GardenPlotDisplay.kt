package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.currentSpray
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.getLowestIndexStatus
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.getPlotStatuses
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.icon
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isPestCountInaccurate
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.isPlayerInside
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.locked
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.name
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.pests
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.sendTeleportTo
import at.hannibal2.skyhanni.features.garden.pests.PestApi.getPestTypesInPlot
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.RenderableInventory.fakeInventory
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
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

        val renderables = GardenPlotApi.plots.map { plot ->
            val status = plot.getPlotStatuses().getLowestIndexStatus(config.displayedStatusTypes)
            val color = status?.highlightColor?.toColor() ?: Color.decode("#8a8d90")

            val displayPests = plot.pests >= 1 || plot.isPestCountInaccurate
            val displaySpray = plot.currentSpray != null

            val pestString = if (displayPests) "§2ൠ §cPests: ${if (plot.isPestCountInaccurate) "1+?" else plot.pests}" else ""
            val sprayString = if (displaySpray) getSprayString(plot) else ""
            val warpString = getWarpString(plot)

            Renderable.clickable(
                Renderable.drawInsideRoundedRect(
                    when (status) {
                        GardenPlotApi.PlotStatusType.PESTS -> createPestIcon(plot)
                        GardenPlotApi.PlotStatusType.SPRAYS -> plot.currentSpray?.type?.toInternalName()?.getItemStack()?.createCleanItem()
                        GardenPlotApi.PlotStatusType.LOCKED -> ItemStack(Blocks.wooden_button).createCleanItem()
                        GardenPlotApi.PlotStatusType.CURRENT,
                        GardenPlotApi.PlotStatusType.PASTING,
                        null,
                        -> Renderable.item(plot.icon, scale = 1.0, xSpacing = 0, ySpacing = 0)
                    } ?: Renderable.placeholder(16, 16),
                    color,
                    padding = 0,
                    radius = 0,
                ),
                tips = buildList {
                    add("§b${plot.name} §7- §e${plot.id}")
                    add("")
                    if (displayPests) add(pestString)
                    if (displaySpray) add(sprayString)
                    if (displayPests || displaySpray) add("")
                    add(warpString)
                },
                onLeftClick = { plot.sendTeleportTo() },
            )
        }

        display = Renderable.fakeInventory(renderables, 5)
    }

    private fun createPestIcon(plot: GardenPlotApi.Plot): Renderable {
        val pests = plot.getPestTypesInPlot()

        if (pests.isEmpty()) {
            return Renderable.doubleLayered(
                Renderable.placeholder(16, 16),
                StringRenderable("§2ൠ", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
            )
        }

        val total = pests.size
        val columns = when {
            total == 1 -> 1
            total <= 4 -> 2
            else -> 4
        }
        val scale = if (total == 1) 1.0 else if (total <= 4) 0.5 else 0.35

        val pestIcons = pests.chunked(columns).map { row ->
            row.map { pest -> Renderable.item(pest.internalName, scale, 0, 0) }
        }

        return Renderable.doubleLayered(
            Renderable.placeholder(16, 16),
            Renderable.table(pestIcons, xSpacing = 0),
        )
    }

    private fun ItemStack.createCleanItem() = Renderable.item(this, scale = 1.0, xSpacing = 0, ySpacing = 0)

    private fun getSprayString(plot: GardenPlotApi.Plot): String {
        return plot.currentSpray?.let { spray ->
            "§6Sprayed with §a${spray.type.displayName} §f${spray.expiry.timeUntil().format(TimeUnit.MINUTE)}"
        } ?: ""
    }

    private fun getWarpString(plot: GardenPlotApi.Plot): String {
        return when {
            plot.locked -> "§8Locked!"
            plot.isPlayerInside() -> "§aYou're here!"
            else -> "§eClick here to warp!"
        }
    }
}
