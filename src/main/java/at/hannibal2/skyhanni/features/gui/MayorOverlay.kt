package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

enum class MayorOverlay(private val configLine: String, private val display: () -> Renderable) {
    MAYOR(
        "Mayor",
        {
            val mayorColor = MayorAPI.mayorNameToColorCode(MayorAPI.currentMayor?.mayorName ?: "")

            Renderable.verticalContainer(
                buildMap {
                    MayorAPI.currentMayor?.mayorName?.let { put("${mayorColor}Mayor $it", null) }
                    MayorAPI.currentMayor?.activePerks?.map { " §e${it.perkName}" to "§7${it.description}" }?.let { putAll(it) }
                }.map { pair ->
                    pair.value?.let {
                        Renderable.hoverTips(
                            Renderable.string(pair.key),
                            listOf(Renderable.wrappedString(it, 200)),
                        )
                    } ?: Renderable.string(pair.key)
                },
            )
        },
    ),
    MINISTER(
        "Minister",
        {
            val ministerColor = MayorAPI.mayorNameToColorCode(MayorAPI.currentMinister?.mayorName ?: "")

            Renderable.verticalContainer(
                buildMap {
                    MayorAPI.currentMinister?.mayorName?.let { put("${ministerColor}Minister $it", null) }
                    MayorAPI.currentMinister?.activePerks?.map { " §e${it.perkName}" to "§7${it.description}" }?.let { putAll(it) }
                }.map { pair ->
                    pair.value?.let {
                        Renderable.hoverTips(
                            Renderable.string(pair.key),
                            listOf(Renderable.wrappedString(it, 200)),
                        )
                    } ?: Renderable.string(pair.key)
                },
            )
        },
    ),
    CANDIDATES(
        "Candidates",
        {
            val candidates = MayorAPI.rawMayorData?.current?.candidates ?: emptyList()
            Renderable.verticalContainer(
                candidates.map { mayor ->
                    val candidateColor = MayorAPI.mayorNameToColorCode(mayor.name)
                    Renderable.verticalContainer(
                        buildMap {
                            put("${candidateColor}Candidate ${mayor.name}", null)
                            mayor.perks.map { perk ->
                                val ministerPerk = if (perk.minister) "§6✯ " else ""
                                " ${ministerPerk}§e${perk.name}" to "§7${perk.description}"
                            }.let { putAll(it) }
                        }.map { pair ->
                            pair.value?.let {
                                Renderable.hoverTips(
                                    Renderable.string(pair.key),
                                    listOf(Renderable.wrappedString(it, 200)),
                                )
                            } ?: Renderable.string(pair.key)
                        },
                    )
                },
                spacing = 5,
            )
        },
    ),
    ;

    override fun toString() = configLine

    @SkyHanniModule
    companion object {
        private val config get() = SkyHanniMod.feature.gui.mayorOverlay
        var display: Renderable? = null

        @SubscribeEvent
        fun onSecondPassed(event: SecondPassedEvent) {
            if (!isEnabled()) return

            display = config.mayorOverlay.map { it.display() }.let { Renderable.verticalContainer(it, spacing = 10) }
        }

        @SubscribeEvent
        fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
            if (!isEnabled()) return
            val display = display ?: return
            config.position.renderRenderable(display, posLabel = "Mayor Overlay")
        }

        fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
    }
}
