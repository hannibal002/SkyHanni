package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.jsonobjects.TrophyFishJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TrophyFishManager {

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val json = event.getConstant<TrophyFishJson>("TrophyFish")
        trophyFishInfo = json.trophy_fish
        LorenzUtils.debug("Loaded trophy fish from repo")
    }

    companion object {
        val fishes: MutableMap<String, MutableMap<TrophyRarity, Int>>?
            get() = ProfileStorageData.profileSpecific?.crimsonIsle?.trophyFishes

        private var trophyFishInfo = mapOf<String, TrophyFishInfo>()

        fun getInfo(internalName: String) = trophyFishInfo[internalName]

        fun getInfoByName(name: String) = trophyFishInfo.values.find { it.displayName == name }
    }
}
