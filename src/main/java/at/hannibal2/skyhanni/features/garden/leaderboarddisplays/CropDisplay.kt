package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.data.garden.CropCollectionApi
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.getCurrentlyFarmedCrop
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

class CropDisplay : EliteLeaderboardDisplayBase<CropType, EliteLeaderboardType.Crop>(
    EliteLeaderboardType.Crop::class,
    { crop, mode -> EliteLeaderboardType.Crop(crop, mode) },
    name = "Crop Leaderboard Display"
) {
    val config get() = configBase.cropCollectionLeaderboard
    private val cropStorage get() = GardenApi.storage?.farmingWeight?.cropDisplayType

    override var currentMode: EliteLeaderboardMode
        get() = cropStorage?.mode ?: EliteLeaderboardMode.ALL_TIME
        set(value) { cropStorage?.mode = value }

    override var currentEnum: CropType?
        get() = cropStorage?.enum
        set(value) { cropStorage?.enum = value }

    override fun getDefaultEnum(): CropType? {
        return if (!config.display.hideWhenNotFarming) {
            CropCollectionApi.lastGainedCrop ?: getCurrentlyFarmedCrop()
        } else {
            getCurrentlyFarmedCrop()
        }
    }

    override fun overtakeEta(amountUntil: Double): String {
        if (!config.display.overtakeETA.get() || !config.display.overtakeETAAlways.get() && !GardenApi.isCurrentlyFarming()) return ""

        val crop = currentEnum ?: getDefaultEnum() ?: return ""
        val cropsPerSecond = crop.getSpeed() ?: return ""
        val timeUntil = (amountUntil / cropsPerSecond).seconds
        return " §7(§b${timeUntil.format()}§7)"
    }

    override fun MutableList<Renderable>.buildTypeSwitcher() {
        this.addRenderableNullableButton(
            label = "Crop Type",
            current = currentEnum,
            nullLabel = "Default",
            onChange = { new ->
                currentEnum = new
                update()
            },
            universe = CropType.entries,
        )
    }

    override fun shouldShowDisplay(): Boolean =
        !GardenApi.hideExtraGuis() && (GardenApi.isCurrentlyFarming() || !config.display.hideWhenNotFarming)
}

data class CropLeaderboardStorage(
    @Expose var enum: CropType?,
    @Expose var mode: EliteLeaderboardMode
)
