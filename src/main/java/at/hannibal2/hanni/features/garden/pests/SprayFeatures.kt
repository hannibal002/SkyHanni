package at.hannibal2.hanni.features.garden.pests

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.features.garden.GardenPlotApi
import at.hannibal2.hanni.features.garden.GardenPlotApi.renderPlot
import at.hannibal2.hanni.features.garden.pests.PestApi.getPests
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@HanniModule
object SprayFeatures {

    private val config get() = PestApi.config.spray

    private var display: String? = null
    private var lastChangeTime = SimpleTimeMark.farPast()

    private val changeMaterialPattern by RepoPattern.pattern(
        "garden.spray.material",
        "§a§lSPRAYONATOR! §r§7Your selected material is now §r§a(?<spray>.*)§r§7!",
    )

    private val SPRAYONATOR = "SPRAYONATOR".toInternalName()

    private fun SprayType?.getSprayEffect(): String =
        this?.getPests()?.takeIf { it.isNotEmpty() }?.let { pests ->
            pests.joinToString("§7, §6") { it.displayName }
        } ?: when (this) {
            SprayType.FINE_FLOUR -> "§6+20☘ Farming Fortune"
            else -> "§cUnknown Effect"
        }


    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return

        display = changeMaterialPattern.matchMatcher(event.message) {
            val sprayName = group("spray")
            val type = SprayType.getByNameOrNull(sprayName)
            val sprayEffect = type.getSprayEffect()
            "§a${type?.displayName ?: sprayName} §7(§6$sprayEffect§7)"
        } ?: return

        lastChangeTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val display = display ?: return

        if (lastChangeTime.passedSince() > 5.seconds) {
            this.display = null
            return
        }

        config.position.renderString(display, posLabel = "Pest Spray Selector")
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!config.drawPlotsBorderWhenInHands) return
        if (InventoryUtils.itemInHandId != SPRAYONATOR) return
        val plot = GardenPlotApi.getCurrentPlot() ?: return
        event.renderPlot(plot, LorenzColor.YELLOW.toColor(), LorenzColor.DARK_BLUE.toColor())
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.pestWhenSelector
}
