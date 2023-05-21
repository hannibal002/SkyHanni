package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.github.moulberry.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.moulberry.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.moulberry.moulconfig.processor.ConfigProcessorDriver
import io.github.moulberry.moulconfig.processor.MoulConfigProcessor
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.concurrent.fixedRateTimer

class ConfigManager {
    companion object {
        val gson = GsonBuilder().setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
            .registerTypeAdapter(UUID::class.java, object : TypeAdapter<UUID>() {
                override fun write(out: JsonWriter, value: UUID) {
                    out.value(value.toString())
                }

                override fun read(reader: JsonReader): UUID {
                    return UUID.fromString(reader.nextString())
                }
            }.nullSafe())
            .registerTypeAdapter(LorenzVec::class.java, object : TypeAdapter<LorenzVec>() {
                override fun write(out: JsonWriter, value: LorenzVec) {
                    value.run { out.value("$x:$y:$z") }
                }

                override fun read(reader: JsonReader): LorenzVec {
                    val (x, y, z) = reader.nextString().split(":").map { it.toDouble() }
                    return LorenzVec(x, y, z)
                }
            }.nullSafe())
            .enableComplexMapKeySerialization()
            .create()
    }

    lateinit var features: Features
        private set
    private val logger = LorenzLogger("config_manager")

    var configDirectory = File("config/skyhanni")
    private var configFile: File? = null
    lateinit var processor: MoulConfigProcessor<Features>

    fun firstLoad() {
        if (::features.isInitialized) {
            logger.log("Loading config despite config being already loaded?")
        }
        configDirectory.mkdir()

        configFile = File(configDirectory, "config.json")

        fixedRateTimer(name = "skyhanni-config-auto-save", period = 60_000L, initialDelay = 60_000L) {
            saveConfig("auto-save-60s")
        }

        logger.log("Trying to load config from $configFile")

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


                logger.log("load-config-now")
                var noMigrationNeeded = false
                if (!gson.toJsonTree(builder.toString()).asJsonObject.has("hidden")) {
                    println("no hidden area, migration not necessary!")
                    noMigrationNeeded = true
                }
                features = gson.fromJson(
                    builder.toString(),
                    Features::class.java
                )
                if (noMigrationNeeded) {
                    features.hidden.isMigrated = true
                }
                logger.log("Loaded config from file")
            } catch (error: Exception) {
                error.printStackTrace()
                val backupFile = configFile!!.resolveSibling("config-${System.currentTimeMillis()}-backup.json")
                logger.log("Exception while reading $configFile. Will load blank config and save backup to $backupFile")
                logger.log("Exception was $error")
                try {
                    configFile!!.copyTo(backupFile)
                } catch (e: Exception) {
                    logger.log("Could not create backup for config file")
                    e.printStackTrace()
                }
            }
        }

        if (!::features.isInitialized) {
            logger.log("Creating blank config and saving to file")
            features = Features()
            saveConfig("blank config")
        }

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

    fun saveConfig(reason: String) {
        logger.log("saveConfig: $reason")
        val file = configFile ?: throw Error("Can not save config, configFile is null!")
        try {
            logger.log("Saving config file")
            file.parentFile.mkdirs()
            file.createNewFile()
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { writer ->
                // TODO remove old "hidden" area
                val jsonObject = gson.toJsonTree(SkyHanniMod.feature).asJsonObject
                jsonObject.remove("hidden")
                jsonObject?.getAsJsonObject("storage")?.remove("gardenJacobFarmingContestTimes")
                writer.write(gson.toJson(jsonObject))
            }
        } catch (e: IOException) {
            logger.log("Could not save config file to $file")
            e.printStackTrace()
        }
    }
}