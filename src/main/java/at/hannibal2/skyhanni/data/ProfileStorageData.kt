package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ProfileStorageData {
    var playerSpecific: Storage.PlayerSpecific? = null
    var profileSpecific: Storage.ProfileSpecific? = null
    var loaded = false
    private var noTabListTime = -1L

    private var nextProfile: String? = null

    // TODO USE SH-REPO
    private val profileSwitchPattern = "§7Switching to profile (?<name>.*)\\.\\.\\.".toPattern()

    private var sackPlayers: SackData.PlayerSpecific? = null
    var sackProfiles: SackData.ProfileSpecific? = null

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onChat(event: LorenzChatEvent) {
        profileSwitchPattern.matchMatcher(event.message) {
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
        val sackPlayers = sackPlayers
        if (playerSpecific == null) {
            LorenzUtils.error("profileSpecific after profile swap can not be set: playerSpecific is null!")
            return
        }
        if (sackPlayers == null) {
            LorenzUtils.error("sackPlayers after profile swap can not be set: sackPlayers is null!")
            return
        }
        loadProfileSpecific(playerSpecific, sackPlayers, profileName)
        ConfigLoadEvent().postAndCatch()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onProfileJoin(event: ProfileJoinEvent) {
        val playerSpecific = playerSpecific
        val sackPlayers = sackPlayers
        if (playerSpecific == null) {
            LorenzUtils.error("playerSpecific is null in ProfileJoinEvent!")
            return
        }
        if (sackPlayers == null) {
            LorenzUtils.error("sackPlayers is null in sackPlayers!")
            return
        }

        if (profileSpecific == null) {
            val profileName = event.name
            loadProfileSpecific(playerSpecific, sackPlayers, profileName)
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (profileSpecific != null) return
        val playerSpecific = playerSpecific ?: return
        val sackPlayers = sackPlayers ?: return
        for (line in event.tabList) {
            val pattern = "§e§lProfile: §r§a(?<name>.*)".toPattern()
            pattern.matchMatcher(line) {
                val profileName = group("name").lowercase()
                loadProfileSpecific(playerSpecific, sackPlayers, profileName)
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

    private fun loadProfileSpecific(
        playerSpecific: Storage.PlayerSpecific,
        sackProfile: SackData.PlayerSpecific,
        profileName: String
    ) {
        noTabListTime = -1
        profileSpecific = playerSpecific.profiles.getOrPut(profileName) { Storage.ProfileSpecific() }
        sackProfiles = sackProfile.profiles.getOrPut(profileName) { SackData.ProfileSpecific() }
        loaded = true
        ConfigLoadEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        val playerUuid = LorenzUtils.getRawPlayerUuid()
        playerSpecific = SkyHanniMod.feature.storage.players.getOrPut(playerUuid) { Storage.PlayerSpecific() }
        sackPlayers = SkyHanniMod.sackData.players.getOrPut(playerUuid) { SackData.PlayerSpecific() }
        ConfigLoadEvent().postAndCatch()
    }
}
