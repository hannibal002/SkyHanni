package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ProfileStorageData {

    var playerSpecific: Storage.PlayerSpecific? = null
    var profileSpecific: Storage.ProfileSpecific? = null
    var loaded = false
    private var noTabListTime = SimpleTimeMark.farPast()

    private var nextProfile: String? = null

    private val patternGroup = RepoPattern.group("data.profile")
    private val profileSwitchPattern by patternGroup.pattern(
        "switch",
        "§7Switching to profile (?<name>.*)\\.\\.\\."
    )

    private var sackPlayers: SackData.PlayerSpecific? = null
    var sackProfiles: SackData.ProfileSpecific? = null

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onProfileJoin(event: ProfileJoinEvent) {
        val playerSpecific = playerSpecific
        val sackPlayers = sackPlayers
        if (playerSpecific == null) {
            ChatUtils.error("playerSpecific is null in ProfileJoinEvent!")
            return
        }
        if (sackPlayers == null) {
            ChatUtils.error("sackPlayers is null in ProfileJoinEvent!")
            return
        }

        val profileName = event.name
        loadProfileSpecific(playerSpecific, sackPlayers, profileName)
        ConfigLoadEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!LorenzUtils.inSkyBlock) return

        for (line in event.tabList) {
            UtilsPatterns.tabListProfilePattern.matchMatcher(line) {
                noTabListTime = SimpleTimeMark.farPast()
                return
            }
        }

        noTabListTime = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (noTabListTime == SimpleTimeMark.farPast()) return

        if (noTabListTime.passedSince() > 3.seconds) {
            noTabListTime = SimpleTimeMark.now()
            ChatUtils.chat(
                "Extra Information from Tab list not found! " +
                    "Enable it: SkyBlock Menu ➜ Settings ➜ Personal ➜ User Interface ➜ Player List Info"
            )
        }
    }

    private fun loadProfileSpecific(
        playerSpecific: Storage.PlayerSpecific,
        sackProfile: SackData.PlayerSpecific,
        profileName: String,
    ) {
        noTabListTime = SimpleTimeMark.farPast()
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
