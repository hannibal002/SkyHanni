package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ProfileStorageData {
    var playerSpecific: Storage.PlayerSpecific? = null
    var profileSpecific: Storage.ProfileSpecific? = null
    var loaded = false
    private var noTabListTime = -1L

    private var nextProfile: String? = null

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: LorenzChatEvent) {
        "§7Switching to profile (?<name>.*)\\.\\.\\.".toPattern().matchMatcher(event.message) {
            nextProfile = group("name").lowercase()
            loaded = false
            PreProfileSwitchEvent().postAndCatch()
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        val profileName = nextProfile ?: return
        nextProfile = null

        val playerSpecific = playerSpecific
        if (playerSpecific == null) {
            LorenzUtils.error("profileSpecific after profile swap can not be set: playerSpecific is null!")
            return
        }
        loadProfileSpecific(playerSpecific, profileName, "profile swap (chat message)")
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
            loadProfileSpecific(playerSpecific, profileName, "first join (chat message)")
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (profileSpecific != null) return
        val playerSpecific = playerSpecific ?: return
        for (line in event.tabList) {
            val pattern = "§e§lProfile: §r§a(?<name>.*)".toPattern()
            pattern.matchMatcher(line) {
                val profileName = group("name").lowercase()
                loadProfileSpecific(playerSpecific, profileName, "tab list")
                nextProfile = null
                return
            }
        }

        if (LorenzUtils.inSkyBlock) {
            noTabListTime = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (noTabListTime == -1L) return

        if (System.currentTimeMillis() > noTabListTime + 3_000) {
            noTabListTime = System.currentTimeMillis()
            LorenzUtils.chat(
                "§c[SkyHanni] Extra Information from Tab list not found! " +
                        "Enable it: SkyBlock Menu ➜ Settings ➜ Personal ➜ User Interface ➜ Player List Info"
            )
        }
    }

    private fun loadProfileSpecific(playerSpecific: Storage.PlayerSpecific, profileName: String, reason: String) {
        noTabListTime = -1
        profileSpecific = playerSpecific.profiles.getOrPut(profileName) { Storage.ProfileSpecific() }
        tryMigrateProfileSpecific()
        loaded = true
        ConfigLoadEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        val playerUuid = LorenzUtils.getRawPlayerUuid()
        playerSpecific = SkyHanniMod.feature.storage.players.getOrPut(playerUuid) { Storage.PlayerSpecific() }
        migratePlayerSpecific()
        ConfigLoadEvent().postAndCatch()
    }

    private fun migratePlayerSpecific() {
        val oldHidden = SkyHanniMod.feature.hidden
        if (oldHidden.isMigrated) return

        SkyHanniMod.feature.storage?.let {
            it.gardenJacobFarmingContestTimes = oldHidden.gardenJacobFarmingContestTimes
        }
    }

    private fun tryMigrateProfileSpecific() {
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