package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import com.google.gson.GsonBuilder
import io.github.moulberry.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.moulberry.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.moulberry.moulconfig.processor.ConfigProcessorDriver
import io.github.moulberry.moulconfig.processor.MoulConfigProcessor
import java.io.*
import java.nio.charset.StandardCharsets

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
        try {
            configDirectory.mkdir()
        } catch (ignored: Exception) {
        }

        configFile = File(configDirectory, "config.json")

        logger.info("Trying to load config from $configFile")

        if (configFile!!.exists()) {
            try {
                BufferedReader(InputStreamReader(FileInputStream(configFile!!), StandardCharsets.UTF_8)).use { reader ->
                    SkyHanniMod.feature = gson.fromJson(
                        reader,
                        Features::class.java
                    )
                }
                logger.info("Loaded config from file")
            } catch (e: Exception) {
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
        ConfigProcessorDriver.processConfig(
            features.javaClass,
            features,
            processor
        )
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