package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.File
import java.util.*

class ApiDataLoader {
    private var currentProfileName = ""
    private var currentProfileId = ""

    private var usePlayerApiKey = false

    @SubscribeEvent
    fun onRepositoryReload(event: RepositoryReloadEvent) {
        usePlayerApiKey = false
        event.getConstant("DisabledFeatures")?.let {
            if (it.asJsonObject["user_api_keys"]?.asBoolean ?: false) {
                usePlayerApiKey = true
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        val thePlayer = Minecraft.getMinecraft().thePlayer ?: return
        thePlayer.worldObj ?: return
        if (!usePlayerApiKey) return

        if (nextApiCallTime != -1L && System.currentTimeMillis() > nextApiCallTime) {
            nextApiCallTime = System.currentTimeMillis() + 60_000 * 5
            SkyHanniMod.coroutineScope.launch {
                val apiKey = SkyHanniMod.feature.storage.apiKey
                val uuid = LorenzUtils.getPlayerUuid()
                loadProfileData(apiKey, uuid, currentProfileId)
            }
        }
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        currentProfileName = event.name
        if (!usePlayerApiKey) return
        updateApiData()
    }

    private suspend fun tryUpdateProfileDataAndVerifyKey(apiKey: String): Boolean {
        val uuid = LorenzUtils.getPlayerUuid()
        val url = "https://api.hypixel.net/player?key=$apiKey&uuid=$uuid"
        val jsonObject = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }

        if (jsonObject["success"]?.asBoolean == false) {
            if (jsonObject["throttle"]?.asBoolean == true) return true // 429 Too Many Requests doesn't make an invalid key.
            val cause = jsonObject["cause"].asString
            if (cause == "Invalid API key") {
                return false
            } else {
                throw RuntimeException("API error for url '$url': $cause")
            }
        }
        val player = jsonObject["player"].asJsonObject
        val stats = player["stats"].asJsonObject
        val skyBlock = stats["SkyBlock"].asJsonObject
        val profiles = skyBlock["profiles"].asJsonObject
        for (entry in profiles.entrySet()) {
            val asJsonObject = entry.value.asJsonObject
            val name = asJsonObject["cute_name"].asString
            if (currentProfileName == name.lowercase()) {
                currentProfileId = asJsonObject["profile_id"].asString
                loadProfileData(apiKey, uuid, currentProfileId)
            }
        }
        return true
    }

    private fun updateApiData() {
        nextApiCallTime = -1
        SkyHanniMod.coroutineScope.launch {
            val oldApiKey = SkyHanniMod.feature.storage.apiKey
            if (oldApiKey.isNotEmpty() && tryUpdateProfileDataAndVerifyKey(oldApiKey)) {
                return@launch
            }
            findApiCandidatesFromOtherMods().forEach { (modName, newApiKey) ->
                if (tryUpdateProfileDataAndVerifyKey(newApiKey)) {
                    SkyHanniMod.feature.storage.apiKey = newApiKey
                    LorenzUtils.chat("§e[SkyHanni] Imported valid API key from $modName.")
                    return@launch
                }
            }
        }
    }

    private fun findApiCandidatesFromOtherMods(): Map<String, String> {
        LorenzUtils.consoleLog("Trying to find the api key from the config of other mods..")
        val candidates = mutableMapOf<String, String>()
        for (mod in OtherMod.entries) {
            val modName = mod.modName
            val file = File(mod.configPath)
            if (file.exists()) {
                val reader = APIUtil.readFile(file)
                try {
                    val key = mod.readKey(reader).replace("\n", "").replace(" ", "")
                    if (key == "") {
                        LorenzUtils.consoleLog("- $modName: no api key set!")
                        continue
                    }
                    UUID.fromString(key)
                    candidates[modName] = key
                } catch (e: Throwable) {
                    LorenzUtils.consoleLog("- $modName: wrong config format! (" + e.message + ")")
                    continue
                }
            } else {
                LorenzUtils.consoleLog("- $modName: no mod/config found!")
            }
        }
        return candidates
    }

    private suspend fun loadProfileData(apiKey: String, playerUuid: String, profileId: String) {
        val url = "https://api.hypixel.net/skyblock/profile?key=$apiKey&profile=$profileId"

        val jsonObject = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }
        val profile = jsonObject["profile"]?.asJsonObject ?: return
        val members = profile["members"]?.asJsonObject ?: return
        for (entry in members.entrySet()) {
            if (entry.key == playerUuid) {
                val profileData = entry.value.asJsonObject
                ProfileApiDataLoadedEvent(profileData).postAndCatch()
                nextApiCallTime = System.currentTimeMillis() + 60_000 * 3
            }
        }
    }

    companion object {
        private var nextApiCallTime = -1L

        fun command(args: Array<String>) {
            SkyHanniMod.feature.storage.apiKey = args[0]
            LorenzUtils.chat("§e[SkyHanni] Api key set via command!")
            nextApiCallTime = -1
        }
    }
}
