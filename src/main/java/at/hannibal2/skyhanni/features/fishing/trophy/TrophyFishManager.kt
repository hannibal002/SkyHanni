package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.jsonobjects.TrophyFishJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


class TrophyFishManager {

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        hasLoadedTrophyFish = false
    }

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        if (hasLoadedTrophyFish) return
        val trophyFishes = fishes ?: return
        val profileData = event.profileData
        trophyFishes.clear()
        for ((rawName, value) in profileData["trophy_fish"].asJsonObject.entrySet()) {
            val rarity = TrophyRarity.getByName(rawName) ?: continue
            val text = rawName.replace("_", "")
            val displayName = text.substring(0, text.length - rarity.name.length)

            val amount = value.asInt
            val rarities = trophyFishes.getOrPut(displayName) { mutableMapOf() }
            rarities[rarity] = amount
            hasLoadedTrophyFish = true
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        try {
            val json = event.getConstant<TrophyFishJson>("TrophyFish")
                ?: error("Could not read repo data from TrophyFish.json")
            trophyFishInfo = json.trophy_fish
            LorenzUtils.debug("Loaded trophy fish from repo")
        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("error in RepositoryReloadEvent")
        }
    }

    companion object {
        private var hasLoadedTrophyFish = false

        val fishes: MutableMap<String, MutableMap<TrophyRarity, Int>>?
            get() = ProfileStorageData.profileSpecific?.crimsonIsle?.trophyFishes

        private var trophyFishInfo = mapOf<String, TrophyFishInfo>()

        fun getInfo(internalName: String) = trophyFishInfo[internalName]

        fun getInfoByName(name: String) = trophyFishInfo.values.find { it.displayName == name }
    }
}