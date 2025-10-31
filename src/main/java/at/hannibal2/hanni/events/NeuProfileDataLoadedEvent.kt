package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.HypixelData
import at.hannibal2.hanni.data.jsonobjects.other.HypixelPlayerApiJson
import at.hannibal2.hanni.utils.PlayerUtils

class NeuProfileDataLoadedEvent(val playerData: HypixelPlayerApiJson) : HanniEvent() {
    fun getCurrentProfileData() =
        playerData.profiles.firstOrNull { it.profileName.equals(HypixelData.profileName, ignoreCase = true) }

    fun getCurrentPlayerData() = getCurrentProfileData()?.members?.get(PlayerUtils.getUuid())
}
