package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import com.google.gson.GsonBuilder
import io.github.moulberry.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.moulberry.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.moulberry.moulconfig.processor.ConfigProcessorDriver
import io.github.moulberry.moulconfig.processor.MoulConfigProcessor
import java.io.*
import java.nio.charset.StandardCharsets
import kotlin.concurrent.fixedRateTimer

class ConfigManager {
    companion object {
        val gson = GsonBuilder().setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
            .create()
    }

    val logger = SkyHanniMod.getLogger("ConfigManager")

    var configDirectory = File("config/skyhanni")
    private var configFile: File? = null
    lateinit var processor: MoulConfigProcessor<Features>

    fun firstLoad() {
        fixedRateTimer(name = "config-auto-save", period = 60_000L) {
            saveConfig()
        }

        try {
            configDirectory.mkdir()
        } catch (ignored: Exception) {
        }

        configFile = File(configDirectory, "config.json")

        logger.info("Trying to load config from $configFile")

        if (configFile!!.exists()) {
            try {
                val inputStreamReader = InputStreamReader(FileInputStream(configFile!!), StandardCharsets.UTF_8)
                val bufferedReader = BufferedReader(inputStreamReader)
                val builder = StringBuilder()
                for (line in bufferedReader.lines()) {
                    val result = fixConfig(line)
                    builder.append(result)
                    builder.append("\n")
                }


                SkyHanniMod.feature = gson.fromJson(
                    builder.toString(),
                    Features::class.java
                )
                logger.info("Loaded config from file")
            } catch (e: Exception) {
                println("config error")
                e.printStackTrace()
                val backupFile = configFile!!.resolveSibling("config-${System.currentTimeMillis()}-backup.json")
                logger.error(
                    "Exception while reading $configFile. Will load blank config and save backup to $backupFile",
                    e
                )
                try {
                    configFile!!.copyTo(backupFile)
                } catch (e: Exception) {
                    logger.error("Could not create backup for config file", e)
                }
            }
        }

        if (SkyHanniMod.feature == null) {
            logger.info("Creating blank config and saving to file")
            SkyHanniMod.feature = Features()
            saveConfig()
        }

        ConfigLoadEvent().postAndCatch()

        val features = SkyHanniMod.feature
        processor = MoulConfigProcessor(SkyHanniMod.feature)
        BuiltinMoulConfigGuis.addProcessors(processor)
        UpdateManager.injectConfigProcessor(processor)
        ConfigProcessorDriver.processConfig(
            features.javaClass,
            features,
            processor
        )
    }

    private fun fixConfig(line: String): String {
        var result = line
        for (type in CropType.values()) {
            val normal = "\"${type.cropName}\""
            val enumName = "\"${type.name}\""
            while (result.contains(normal)) {
                result = result.replace(normal, enumName)
            }
        }
        return result
    }

    fun saveConfig() {
        try {
            logger.info("Saving config file")
            configFile!!.parentFile.mkdirs()
            configFile!!.createNewFile()
            BufferedWriter(OutputStreamWriter(FileOutputStream(configFile!!), StandardCharsets.UTF_8)).use { writer ->
                writer.write(gson.toJson(SkyHanniMod.feature))
            }
        } catch (e: IOException) {
            logger.error("Could not save config file to $configFile", e)
        }
    }
}