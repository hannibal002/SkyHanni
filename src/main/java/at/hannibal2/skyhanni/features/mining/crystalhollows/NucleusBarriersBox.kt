package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.mining.nucleus.CrystalHighlighterConfig.BoundingBoxType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.VectorUtils.axisAlignedTo
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawHitbox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.expandBlock
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

@SkyHanniModule
object NucleusBarriersBox {
    private val config get() = SkyHanniMod.feature.mining.crystalHighlighter
    private val colorConfig get() = config.colors

    private var inNucleus = false

    private enum class Crystal(
        val boundingBox: AABB,
        val configColorOption: Property<ChromaColour>,
    ) {
        AMBER(
            Vec3(474.0, 124.0, 524.0).axisAlignedTo(Vec3(485.0, 111.0, 535.0))
                .expandBlock(),
            colorConfig.amber,
        ),
        AMETHYST(
            Vec3(474.0, 124.0, 492.0).axisAlignedTo(Vec3(485.0, 111.0, 503.0))
                .expandBlock(),
            colorConfig.amethyst,
        ),
        TOPAZ(
            Vec3(508.0, 124.0, 473.0).axisAlignedTo(Vec3(519.0, 111.0, 484.0))
                .expandBlock(),
            colorConfig.topaz,
        ),
        JADE(
            Vec3(542.0, 124.0, 492.0).axisAlignedTo(Vec3(553.0, 111.0, 503.0))
                .expandBlock(),
            colorConfig.jade,
        ),
        SAPPHIRE(
            Vec3(542.0, 124.0, 524.0).axisAlignedTo(Vec3(553.0, 111.0, 535.0))
                .expandBlock(),
            colorConfig.sapphire,
        ),
    }

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inNucleus = event.area == "Crystal Nucleus"
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
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
