package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.File
import java.util.concurrent.Executors

class ApiKeyGrabber {

    private var currentProfileName = ""
    private val executors = Executors.newFixedThreadPool(1)


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
        executors.submit {
            val uuid = Minecraft.getMinecraft().thePlayer.uniqueID.toString().replace("-", "")

            var apiKey = SkyHanniMod.feature.hidden.apiKey
            if (!verifyKey(apiKey)) {
                LorenzUtils.chat("§c[SkyHanni] Invalid api key detected, deleting it!")
                apiKey = ""
                SkyHanniMod.feature.hidden.apiKey = ""
            }

            if (apiKey.isEmpty()) {
                readApiKeyFromOtherMods()
                apiKey = SkyHanniMod.feature.hidden.apiKey
                if (apiKey.isEmpty()) {
                    LorenzUtils.warning("SkyHanni has no API Key set. Type /api new to reload.")
                    return@submit
                }
            }

            val url = "https://api.hypixel.net/player?key=$apiKey&uuid=$uuid"

            val jsonObject = APIUtil.getJSONResponse(url)

            if (!jsonObject["success"].asBoolean) {
                val cause = jsonObject["cause"].asString
                if (cause == "Invalid API key") {
                    LorenzUtils.error("SkyHanni got an API error: Invalid API key! Type /api new to reload.")
                    return@submit
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
                    return@submit
                }
            }
        }
    }

    private fun readApiKeyFromOtherMods() {
        LorenzUtils.consoleLog("Trying to find the API Key from the config of other mods..")

        var found = false
        for (mod in OtherMod.values()) {
            val modName = mod.modName
            val file = File(mod.configPath)
            if (file.exists()) {
                val reader = APIUtil.readFile(file)
                try {
                    val key = mod.readKey(reader).replace("\n", "").replace(" ", "")
                    if (verifyKey(key)) {
                        LorenzUtils.consoleLog("- $modName: good key!")
                        if (!found) {
                            found = true
                            LorenzUtils.chat("§e[SkyHanni] Grabbed the API key from $modName!")
                            SkyHanniMod.feature.hidden.apiKey = key
                        }
                    } else {
                        LorenzUtils.consoleLog("- $modName: wrong key!")
                    }
                } catch (e: Throwable) {
                    LorenzUtils.consoleLog("- $modName: wrong config format! (" + e.message + ")")
                    continue
                }
            } else {
                LorenzUtils.consoleLog("- $modName: no config found!")
            }
        }
    }

    private fun verifyKey(key: String): Boolean {
        return try {
            val url = "https://api.hypixel.net/key?key=$key"
            val bazaarData = APIUtil.getJSONResponse(url, silentError = true)
            return bazaarData.get("success").asBoolean
        } catch (e: Throwable) {
            e.printStackTrace()
            false
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