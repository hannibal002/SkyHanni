package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.StorageData
import at.hannibal2.skyhanni.config.storage.OrderedWaypointsRoutes
import at.hannibal2.skyhanni.config.storage.PlayerSpecificStorage
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TabListData
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ProfileStorageData {

    var playerSpecific: PlayerSpecificStorage? = null
    var profileSpecific: ProfileSpecificStorage? = null
    var loaded = false
    private var noTabListTime = SimpleTimeMark.farPast()

    private var sackPlayers: SackData.PlayerSpecific? = null
    var sackProfiles: SackData.ProfileSpecific? = null

    private var hypixelDataLoaded = false

    private var storagePlayer: StorageData.PlayerSpecific? = null
    var storageProfiles: StorageData.ProfileSpecific? = null

    private var petPlayers: PetDataStorage.PlayerSpecific? = null
    var petProfiles: PetDataStorage.ProfileSpecific? = null

    var orderedWaypointsRoutes: OrderedWaypointsRoutes? = null

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onProfileJoin(event: ProfileJoinEvent) {
        val playerSpecific = playerSpecific
        val sackPlayers = sackPlayers
        val storagePlayer = storagePlayer
        val petPlayers = petPlayers
        val orderedWaypointsRoutes = orderedWaypointsRoutes
        val profileName = event.name
        if (playerSpecific == null) {
            DelayedRun.runDelayed(10.seconds) {
                workaroundIn10SecondsProfileStorage(profileName)
            }
            return
        }
        if (sackPlayers == null) {
            ErrorManager.skyHanniError("sackPlayers is null in ProfileJoinEvent!")
        }
        if (storagePlayer == null) {
            ErrorManager.skyHanniError("storagePlayer is null in ProfileJoinEvent!")
        }
        if (petPlayers == null) {
            ErrorManager.skyHanniError("petPlayers is null in ProfileJoinEvent!")
        }
        if (orderedWaypointsRoutes == null) {
            ErrorManager.skyHanniError("orderedWaypointRoutes is null in ProfileJoinEvent!")
        }
        loadProfileSpecific(playerSpecific, sackPlayers, storagePlayer, petPlayers, profileName)
        ConfigLoadEvent.post()
    }

    private fun workaroundIn10SecondsProfileStorage(profileName: String) {
        println("workaroundIn10SecondsProfileStorage")
        val playerSpecific = playerSpecific
        val sackPlayers = sackPlayers
        val storagePlayer = storagePlayer
        val petPlayers = petPlayers
        val orderedWaypointsRoutes = orderedWaypointsRoutes

        if (playerSpecific == null) {
            ErrorManager.skyHanniError(
                "failed to load your profile data delayed ",
                "onHypixel" to SkyBlockUtils.onHypixel,
                "HypixelData.hypixelLive" to HypixelData.hypixelLive,
                "HypixelData.hypixelAlpha" to HypixelData.hypixelAlpha,
                "sidebarLinesFormatted" to ScoreboardData.sidebarLinesFormatted,
            )
        }
        if (sackPlayers == null) {
            ErrorManager.skyHanniError("sackPlayers is null in ProfileJoinEvent!")
        }
        if (storagePlayer == null) {
            ErrorManager.skyHanniError("storagePlayer is null in ProfileJoinEvent!")
        }
        if (petPlayers == null) {
            ErrorManager.skyHanniError("petPlayers is null in ProfileJoinEvent!")
        }
        if (orderedWaypointsRoutes == null) {
            ErrorManager.skyHanniError("orderedWaypointRoutes is null in ProfileJoinEvent!")
        }

        loadProfileSpecific(playerSpecific, sackPlayers, storagePlayer, petPlayers, profileName)
        ConfigLoadEvent.post()
    }

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PROFILE)) return
        noTabListTime = if (event.isClear()) SimpleTimeMark.now() else SimpleTimeMark.farPast()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (noTabListTime.isFarPast()) return

        playerSpecific?.let {
            // do not try to load the data when hypixel has not yet send the profile loaded message
            if (it.multipleProfiles && !hypixelDataLoaded) return
        }

        if (noTabListTime.passedSince() < 3.seconds) return
        noTabListTime = SimpleTimeMark.now()
        val foundSkyBlockTabList = TabListData.getTabList().any { it.contains("§b§lArea:") }
        if (foundSkyBlockTabList) {
            ChatUtils.clickableChat(
                "§cCan not read profile name from tab list! Open /widget and enable Profile Widget. " +
                    "This is needed for the mod to function! And therefore this warning cannot be disabled",
                onClick = {
                    HypixelCommands.widget()
                },
                "§eClick to run /widget!",
                replaceSameMessage = true,
            )
        } else {
            ChatUtils.chat(
                "§cExtra Information from Tab list not found! " +
                    "Enable it: SkyBlock Menu ➜ Settings ➜ Personal ➜ User Interface ➜ Player List Info",
                replaceSameMessage = true,
            )
        }
    }

    private fun loadProfileSpecific(
        playerSpecific: PlayerSpecificStorage,
        sackProfile: SackData.PlayerSpecific,
        storagePlayer: StorageData.PlayerSpecific,
        petPlayer: PetDataStorage.PlayerSpecific,
        profileName: String,
    ) {
        noTabListTime = SimpleTimeMark.farPast()
        profileSpecific = playerSpecific.profiles.getOrPut(profileName) { ProfileSpecificStorage() }
        sackProfiles = sackProfile.profiles.getOrPut(profileName) { SackData.ProfileSpecific() }
        storageProfiles = storagePlayer.profiles.getOrPut(profileName) { StorageData.ProfileSpecific() }
        petProfiles = petPlayer.profiles.getOrPut(profileName) { PetDataStorage.ProfileSpecific() }
        loaded = true
        ConfigLoadEvent.post()
    }

    @HandleEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        val playerUuid = PlayerUtils.getRawUuid()
        playerSpecific = SkyHanniMod.feature.storage.players.getOrPut(playerUuid) { PlayerSpecificStorage() }
        sackPlayers = SkyHanniMod.sackData.players.getOrPut(playerUuid) { SackData.PlayerSpecific() }
        storagePlayer = SkyHanniMod.storageData.players.getOrPut(playerUuid) { StorageData.PlayerSpecific() }
        petPlayers = SkyHanniMod.petData.players.getOrPut(playerUuid) { PetDataStorage.PlayerSpecific() }
        orderedWaypointsRoutes = SkyHanniMod.orderedWaypointsRoutesData
        ConfigLoadEvent.post()
    }

    @HandleEvent
    fun onWorldChange() {
        hypixelDataLoaded = false
    }

    fun profileJoinMessage() {
        hypixelDataLoaded = true
        playerSpecific?.multipleProfiles = true
    }
}
