package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.migration.LoadResult
import at.hannibal2.skyhanni.config.migration.MigratingConfigLoader
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.garden.CropType
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
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

    var lastFailures: List<LoadResult.Failure> = listOf()
        private set

    fun loadConfig(file: File): Features {
        val migrator = MigratingConfigLoader()
        val x = migrator.loadConfig(
            gson.fromJson(file.readText(), JsonElement::class.java),
            Features::class.java
        )
        lastFailures = migrator.allFailures
        if (migrator.hasAnyFailure()) {
            val backupFile = file.resolveSibling("config-${System.currentTimeMillis()}-backup.json")
            logger.error(
                "Error while reading $file. Will save backup to $backupFile"
            )
            try {
                file.copyTo(backupFile)
            } catch (e: Exception) {
                logger.error("Could not create backup for config file", e)
            }
        }
        return when (x) {
            LoadResult.UseDefault -> Features()
            is LoadResult.Instance -> x.instance!!
            is LoadResult.Failure -> Features()
            LoadResult.Invalid -> error("LoadResult.Invalid returned directly?")
        }
    }


    fun firstLoad() {
        try {
            configDirectory.mkdir()
        } catch (ignored: Exception) {
        }

        configFile = File(configDirectory, "config.json")

        logger.info("Trying to load config from $configFile")

        if (configFile!!.exists()) {
                SkyHanniMod.feature = loadConfig(configFile!!)
                logger.info("Loaded config from file")
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