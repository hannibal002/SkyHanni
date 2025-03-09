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

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.linesToDraw.any()) return
        val activeDataSet = PigFeaturesApi.activeDataSet ?: return

        event.tryRenderLineToPig(activeDataSet)
        event.tryRenderLinePigToOrb(activeDataSet)
    }

    private fun SkyHanniRenderWorldEvent.tryRenderLineToPig(dataSet: PigFeaturesApi.ShinyOrbDataSet, ) {
        val pigEntity = dataSet.clickedPigEntityId?.let { EntityUtils.getEntityByID(it) } ?: return
        val nearPig = pigEntity.distanceToPlayer() < 5
        val lineToPigEnabled = config.linesToDraw.contains(YearOfThePigConfig.ShinyOrbLineType.TO_PIG) && !nearPig
        if (!lineToPigEnabled) return

        val pigEntityLocation = exactLocation(pigEntity)
        drawLineToEye(
            pigEntityLocation.up(0.54),
            Color.PINK,
            1,
            true
        )
    }

    private fun SkyHanniRenderWorldEvent.tryRenderLinePigToOrb(
        dataSet: PigFeaturesApi.ShinyOrbDataSet
    ): Boolean {
        val orbEntity = dataSet.shinyOrbEntityId?.let { EntityUtils.getEntityByID(it) } ?: return false
        val pigEntity = dataSet.clickedPigEntityId?.let { EntityUtils.getEntityByID(it) } ?: return false

        val lineToOrbEnabled = config.linesToDraw.contains(YearOfThePigConfig.ShinyOrbLineType.TO_ORB)
        if (!lineToOrbEnabled) return false

        val orbEntityLocation = exactLocation(orbEntity)
        val pigEntityLocation = exactLocation(pigEntity)

        draw3DLine(
            pigEntityLocation,
            orbEntityLocation,
            Color.YELLOW,
            1,
            true
        )
        return true
    }

}
