package at.hannibal2.skyhanni.features.fishing
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
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
        trophyFishInfo.clear()
        try {
            val data = event.getConstant("TrophyFish")!!
            for ((internalName, value) in data.entrySet()) {
                val trophyFish = value.asJsonObject
                val displayName = trophyFish["displayName"].asString
                val description = trophyFish["description"].asString
                val rate = trophyFish["rate"].runCatching { this.asInt }.getOrNull()
                val filletObject = trophyFish["fillet"].asJsonObject
                val filletValues = filletObject.entrySet().associate { (rarity, value) ->
                    Pair(TrophyRarity.getByName(rarity)!!, value.asInt)
                }
                trophyFishInfo[internalName] = TrophyFishInfo(
                    internalName,
                    displayName,
                    description,
                    rate,
                    filletValues
                )
            }
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

        private val trophyFishInfo = mutableMapOf<String, TrophyFishInfo>()

        fun getInfo(internalName: String): TrophyFishInfo? {
            return trophyFishInfo[internalName]
        }
    }
}