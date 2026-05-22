package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.compat.changeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenWrongToolAlert {

    private val config get() = GardenApi.config.wrongToolAlert
    private var lastWarning: SimpleTimeMark = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onCropBreak(event: BlockClickEvent) {
        if (!config.enabled) return
        if (GardenApi.cropInHand == null) return
        if (GardenApi.cropInHand == GardenApi.lastBrokenCropType) return
        if (GardenApi.onUnfarmablePlot) return
        if (lastWarning.passedSince() < 20.seconds) return
        warn()
    }

    fun warn() {
        lastWarning = SimpleTimeMark.now()
        val cropName = GardenApi.lastBrokenCropType?.cropName
        val toolName = GardenApi.itemInHand?.hoverName.formattedTextCompatLessResets()
        ChatUtils.notifyOrDisable(
            "§cWrong tool detected while farming §f${cropName} §cwith ${toolName}§c!",
            config::enabled,
        )
        if (config.showTitle) {
            TitleManager.sendTitle("§cWrong tool!")
        }
    }
}
