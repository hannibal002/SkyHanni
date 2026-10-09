package at.hannibal2.skyhanni.features.fishing.trophyfrog

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.fishing.TrophyFrogCaughtEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyCollectionDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object TrophyFrogDisplay : TrophyCollectionDisplay() {
    override val config get() = SkyHanniMod.feature.fishing.trophyFrogs.display
    override val data get() = TrophyFrogManager.frog
    override val header = "§e§lTrophy Frog Display"
    override val posLabel = "Trophy Frog Display"
    override val collectionName = "Trophy Frogs"
    override val dataSourceName = "Researcher Ribery"

    override fun getInternalName(rawName: String) = TrophyFrogManager.getInternalName(rawName)
    override fun getDisplayName(rawName: String) = TrophyFrogManager.getDisplayName(rawName)
    override fun hoverInfo(rawName: String) = TrophyFrogApi.hoverInfo(rawName)
    override fun isOnIsland() = IslandType.LOTUS_ATOLL.isInIsland()
    override fun holdingRod() = FishingApi.holdingWaterRod
    override fun passesGearCheck() = FishingApi.isWearingAnyTrophyArmor()

    @HandleEvent(onlyOnIsland = LOTUS_ATOLL)
    private fun onIslandJoin() {
        delayedIslandJoinUpdate()
    }

    @HandleEvent
    private fun onTrophyFrogCaught(event: TrophyFrogCaughtEvent) {
        onCaught(event.trophyFrogName, event.rarity)
    }

    @HandleEvent
    private fun onProfileJoin() {
        resetAndUpdate()
    }

    @HandleEvent
    private fun onConfigLoad() {
        watchConfig()
    }

    @HandleEvent
    private fun onGuiRenderTop() {
        render()
    }
}
