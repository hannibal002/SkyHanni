package at.hannibal2.skyhanni.features.event.yearofthepig

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.yearofthepig.YearOfThePigConfig
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import java.awt.Color

@SkyHanniModule
object PigFeatures {

    private val config get() = SkyHanniMod.feature.event.yearOfThePig
    private val dataSet get() = PigFeaturesApi.dataSet

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.linesToDraw.any()) return

        event.tryRenderLineToPig(dataSet)
        event.tryRenderLinePigToOrb(dataSet)
    }

    private fun SkyHanniRenderWorldEvent.tryRenderLineToPig(dataSet: PigFeaturesApi.ShinyOrbDataSet) {
        val pigEntity = dataSet.pigEntityId?.let { EntityUtils.getEntityByID(it) } ?: return

        val nearPig = pigEntity.distanceToPlayer() < 5
        val lineToPigEnabled = config.linesToDraw.contains(YearOfThePigConfig.ShinyOrbLineType.TO_PIG) && !nearPig
        if (!lineToPigEnabled) return

        val pigEntityLocation = exactLocation(pigEntity)
        drawLineToEye(
            pigEntityLocation.up(0.54),
            Color.PINK,
            3,
            true
        )
    }

    private fun SkyHanniRenderWorldEvent.tryRenderLinePigToOrb(
        dataSet: PigFeaturesApi.ShinyOrbDataSet
    ) {
        val pigEntity = dataSet.pigEntityId?.let { EntityUtils.getEntityByID(it) } ?: return
        val nearPig = pigEntity.distanceToPlayer() < 10

        val lineToOrbEnabled = config.linesToDraw.contains(YearOfThePigConfig.ShinyOrbLineType.TO_ORB) && nearPig
        if (!lineToOrbEnabled) return

        val orbEntityLocation = dataSet.shinyOrbLocation ?: return
        val pigEntityLocation = exactLocation(pigEntity)

        draw3DLine(
            pigEntityLocation.up(0.54),
            orbEntityLocation.down(0.5),
            Color.YELLOW,
            3,
            true
        )
        return
    }

}
