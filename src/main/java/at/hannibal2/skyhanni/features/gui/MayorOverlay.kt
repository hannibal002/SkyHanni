package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.Perk.Companion.toPerk
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

private fun renderPerson(
    title: String,
    name: String?,
    perks: List<Perk>?,
): Renderable {
    val colorCode = MayorAPI.mayorNameToColorCode(name ?: "")

    return Renderable.verticalContainer(
        buildMap {
            name?.let { put("$colorCode$title $it", null) }
            perks?.map { perk ->
                val ministerPerk = if (perk.minister) "§6✯ " else ""
                " $ministerPerk§e${perk.perkName}" to "§7${perk.description}"
            }?.let { putAll(it) }
        }.map { pair ->
            pair.value?.let {
                Renderable.hoverTips(
                    Renderable.string(pair.key),
                    listOf(Renderable.wrappedString(it, 200)),
                )
            } ?: Renderable.string(pair.key)
        },
    )
}

enum class MayorOverlay(private val configLine: String, private val createLines: () -> Renderable) {
    MAYOR(
        "Mayor",
        {
            val currentMayor = MayorAPI.currentMayor
            renderPerson(
                title = "Mayor",
                name = currentMayor?.mayorName,
                perks = currentMayor?.activePerks,
            )
        },
    ),
    MINISTER(
        "Minister",
        {
            val currentMinister = MayorAPI.currentMinister
            renderPerson(
                title = "Minister",
                name = currentMinister?.mayorName,
                perks = currentMinister?.activePerks,
            )
        },
    ),
    CANDIDATES(
        "Candidates",
        {
            val candidates = MayorAPI.rawMayorData?.current?.candidates ?: emptyList()

            Renderable.verticalContainer(
                candidates.map { candidate ->
                    renderPerson(
                        title = "Candidate",
                        name = candidate.name,
                        perks = candidate.perks.mapNotNull { it.toPerk() },
                    )
                },
                spacing = 5,
            )
        },
    );

    override fun toString() = configLine

    @SkyHanniModule
    companion object {
        private val config get() = SkyHanniMod.feature.gui.mayorOverlay
        var display: Renderable? = null

        @SubscribeEvent
        fun onSecondPassed(event: SecondPassedEvent) {
            if (!isEnabled()) return

            display = config.mayorOverlay.map { it.createLines() }.let { Renderable.verticalContainer(it, spacing = 10) }
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
