package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.migration.LoadResult
import at.hannibal2.skyhanni.config.migration.MigratingConfigLoader
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
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

    var lastFailures: List<LoadResult.Failure> = listOf()
        private set

    fun loadConfig(file: File): Features? {
        val migrator = MigratingConfigLoader()
        val x = migrator.loadConfig(
            gson.fromJson(file.readText(), JsonElement::class.java),
            Features::class.java
        )
        lastFailures = migrator.allFailures
        if (migrator.hasAnyFailure()) {
            val backupFile = file.resolveSibling("config-${System.currentTimeMillis()}-backup.json")
            logger.error("Error while reading $file. Will save backup to $backupFile")
            try {
                file.copyTo(backupFile)
            } catch (e: Exception) {
                logger.error("Could not create backup for config file", e)
            }
        }
        return when (x) {
            LoadResult.UseDefault -> null
            is LoadResult.Instance -> x.instance
            is LoadResult.Failure -> null
            LoadResult.Invalid -> error("LoadResult.Invalid returned directly?")
        }
    }


    fun firstLoad() {
        configDirectory.mkdir()

        configFile = File(configDirectory, "config.json")

        fixedRateTimer(name = "config-auto-save", period = 60_000L, initialDelay = 60_000L) {
            saveConfig("auto-save-60s")
        }

        logger.info("Trying to load config from $configFile")

        if (configFile!!.exists()) {
            SkyHanniMod.feature = loadConfig(configFile!!)
            logger.info("Loaded config from file")
        }

        if (SkyHanniMod.feature == null) {
            logger.warn("Creating blank config and saving to file")
            SkyHanniMod.feature = Features()
            saveConfig("blank config")
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

    fun saveConfig(reason: String) {
        logger.info("Saving config: $reason")
        val file = configFile ?: throw Error("Can not save config, configFile is null!")
        try {
            logger.warn("Saving config file")
            file.parentFile.mkdirs()
            file.createNewFile()
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { writer ->
                writer.write(gson.toJson(SkyHanniMod.feature))
            }
        } catch (e: IOException) {
            logger.error("Could not save config file to $file", e)
        }
    }
}