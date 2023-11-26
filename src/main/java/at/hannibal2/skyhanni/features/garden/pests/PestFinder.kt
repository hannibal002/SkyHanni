package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.events.DamageIndicatorDeathEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.name
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.pests
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.sendTeleportTo
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PestFinder {

    private val config get() = PestAPI.config.pestFinder

    private var display = emptyList<Renderable>()

    @SubscribeEvent
    fun onPestSpawn(event: PestSpawnEvent) {
        if (!isEnabled()) return
        PestSpawnTimer.lastSpawnTime = SimpleTimeMark.now()
        val plot = GardenPlotAPI.getPlotByName(event.plotName)
        if (plot == null) {
            LorenzUtils.userError("Open Desk to load plot names and pest locations!")
            return
        }
        plot.pests += event.amountPests
        update()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Configure Plots") return

        val pestInventoryPattern = "§4§lൠ §cThis plot has §6(?<amount>\\d) Pests?§c!".toPattern()

        for (plot in GardenPlotAPI.plots) {
            plot.pests = 0
            val item = event.inventoryItems[plot.inventorySlot] ?: continue
            for (line in item.getLore()) {
                pestInventoryPattern.matchMatcher(line) {
                    plot.pests = group("amount").formatNumber().toInt()
                }
            }
        }
        update()

    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val totalAmount = totalAmount()
        if (totalAmount == 0) {
            add(Renderable.string("§cNo pests detected."))
            add(Renderable.string("§7Open §eConfigure Plots Menu"))
            add(Renderable.string("§7when incorrect to reload."))
            return@buildList
        }

        add(Renderable.string("§eTotal pests on garden: §c${totalAmount()}§7/§c8"))

        for (plot in GardenPlotAPI.plots) {
            val pests = plot.pests
            if (pests == 0) continue

            val name = plot.name
            val pestsName = StringUtils.optionalPlural(pests, "pest", "pests")
            val renderable = Renderable.clickAndHover(
                "§c$pestsName §7in §b$name",
                listOf(
                    "§7Pests Found: §e$pests",
                    "§7In plot §b$name",
                    "",
                    "§eClick here to warp!"
                ),
                onClick = {
                    plot.sendTeleportTo()
                }
            )
            add(renderable)
        }
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        update()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (event.message == "§cThere are not any Pests on your Garden right now! Keep farming!") {
            GardenPlotAPI.plots.forEach {
                it.pests = 0
            }
            update()
        }
    }

    @SubscribeEvent
    fun onDamageIndicatorDeath(event: DamageIndicatorDeathEvent) {
        if (!isEnabled()) return

        // Check if an unknown damage indiactor mob dies in garden
        val type = event.data.bossType
        if (!PestType.entries.any { it.damageIndicatorBoss == type }) return

        val plot = GardenPlotAPI.getCurrentPlot()?.takeIf { it.pests > 0 } ?: run {
            LorenzUtils.userError("Could not detect the plot of the killed pest. Please Open the Configure Plots menu again.")
            return
        }

        plot.pests--
        update()
    }

    private fun totalAmount() = GardenPlotAPI.plots.sumOf { it.pests }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (config.onlyWithVacuum && !PestAPI.hasVacuumInHand()) return

        if (GardenAPI.inGarden() && config.showDisplay) {
            config.position.renderRenderables(display, posLabel = "Pest Finder")
        }
    }

    fun isEnabled() = GardenAPI.inGarden() && config.showDisplay
}
