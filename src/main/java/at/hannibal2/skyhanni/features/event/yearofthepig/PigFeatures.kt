package at.hannibal2.skyhanni.features.event.yearofthepig

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.yearofthepig.YearOfThePigConfig
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import java.awt.Color

@SkyHanniModule
object PigFeatures {

    private val config get() = SkyHanniMod.feature.event.yearOfThePig
    private val dataSetList get() = PigFeaturesApi.dataSetList

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.linesToDraw.any()) return

        dataSetList.forEach { dataSet ->
            event.tryRenderLineToPig(dataSet)
            event.tryRenderLinePigToOrb(dataSet)
        }
    }

    private fun SkyHanniRenderWorldEvent.tryRenderLineToPig(dataSet: PigFeaturesApi.ShinyOrbData) {
        val pigEntity = EntityUtils.getEntityByID(dataSet.pigEntityId) ?: return

        val lineToPigEnabled = config.linesToDraw.contains(YearOfThePigConfig.ShinyOrbLineType.TO_PIG)
        if (!lineToPigEnabled) return

        val pigEntityLocation = WorldRenderUtils.exactLocation(pigEntity, partialTicks)
        drawLineToEye(
            pigEntityLocation.up(0.54),
            Color.PINK,
            3,
            true,
        )
    }

    private fun SkyHanniRenderWorldEvent.tryRenderLinePigToOrb(dataSet: PigFeaturesApi.ShinyOrbData) {
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
