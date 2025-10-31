package at.hannibal2.hanni.features.mining.crystalhollows

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.features.mining.nucleus.CrystalHighlighterConfig.BoundingBoxType
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.hanni.features.event.hoppity.HoppityApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawHitbox
import at.hannibal2.hanni.utils.render.WorldRenderUtils.expandBlock
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.util.AxisAlignedBB

@HanniModule
object NucleusBarriersBox {
    private val config get() = HanniMod.feature.mining.crystalHighlighter
    private val colorConfig get() = config.colors

    private var inNucleus = false

    private enum class Crystal(
        val boundingBox: AxisAlignedBB,
        val configColorOption: Property<ChromaColour>,
    ) {
        AMBER(
            LorenzVec(474, 124, 524).axisAlignedTo(LorenzVec(485, 111, 535))
                .expandBlock(),
            colorConfig.amber,
        ),
        AMETHYST(
            LorenzVec(474, 124, 492).axisAlignedTo(LorenzVec(485, 111, 503))
                .expandBlock(),
            colorConfig.amethyst,
        ),
        TOPAZ(
            LorenzVec(508, 124, 473).axisAlignedTo(LorenzVec(519, 111, 484))
                .expandBlock(),
            colorConfig.topaz,
        ),
        JADE(
            LorenzVec(542, 124, 492).axisAlignedTo(LorenzVec(553, 111, 503))
                .expandBlock(),
            colorConfig.jade,
        ),
        SAPPHIRE(
            LorenzVec(542, 124, 524).axisAlignedTo(LorenzVec(553, 111, 535))
                .expandBlock(),
            colorConfig.sapphire,
        ),
    }

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inNucleus = event.area == "Crystal Nucleus"
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return

        Crystal.entries.forEach { crystal ->
            when (config.boxStyle) {
                BoundingBoxType.FILLED -> {
                    event.drawFilledBoundingBox(
                        crystal.boundingBox,
                        crystal.configColorOption.get(),
                    )
                }

                BoundingBoxType.OUTLINE -> {
                    event.drawHitbox(
                        crystal.boundingBox,
                        crystal.configColorOption.get().toColor(),
                    )
                }
            }
        }
    }

    private fun isEnabled(): Boolean = IslandType.CRYSTAL_HOLLOWS.isCurrent() && inNucleus &&
        (HoppityApi.isHoppityEvent() || !config.onlyDuringHoppity) && config.enabled
}
