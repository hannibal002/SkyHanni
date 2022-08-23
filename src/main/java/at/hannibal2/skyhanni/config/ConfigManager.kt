package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import com.google.gson.GsonBuilder
import java.io.*
import java.nio.charset.StandardCharsets

class ConfigManager(val mod: SkyHanniMod) {
    companion object {
        val gson = GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create()
    }

    var configDirectory = File("config/skyhanni")
    private var configFile: File? = null


    fun firstLoad() {
        try {
            configDirectory.mkdir()
        } catch (ignored: Exception) {
        }

        configFile = File(configDirectory, "config.json")

        if (configFile!!.exists()) {
            try {
                BufferedReader(InputStreamReader(FileInputStream(configFile!!), StandardCharsets.UTF_8)).use { reader ->
                    SkyHanniMod.feature = gson.fromJson(reader,
                        Features::class.java)
                }
                ConfigLoadEvent().postAndCatch()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (SkyHanniMod.feature == null) {
            SkyHanniMod.feature = Features()
            saveConfig()
        }
    }

    fun saveConfig() {
        try {
            configFile!!.createNewFile()
            BufferedWriter(OutputStreamWriter(FileOutputStream(configFile!!), StandardCharsets.UTF_8)).use { writer ->
                writer.write(gson.toJson(SkyHanniMod.feature))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}