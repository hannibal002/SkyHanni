package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.fishing.TrophyFishCaughtEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
object TrophyFishDisplay : TrophyCollectionDisplay() {
    override val config get() = SkyHanniMod.feature.fishing.trophyFishing.display
    override val data get() = TrophyFishManager.fish
    override val header = "§e§lTrophy Fish Display"
    override val posLabel = "Trophy Fishing Display"
    override val collectionName = "Trophy Fish"
    override val dataSourceName = "Odger"

    private val itemNameCache = mutableMapOf<String, NeuInternalName>()

    override fun getDisplayName(rawName: String): String {
        val name = getInternalName(rawName).repoItemName
        return name.split(" ").dropLast(1).joinToString(" ")
    }

    override fun getInternalName(rawName: String): NeuInternalName {
        itemNameCache[rawName]?.let {
            return it
        }
        // getOrPut does not support our null check
        readInternalName(rawName)?.let {
            itemNameCache[rawName] = it
            return it
        }

        ErrorManager.skyHanniError(
            "No Trophy Fishing name found",
            "name" to rawName,
        )
    }

    private fun readInternalName(rawName: String): NeuInternalName? {
        for ((name, internalName) in NeuItems.allItemsCache) {
            val test = name.removeColor().replace(" ", "").replace("-", "")
            if (test.startsWith(rawName)) {
                return internalName
            }
        }
        if (rawName.endsWith("1")) return "OBFUSCATED_FISH_1_BRONZE".toInternalName()
        if (rawName.endsWith("2")) return "OBFUSCATED_FISH_2_BRONZE".toInternalName()
        if (rawName.endsWith("3")) return "OBFUSCATED_FISH_3_BRONZE".toInternalName()

        return null
    }

    override fun hoverInfo(rawName: String) = TrophyFishApi.hoverInfo(rawName)
    override fun isOnIsland() = IslandType.CRIMSON_ISLE.isInIsland() || SkyBlockUtils.isStrandedProfile
    override fun holdingRod() = FishingApi.holdingLavaRod
    override fun passesGearCheck() = FishingApi.isTrophyFishing()
    override fun canRenderExtra() = !FishingApi.hasTreasureHook
    override fun beforeUpdate() {
        TrophyFishManager.loadMissingTrophyFish()
    }

    @HandleEvent(onlyOnIsland = CRIMSON_ISLE)
    private fun onIslandJoin() {
        delayedIslandJoinUpdate()
    }

    @HandleEvent
    private fun onTrophyFishCaught(event: TrophyFishCaughtEvent) {
        onCaught(event.trophyFishName, event.rarity)
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

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val base = "fishing.trophyFishing.display"
        event.move(94, "$base.requireHunterArmor", "$base.requireArmor")
    }
}
