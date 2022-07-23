package at.hannibal2.skyhanni.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ApiData {

    private var currentProfileName = ""

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        val message = event.message
        if (message.startsWith("§aYour new API key is §r§b")) {
            SkyHanniMod.feature.hidden.apiKey = message.substring(26)
            LorenzUtils.chat("§b[SkyHanni] A new API Key has been detected and installed")

            if (currentProfileName != "") {
                updateApiData()
            }
        }
    }

    @SubscribeEvent
    fun onStatusBar(event: ProfileJoinEvent) {
        currentProfileName = event.name
        updateApiData()
    }

    private fun updateApiData() {
        val uuid = Minecraft.getMinecraft().thePlayer.uniqueID.toString().replace("-", "")

        val apiKey = SkyHanniMod.feature.hidden.apiKey

        if (apiKey.isEmpty()) {
            LorenzUtils.error("SkyHanni has no API Key set. Type /api new to reload.")
            return
        }

        val url = "https://api.hypixel.net/player?key=$apiKey&uuid=$uuid"

        val jsonObject = APIUtil.getJSONResponse(url)

        if (!jsonObject["success"].asBoolean) {
            val cause = jsonObject["cause"].asString
            if (cause == "Invalid API key") {
                LorenzUtils.error("SkyHanni got an API error: Invalid API key! Type /api new to reload.")
                return
            } else {
                throw RuntimeException("API error for url '$url': $cause")
            }
        }

        val player = jsonObject["player"].asJsonObject
        val stats = player["stats"].asJsonObject
        val skyblock = stats["SkyBlock"].asJsonObject
        val profiles = skyblock["profiles"].asJsonObject
        for (entry in profiles.entrySet()) {
            val asJsonObject = entry.value.asJsonObject
            val name = asJsonObject["cute_name"].asString
            val profileId = asJsonObject["profile_id"].asString
            if (currentProfileName == name.lowercase()) {
                loadProfile(uuid, profileId)
                return
            }
        }
    }

    private fun loadProfile(playerUuid: String, profileId: String) {
        val apiKey = SkyHanniMod.feature.hidden.apiKey
        val url = "https://api.hypixel.net/skyblock/profile?key=$apiKey&profile=$profileId"

        val jsonObject = APIUtil.getJSONResponse(url)

        val profile = jsonObject["profile"].asJsonObject
        val members = profile["members"].asJsonObject
        for (entry in members.entrySet()) {
            if (entry.key == playerUuid) {
                val profileData = entry.value.asJsonObject
                ProfileApiDataLoadedEvent(profileData).postAndCatch()

            }
        }
    }
}