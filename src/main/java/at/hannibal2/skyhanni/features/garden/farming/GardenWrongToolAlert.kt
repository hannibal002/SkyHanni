package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenWrongToolAlert {

    private val config get() = GardenApi.config.wrongToolAlert
    private var lastWarning: SimpleTimeMark = SimpleTimeMark.farPast()
    private var suppressed = false

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onCropClick(event: CropClickEvent) {
        if (!config.enabled) return
        if (suppressed) return
        if (GardenApi.cropInHand == null) return
        if (GardenApi.cropInHand == event.crop) return
        if (GardenApi.onUnfarmablePlot) return
        if (lastWarning.passedSince() < 20.seconds) return
        warn(event.crop)
    }

    fun warn(crop: CropType) {
        lastWarning = SimpleTimeMark.now()
        val cropName = crop.cropName
        val toolName = GardenApi.itemInHand?.hoverName.formattedTextCompatLessResets()
        ChatUtils.notifyOrDisable(
            "§cWrong tool detected while farming §f${cropName} §cwith ${toolName}§c!",
            config::enabled,
        )
        ChatUtils.clickableChat(
            message = "§e[CLICK to suppress this warning for this session]",
            hover = "§eClick to suppress this warning for this session!",
            onClick = {suppress()}, prefix = false, oneTimeClick = true)
        if (config.showTitle) {
            TitleManager.sendTitle("§cWrong tool!")
        }
    }

    fun suppress() {
        suppressed = true
        ChatUtils.chat("§aWarning suppressed for the current session!")
    }
}
