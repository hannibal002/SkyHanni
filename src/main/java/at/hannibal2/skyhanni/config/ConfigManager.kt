package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.jsonobjects.FriendsJson
import at.hannibal2.skyhanni.utils.jsonobjects.JacobContestsJson
import at.hannibal2.skyhanni.utils.jsonobjects.KnownFeaturesJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.github.moulberry.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.moulberry.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.moulberry.moulconfig.processor.ConfigProcessorDriver
import io.github.moulberry.moulconfig.processor.MoulConfigProcessor
import net.minecraft.item.ItemStack
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
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
            .registerTypeAdapter(TrophyRarity::class.java, object : TypeAdapter<TrophyRarity>() {
                override fun write(out: JsonWriter, value: TrophyRarity) {
                    value.run { out.value(value.name) }
                }

                override fun read(reader: JsonReader): TrophyRarity {
                    val text = reader.nextString()
                    return TrophyRarity.getByName(text) ?: error("Could not parse TrophyRarity from '$text'")
                }
            }.nullSafe())
            .registerTypeAdapter(ItemStack::class.java, object : TypeAdapter<ItemStack>() {
                override fun write(out: JsonWriter, value: ItemStack) {
                    out.value(NEUItems.saveNBTData(value))
                }

                override fun read(reader: JsonReader): ItemStack {
                    return NEUItems.loadNBTData(reader.nextString())
                }
            }.nullSafe())
            .registerTypeAdapter(NEUInternalName::class.java, object : TypeAdapter<NEUInternalName>() {
                override fun write(out: JsonWriter, value: NEUInternalName) {
                    out.value(value.asString())
                }

                override fun read(reader: JsonReader): NEUInternalName {
                    return reader.nextString().asInternalName()
                }
            }.nullSafe())
            .registerTypeAdapter(LorenzRarity::class.java, object : TypeAdapter<LorenzRarity>() {
                override fun write(out: JsonWriter, value: LorenzRarity) {
                    out.value(value.name)
                }

                override fun read(reader: JsonReader): LorenzRarity {
                    return LorenzRarity.valueOf(reader.nextString())
                }
            }.nullSafe())
            .enableComplexMapKeySerialization()
            .create()
    }

    lateinit var features: Features
        private set
    lateinit var sackData: SackData
        private set
    lateinit var friendsData: FriendsJson
        private set
    lateinit var knownFeaturesData: KnownFeaturesJson
        private set
    lateinit var jacobContestData: JacobContestsJson
        private set

    private val logger = LorenzLogger("config_manager")

    var configDirectory = File("config/skyhanni")

    private var configFile: File? = null
    private var sackFile: File? = null
    private var friendsFile: File? = null
    private var knowFeaturesFile: File? = null
    private var jacobContestsFile: File? = null

    lateinit var processor: MoulConfigProcessor<Features>
    private var disableSaving = false

    fun firstLoad() {
        if (::features.isInitialized) {
            logger.log("Loading config despite config being already loaded?")
        }
        configDirectory.mkdir()

        configFile = File(configDirectory, "config.json")
        sackFile = File(configDirectory, "sacks.json")
        friendsFile = File(configDirectory, "friends.json")
        knowFeaturesFile = File(configDirectory, "known_features.json")
        jacobContestsFile = File(configDirectory, "jacob_contests.json")

        features = firstLoadFile(configFile, ConfigFileType.FEATURES, Features(), true)
        sackData = firstLoadFile(sackFile, ConfigFileType.SACKS, SackData(), false)
        friendsData = firstLoadFile(friendsFile, ConfigFileType.FRIENDS, FriendsJson(), false)
        knownFeaturesData = firstLoadFile(knowFeaturesFile, ConfigFileType.KNOWN_FEATURES, KnownFeaturesJson(), false)
        jacobContestData = firstLoadFile(jacobContestsFile, ConfigFileType.JACOB_CONTESTS, JacobContestsJson(), false)

        fixedRateTimer(name = "skyhanni-config-auto-save", period = 60_000L, initialDelay = 60_000L) {
            saveConfig(ConfigFileType.FEATURES, "auto-save-60s")
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

    private inline fun <reified T> firstLoadFile(file: File?, fileType: ConfigFileType, defaultValue: T, isConfig: Boolean): T {
        val fileName = fileType.fileName
        logger.log("Trying to load $fileName from $file")
        var output: T = defaultValue

        if (file!!.exists()) {
            try {
                val inputStreamReader = InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)
                val bufferedReader = BufferedReader(inputStreamReader)

                logger.log("load-$fileName-now")

                output = if (isConfig) {
                    val jsonObject = gson.fromJson(bufferedReader.readText(), JsonObject::class.java)
                    val newJsonObject = ConfigUpdaterMigrator.fixConfig(jsonObject)
                    gson.fromJson(newJsonObject, T::class.java)
                } else {
                    gson.fromJson(bufferedReader.readText(), T::class.java)
                }

                logger.log("Loaded $fileName from file")
            } catch (error: Exception) {
                error.printStackTrace()
                val backupFile = file.resolveSibling("$fileName-${System.currentTimeMillis()}-backup.json")
                logger.log("Exception while reading $file. Will load blank $fileName and save backup to $backupFile")
                logger.log("Exception was $error")
                try {
                    file.copyTo(backupFile)
                } catch (e: Exception) {
                    logger.log("Could not create backup for $fileName file")
                    e.printStackTrace()
                }
            }
        }

        if (output == defaultValue) {
            logger.log("Setting $fileName to be blank as it did not exist. It will be saved once something is written to it")
        }

        return output
    }

    fun saveConfig(fileType: ConfigFileType, reason: String) {
        when (fileType) {
            ConfigFileType.FEATURES -> saveFile(configFile, fileType.fileName, SkyHanniMod.feature, reason)
            ConfigFileType.SACKS -> saveFile(sackFile, fileType.fileName, SkyHanniMod.sackData, reason)
            ConfigFileType.FRIENDS -> saveFile(friendsFile, fileType.fileName, SkyHanniMod.friendsData, reason)
            ConfigFileType.KNOWN_FEATURES -> saveFile(knowFeaturesFile, fileType.fileName, SkyHanniMod.knownFeaturesData, reason)
            ConfigFileType.JACOB_CONTESTS -> saveFile(jacobContestsFile, fileType.fileName, SkyHanniMod.jacobContestsData, reason)
        }
    }

    private fun saveFile(file: File?, fileName: String, data: Any, reason: String) {
        if (disableSaving) return
        logger.log("saveConfig: $reason")
        if (file == null) throw Error("Can not save $fileName, ${fileName}File is null!")
        try {
            logger.log("Saving $fileName file")
            file.parentFile.mkdirs()
            val unit = file.parentFile.resolve("$fileName.json.write")
            unit.createNewFile()
            BufferedWriter(OutputStreamWriter(FileOutputStream(unit), StandardCharsets.UTF_8)).use { writer ->
                writer.write(gson.toJson(data))
            }
            // Perform move — which is atomic, unlike writing — after writing is done.
            Files.move(
                unit.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: IOException) {
            logger.log("Could not save $fileName file to $file")
            e.printStackTrace()
        }
    }

    fun disableSaving() {
        disableSaving = true
    }
}

enum class ConfigFileType(val fileName: String) {
    FEATURES("config"),
    SACKS("sacks"),
    FRIENDS("friends"),
    KNOWN_FEATURES("known_features"),
    JACOB_CONTESTS("jacob_contests"),
    ;
}
