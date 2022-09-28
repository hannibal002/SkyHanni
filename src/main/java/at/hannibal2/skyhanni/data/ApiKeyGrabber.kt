package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.toDashlessUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.File
import java.util.*

class ApiKeyGrabber {

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


    private suspend fun tryUpdateProfileDataAndVerifyKey(apiKey: String): Boolean {
        val uuid = Minecraft.getMinecraft().thePlayer.uniqueID.toDashlessUUID()
        val url = "https://api.hypixel.net/player?key=$apiKey&uuid=$uuid"
        val jsonObject = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }

        if (jsonObject["success"]?.asBoolean == false) {
            if (jsonObject["throttle"]?.asBoolean == true) return true // 429 Too Many Requests does not make an invalid key.
            val cause = jsonObject["cause"].asString
            if (cause == "Invalid API key") {
                return false
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
            if (currentProfileName == name.lowercase()) {
                val profileId = asJsonObject["profile_id"].asString
                loadProfile(apiKey, uuid, profileId)
            }
        }
        return true
    }

    private fun updateApiData() {
        SkyHanniMod.coroutineScope.launch {
            val oldApiKey = SkyHanniMod.feature.hidden.apiKey
            if (oldApiKey.isNotEmpty() && tryUpdateProfileDataAndVerifyKey(oldApiKey)) {
                return@launch
            }
            findApiCandidatesFromOtherMods().forEach { (modName, newApiKey) ->
                if (tryUpdateProfileDataAndVerifyKey(newApiKey)) {
                    SkyHanniMod.feature.hidden.apiKey = newApiKey
                    LorenzUtils.chat("§e[SkyHanni] Imported valid new API key from $modName.")
                    return@launch
                } else {
                    LorenzUtils.error("§c[SkyHanni] Invalid API key from $modName")
                }
            }
            LorenzUtils.error("§c[SkyHanni] SkyHanni has no API key set. Please run /api new")
        }
    }

    private fun findApiCandidatesFromOtherMods(): Map<String, String> {
        LorenzUtils.consoleLog("Trying to find the API Key from the config of other mods..")
        val candidates = mutableMapOf<String, String>()
        for (mod in OtherMod.values()) {
            val modName = mod.modName
            val file = File(mod.configPath)
            if (file.exists()) {
                val reader = APIUtil.readFile(file)
                try {
                    val key = mod.readKey(reader).replace("\n", "").replace(" ", "")
                    UUID.fromString(key)
                    candidates[modName] = key
                } catch (e: Throwable) {
                    LorenzUtils.consoleLog("- $modName: wrong config format! (" + e.message + ")")
                    continue
                }
            } else {
                LorenzUtils.consoleLog("- $modName: no config found!")
            }
        }
        return candidates
    }

    private suspend fun loadProfile(apiKey: String, playerUuid: String, profileId: String) {
        val url = "https://api.hypixel.net/skyblock/profile?key=$apiKey&profile=$profileId"

        val jsonObject = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }

        val profile = jsonObject["profile"]?.asJsonObject ?: return
        val members = profile["members"]?.asJsonObject ?: return
        for (entry in members.entrySet()) {
            if (entry.key == playerUuid) {
                val profileData = entry.value.asJsonObject
                ProfileApiDataLoadedEvent(profileData).postAndCatch()
            }
        }
    }
}