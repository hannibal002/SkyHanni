package at.hannibal2.hanni.features.gui

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.data.ElectionApi
import at.hannibal2.hanni.data.Perk
import at.hannibal2.hanni.data.Perk.Companion.toPerk
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.hanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.hanni.utils.renderables.primitives.text

private val config get() = HanniMod.feature.gui.mayorOverlay

enum class MayorOverlay(private val configLine: String, private val createLines: () -> Renderable) {
    TITLE(
        "Title",
        { Renderable.text("§6§lMAYOR OVERLAY") },
    ),
    MAYOR(
        "Mayor",
        {
            val currentMayor = ElectionApi.currentMayor
            renderPerson(
                "Mayor",
                currentMayor?.mayorName,
                currentMayor?.activePerks,
            )
        },
    ),
    MINISTER(
        "Minister",
        {
            val currentMinister = ElectionApi.currentMinister
            renderPerson(
                "Minister",
                currentMinister?.mayorName,
                currentMinister?.activePerks,
            )
        },
    ),
    CANDIDATES(
        "Candidates",
        {
            val candidates = ElectionApi.rawMayorData?.current?.candidates.orEmpty()

            Renderable.vertical(
                candidates.map { candidate ->
                    renderPerson(
                        "Candidate",
                        candidate.name,
                        candidate.perks.mapNotNull { it.toPerk() },
                    )
                },
                spacing = config.candidateSpacing,
            )
        },
    ),
    NEW_MAYOR(
        "New Mayor Time",
        {
            Renderable.text("§7New Mayor in: §e${ElectionApi.nextMayorTimestamp.timeUntil().format(showMilliSeconds = false)}")
        },
    ),
    ;

    override fun toString() = configLine

    @HanniModule
    companion object {
        var display: Renderable? = null

        @HandleEvent
        fun onSecondPassed(event: SecondPassedEvent) {
            if (!isEnabled()) return
            with(config) {
                display = Renderable.vertical(mayorOverlay.map { it.createLines() }, spacing = spacing)
            }
        }

        @HandleEvent
        fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
            if (!isEnabled()) return
            display?.let { config.position.renderRenderable(it, posLabel = "Mayor Overlay") }
        }

        private fun isEnabled() = (SkyBlockUtils.inSkyBlock || OutsideSBFeature.MAYOR_OVERLAY.isSelected()) && config.enabled
    }
}

private fun renderPerson(title: String, name: String?, perks: List<Perk>?): Renderable {
    val colorCode = ElectionApi.mayorNameToColorCode(name.orEmpty())
    val perkLines = perks?.takeIf { config.showPerks }?.map { perk ->
        " ${if (perk.minister) "§6✯ " else ""}§e${perk.perkName}" to "§7${perk.description}"
    }.orEmpty()

    return Renderable.vertical(
        buildMap {
            name?.let { put("$colorCode$title $it", null) }
            putAll(perkLines)
        }.map { (key, value) ->
            value?.let {
                Renderable.hoverTips(
                    Renderable.text(key),
                    listOf(Renderable.wrappedText(it, 200)),
                )
            } ?: Renderable.text(key)
        },
    )
}
