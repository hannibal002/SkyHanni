package at.hannibal2.skyhanni.features.garden.farming

import EliteLeaderboardDisplay
import at.hannibal2.skyhanni.config.features.garden.leaderboards.CropCollectionDisplayConfig.CropCollectionTextEntry
import at.hannibal2.skyhanni.data.garden.CropCollectionApi
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.data.garden.FarmingWeight
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableNullableButton

class CropDisplay: EliteLeaderboardDisplay<CropType, EliteLeaderboardType.Crop>(
    GardenApi.storage?.farmingWeight?.cropDisplayType,
    { crop, mode -> EliteLeaderboardType.Crop(crop, mode) },
    name = "Crop Leaderboard Display"
) {
    val config get() = configBase.cropCollectionDisplay

    override fun getDefaultEnum(): CropType? {
        return CropType.MELON
    }

    override fun drawDisplay(leaderboardType: EliteLeaderboardType) {
        if (!isEnabled()) return

        val lineMap = mutableMapOf<CropCollectionTextEntry, Renderable>()

        lineMap[CropCollectionTextEntry.WEIGHT_POSITION] = weightPosRenderable(leaderboardType)
        lineMap[CropCollectionTextEntry.OVERTAKE] = overtakeRenderable(leaderboardType)

        display = formatDisplay(lineMap)
    }

    override fun overtakeEta(weightUntil: Double): String {
        return ""
    }

    override fun useEtaGoalRank(): Boolean {
        return config.useEtaGoalRank.get()
    }

    override fun showLeaderboard(): Boolean = config.leaderboard.get()

    private fun formatDisplay(lineMap: MutableMap<CropCollectionTextEntry, Renderable>): List<Renderable> {
        if (FarmingWeight.apiError || EliteFarmersLeaderboard.apiError) {
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

    override fun shouldShowDisplay(): Boolean {
        return true
    }


}
