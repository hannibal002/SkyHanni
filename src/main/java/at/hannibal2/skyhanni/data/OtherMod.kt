package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.config.ConfigManager
import com.google.gson.JsonObject
import java.io.BufferedReader

enum class OtherMod(val modName: String, val configPath: String, val readKey: (BufferedReader) -> (String)) {
    NEU("Not Enough Updates", "config/notenoughupdates/configNew.json", { reader ->
        getJson(reader)["apiData"].asJsonObject["apiKey"].asString
    }),
    COW("Cowlection", "config/cowlection/do-not-share-me-with-other-players.cfg", { reader ->
        val lines = reader.readText().split(System.lineSeparator())
        val line = lines.find { it.startsWith("    S:moo=") }!!
        line.split("=")[1]
    }),
    DSM("Dankers SkyBlock Mod", "config/Danker's Skyblock Mod.cfg", { reader ->
        val lines = reader.readText().split(System.lineSeparator())
        val line = lines.find { it.startsWith("    S:APIKey=") }!!
        line.split("=")[1]
    }),
    DG("Dungeons Guide", "config/dungeonsguide/config.json", { reader ->
        getJson(reader)["partykicker.apikey"].asJsonObject["apikey"].asString
    }),
    SKYTILS("Skytils", "config/skytils/config.toml", { reader ->
        val lines = reader.readText().split(System.lineSeparator())
        val line = lines.find { it.startsWith("		hypixel_api_key = \"") }!!
        line.split("\"")[1]
    }),
    HYPIXEL_API_KEY_MANAGER("Hypixel API Key Manager", "HypixelApiKeyManager/localdata.json", { reader ->
        getJson(reader)["key"].asString
    }),
    SOOPY("Soopy Addons", "soopyAddonsData/apikey.txt", { reader ->
        reader.readText()
    }),
    SBE("SkyBlock Extras", "config/SkyblockExtras.cfg", { reader ->
        getJson(reader)["values"].asJsonObject["apiKey"].asString
    }),
}

fun getJson(reader: BufferedReader): JsonObject {
    return ConfigManager.gson.fromJson(reader, com.google.gson.JsonObject::class.java)
}