package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ProfileStorageData {
    var playerSpecific: Storage.PlayerSpecific? = null
    var profileSpecific: Storage.ProfileSpecific? = null
    var loaded = false

    private var nextProfile: String? = null

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: LorenzChatEvent) {
        "§7Switching to profile (?<name>.*)\\.\\.\\.".toPattern().matchMatcher(event.message) {
            nextProfile = group("name").lowercase()
            println("switching to profile: '$nextProfile'")
            loaded = false
            PreProfileSwitchEvent().postAndCatch()
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onWorldChange(event: WorldEvent.Load) {
        val profileName = nextProfile ?: return
        nextProfile = null
        println("new world after profile swap.")

        val playerSpecific = playerSpecific
        if (playerSpecific == null) {
            LorenzUtils.error("profileSpecific after profile swap can not be set: playerSpecific is null!")
            return
        }
        profileSpecific = playerSpecific.profiles.getOrPut(profileName) { Storage.ProfileSpecific() }
        loaded = true
        println("profileSpecific loaded after profile swap!")
        ConfigLoadEvent().postAndCatch()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onProfileJoin(event: ProfileJoinEvent) {
        val playerSpecific = playerSpecific
        if (playerSpecific == null) {
            LorenzUtils.error("playerSpecific is null in ProfileJoinEvent!")
            return
        }

        if (profileSpecific == null) {
            val profileName = event.name
            profileSpecific = playerSpecific.profiles.getOrPut(profileName) { Storage.ProfileSpecific() }
            loaded = true
            migrateProfileSpecific()
            println("profileSpecific loaded for first join!")
            ConfigLoadEvent().postAndCatch()
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: HypixelJoinEvent) {
        val playerUuid = LorenzUtils.getRawPlayerUuid()
        playerSpecific = SkyHanniMod.feature.storage.players.getOrPut(playerUuid) { Storage.PlayerSpecific() }
        migratePlayerSpecific()
        println("loaded playerSpecific because of HypixelJoinEvent!")
        ConfigLoadEvent().postAndCatch()
    }

    private fun migratePlayerSpecific() {
        val oldHidden = SkyHanniMod.feature.hidden
        if (oldHidden.isMigrated) return

        SkyHanniMod.feature.storage.apiKey = oldHidden.apiKey

        SkyHanniMod.feature.storage?.let {
            it.gardenJacobFarmingContestTimes = oldHidden.gardenJacobFarmingContestTimes
        }
    }

    private fun migrateProfileSpecific() {
        val oldHidden = SkyHanniMod.feature.hidden
        if (oldHidden.isMigrated) return

        profileSpecific?.let {
            it.currentPet = oldHidden.currentPet

            for ((rawLocation, minionName) in oldHidden.minionName) {
                val lastClick = oldHidden.minionLastClick[rawLocation] ?: -1
                val location = LorenzVec.decodeFromString(rawLocation)
                val minionConfig = Storage.ProfileSpecific.MinionConfig()
                minionConfig.displayName = minionName
                minionConfig.lastClicked = lastClick
                it.minions[location] = minionConfig
            }
        }

        profileSpecific?.crimsonIsle?.let {
            it.quests = oldHidden.crimsonIsleQuests
            it.latestTrophyFishInInventory = oldHidden.crimsonIsleLatestTrophyFishInInventory
            it.miniBossesDoneToday = oldHidden.crimsonIsleMiniBossesDoneToday
            it.kuudraTiersDone = oldHidden.crimsonIsleKuudraTiersDone
        }

            profileSpecific?.garden?.let {
            it.experience = oldHidden.gardenExp
            it.cropCounter = oldHidden.gardenCropCounter
            it.cropUpgrades = oldHidden.gardenCropUpgrades

            for ((crop, speed) in oldHidden.gardenCropsPerSecond) {
                if (speed != -1) {
                    it.cropsPerSecond[crop] = speed
                }
            }

            it.latestBlocksPerSecond = oldHidden.gardenLatestBlocksPerSecond
            it.latestTrueFarmingFortune = oldHidden.gardenLatestTrueFarmingFortune
            it.savedCropAccessory = oldHidden.savedCropAccessory
            it.dicerRngDrops = oldHidden.gardenDicerRngDrops
            it.informedAboutLowMatter = oldHidden.informedAboutLowMatter
            it.informedAboutLowFuel = oldHidden.informedAboutLowFuel
            it.visitorInterval = oldHidden.visitorInterval
            it.nextSixthVisitorArrival = oldHidden.nextSixthVisitorArrival
            it.farmArmorDrops = oldHidden.gardenFarmingArmorDrops
            it.composterUpgrades = oldHidden.gardenComposterUpgrades
            it.toolWithBountiful = oldHidden.gardenToolHasBountiful
            it.composterCurrentOrganicMatterItem = oldHidden.gardenComposterCurrentOrganicMatterItem
            it.composterCurrentFuelItem = oldHidden.gardenComposterCurrentFuelItem
        }

        profileSpecific?.garden?.visitorDrops?.let {
            val old = oldHidden.visitorDrops
            it.acceptedVisitors = old.acceptedVisitors
            it.deniedVisitors = old.deniedVisitors
            it.visitorRarities = old.visitorRarities
            it.copper = old.copper
            it.farmingExp = old.farmingExp
            it.coinsSpent = old.coinsSpent
            it.rewardsCount = old.rewardsCount
        }

        oldHidden.isMigrated = true
    }
}