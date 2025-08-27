package at.hannibal2.skyhanni.features.garden.farming

import EliteLeaderboardDisplay
import at.hannibal2.skyhanni.config.features.garden.leaderboards.CropCollectionDisplayConfig.CropCollectionTextEntry
import at.hannibal2.skyhanni.config.features.garden.leaderboards.FarmingWeightDisplayConfig.FarmingWeightTextEntry
import at.hannibal2.skyhanni.data.garden.CropCollectionApi
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.data.garden.FarmingWeightData
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.getCurrentlyFarmedCrop
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton
import kotlin.time.Duration.Companion.seconds

class CropDisplay: EliteLeaderboardDisplay<CropType, EliteLeaderboardType.Crop>(
    GardenApi.storage?.farmingWeight?.cropDisplayType,
    { crop, mode -> EliteLeaderboardType.Crop(crop, mode) },
    name = "Crop Leaderboard Display"
) {
    val config get() = configBase.cropCollectionDisplay

    override fun getDefaultEnum(): CropType? {
        return if (!config.hideWhenNotFarming) {
            CropCollectionApi.lastGainedCrop ?: getCurrentlyFarmedCrop()
        } else {
            getCurrentlyFarmedCrop()
        }
    }

    override fun drawDisplay(leaderboardType: EliteLeaderboardType) {
        if (!isEnabled()) return

        val lineMap = mutableMapOf<CropCollectionTextEntry, Renderable>()
        val isFirst = leaderboardPos == 1

        lineMap[CropCollectionTextEntry.WEIGHT_POSITION] = weightPosRenderable(leaderboardType)
        lineMap[CropCollectionTextEntry.OVERTAKE] = overtakeRenderable(leaderboardType, isFirst)
        if (!isFirst && config.text.get().contains(CropCollectionTextEntry.OVERTAKE)) {
            lineMap[CropCollectionTextEntry.LAST_PLAYER] = overtakeRenderable(leaderboardType, true)
        }

        display = formatDisplay(lineMap)
    }

    override fun overtakeEta(amountUntil: Double): String {
        if (!config.overtakeETA.get() || !config.overtakeETAAlways.get() && !GardenApi.isCurrentlyFarming()) return ""

        val crop = currentEnum ?: getDefaultEnum() ?: return ""
        val cropsPerSecond = crop.getSpeed() ?: return ""
        val timeUntil = (amountUntil / cropsPerSecond).seconds
        return " §7(§b${timeUntil.format()}§7)"
    }

    override fun useEtaGoalRank(): Boolean {
        return config.useEtaGoalRank.get()
    }

    override fun showLeaderboard(): Boolean = config.leaderboard.get()

    private fun formatDisplay(lineMap: MutableMap<CropCollectionTextEntry, Renderable>): List<Renderable> {
        if (FarmingWeightData.apiError || EliteFarmersLeaderboard.apiError) {
            return errorMessage
        }

        val newList = mutableListOf<Renderable>()
        if (inventoryOpen) newList.buildModeSwitcher() else newList.addVerticalSpacer()
        newList.addAll(config.text.get().mapNotNull { lineMap[it] })
        if (inventoryOpen) newList.buildTypeSwitcher() else newList.addVerticalSpacer()
        return newList
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
            enableUniverseScroll = false // would infinitely scroll while hovered
        )
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun isEnabled(): Boolean = config.display && (inGardenEnabled())

    private fun inGardenEnabled() = SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.showOutsideGarden)

    override fun shouldShowDisplay(): Boolean = !GardenApi.hideExtraGuis()


}
