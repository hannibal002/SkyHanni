package at.hannibal2.hanni.features.event.yearofthepig

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.features.event.yearofthepig.YearOfThePigConfig
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toChromaColor
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.render.WorldRenderUtils
import at.hannibal2.hanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawLineToEye
import java.awt.Color

@HanniModule
object PigFeatures {

    private val config get() = HanniMod.feature.event.yearOfThePig
    private val dataSetList get() = PigFeaturesApi.dataSetList

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!config.linesToDraw.any()) return

        dataSetList.forEach { dataSet ->
            event.tryRenderLineToPig(dataSet)
            event.tryRenderLinePigToOrb(dataSet)
        }
    }

    private fun HanniRenderWorldEvent.tryRenderLineToPig(dataSet: PigFeaturesApi.ShinyOrbData) {
        val pigEntity = EntityUtils.getEntityByID(dataSet.pigEntityId) ?: return

        val lineToPigEnabled = config.linesToDraw.contains(YearOfThePigConfig.ShinyOrbLineType.TO_PIG)
        if (!lineToPigEnabled) return

        val pigEntityLocation = WorldRenderUtils.exactLocation(pigEntity, partialTicks)
        drawLineToEye(
            pigEntityLocation.up(0.54),
            Color.PINK.toChromaColor(),
            3,
            true,
        )
    }

    private fun HanniRenderWorldEvent.tryRenderLinePigToOrb(dataSet: PigFeaturesApi.ShinyOrbData) {
        val pigEntity = EntityUtils.getEntityByID(dataSet.pigEntityId) ?: return

        val lineToOrbEnabled = config.linesToDraw.contains(YearOfThePigConfig.ShinyOrbLineType.TO_ORB)
        if (!lineToOrbEnabled) return

        val orbEntityLocation = dataSet.shinyOrbLocation
        val pigEntityLocation = WorldRenderUtils.exactLocation(pigEntity, partialTicks)

        draw3DLine(
            pigEntityLocation.up(0.54),
            orbEntityLocation.down(0.5),
            Color.YELLOW,
            3,
            true,
        )
        return
    }
}
