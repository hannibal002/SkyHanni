package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.storage.PlayerSpecificStorage
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.UUID

object ProfileStorageData {

    var playerSpecific: PlayerSpecificStorage? = null
    var profileSpecific: ProfileSpecificStorage? = null
    val loaded get() = profileSpecific != null
    private var profileId: UUID? = null
    private var lastChatProfile: String? = null

    private var sackPlayers: SackData.PlayerSpecific? = null
    var sackProfiles: SackData.ProfileSpecific? = null

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val inSkyBlock = LorenzUtils.inSkyBlock
        if (!inSkyBlock) return
        val message = event.message

        "§aYou are playing on profile: §e(?<name>.*)§b( \\(Co-op\\))?".toPattern().matchMatcher(message) {
            lastChatProfile = group("name")
        }

        "§8Profile ID: (?<uuid>.*)".toPattern().matchMatcher(message) {
            load(UUID.fromString(group("uuid")))
        }
    }

    private fun load(uuid: UUID) {
        val playerSpecific = playerSpecific ?: ErrorManager.skyHanniError("Can not load user profile", "uuid" to uuid)
        if (profileId == uuid) {
            profileSpecific = playerSpecific.uuidProfiles[uuid]
            return
        }

        profileId = uuid
        profileSpecific = loadProfile(playerSpecific, uuid)

        ConfigLoadEvent().postAndCatch()
    }

    private fun loadProfile(
        playerSpecific: PlayerSpecificStorage,
        uuid: UUID,
    ): ProfileSpecificStorage? {
        val newProfile = playerSpecific.uuidProfiles.getOrPut(uuid) { ProfileSpecificStorage() }
        if (!newProfile.migrated) {
            val lastProfile = lastChatProfile ?: getProfileFromTab()
            for ((name, data) in playerSpecific.profiles) {
                if (name.equals(lastProfile, ignoreCase = true)) {
                    ChatUtils.debug("successfully migrated!")
                    playerSpecific.uuidProfiles[uuid] = data
                    profileSpecific = data
                    data.migrated = true
                    return data
                }
            }
        }
        return newProfile
    }

    private fun getProfileFromTab(): String? {
        val pattern = "§l§r§e§lProfile: §r§a(?<name>.*)".toPattern()
        for (line in TabListData.getTabList()) {
            pattern.matchMatcher(line) {
                return group("name")
            }
        }

        return null
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        lastChatProfile = null
        profileSpecific = null
    }

    @SubscribeEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        val playerUuid = LorenzUtils.getRawPlayerUuid()
        playerSpecific = SkyHanniMod.feature.storage.players.getOrPut(playerUuid) { PlayerSpecificStorage() }
        sackPlayers = SkyHanniMod.sackData.players.getOrPut(playerUuid) { SackData.PlayerSpecific() }
        ConfigLoadEvent().postAndCatch()
    }
}
