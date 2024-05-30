package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.local.FriendsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.JacobContestsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.KnownFeaturesJson
import at.hannibal2.skyhanni.data.jsonobjects.local.VisualWordsJson
import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.IdentityCharacteristics
import at.hannibal2.skyhanni.utils.KotlinTypeAdapterFactory
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
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

typealias TrackerDisplayMode = SkyHanniTracker.DefaultDisplayMode

private fun GsonBuilder.reigsterIfBeta(create: TypeAdapterFactory): GsonBuilder {
    return if (LorenzUtils.isBetaVersion()) {
        registerTypeAdapterFactory(create)
    } else this
}

class ConfigManager {
    companion object {
        fun createBaseGsonBuilder(): GsonBuilder {
            return GsonBuilder().setPrettyPrinting()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeSpecialFloatingPointValues()
                .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
                .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
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
                        return LorenzVec.decodeFromString(reader.nextString())
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
                        return LorenzRarity.valueOf(reader.nextString().uppercase().replace(" ", "_"))
                    }
                }.nullSafe())
                .registerTypeAdapter(IslandType::class.java, object : TypeAdapter<IslandType>() {
                    override fun write(out: JsonWriter, value: IslandType) {
                        out.value(value.name)
                    }

                    override fun read(reader: JsonReader): IslandType {
                        return IslandType.valueOf(reader.nextString().uppercase())
                    }
                }.nullSafe())
                .registerTypeAdapter(TrackerDisplayMode::class.java, object : TypeAdapter<TrackerDisplayMode>() {
                    override fun write(out: JsonWriter, value: TrackerDisplayMode) {
                        out.value(value.name)
                    }

                    override fun read(reader: JsonReader): TrackerDisplayMode {
                        return TrackerDisplayMode.valueOf(reader.nextString())
                    }
                }.nullSafe())
                .registerTypeAdapter(SimpleTimeMark::class.java, object : TypeAdapter<SimpleTimeMark>() {
                    override fun write(out: JsonWriter, value: SimpleTimeMark) {
                        out.value(value.toMillis())
                    }

                    override fun read(reader: JsonReader): SimpleTimeMark {
                        return reader.nextString().toLong().asTimeMark()
                    }
                }.nullSafe())
                .enableComplexMapKeySerialization()
        }

        val gson: Gson = createBaseGsonBuilder()
            // TODO reenable with toggle that is default disabled
//             .reigsterIfBeta(FeatureTogglesByDefaultAdapter)
            .create()

        var configDirectory = File("config/skyhanni")

        inline fun <reified T> GsonBuilder.registerTypeAdapter(
            crossinline write: (JsonWriter, T) -> Unit,
            crossinline read: (JsonReader) -> T,
        ): GsonBuilder {
            this.registerTypeAdapter(T::class.java, object : TypeAdapter<T>() {
                override fun write(out: JsonWriter, value: T) = write(out, value)
                override fun read(reader: JsonReader) = read(reader)
            }.nullSafe())
            return this
        }
    }

    val features get() = jsonHolder[ConfigFileType.FEATURES] as Features
    val sackData get() = jsonHolder[ConfigFileType.SACKS] as SackData
    val friendsData get() = jsonHolder[ConfigFileType.FRIENDS] as FriendsJson
    val knownFeaturesData get() = jsonHolder[ConfigFileType.KNOWN_FEATURES] as KnownFeaturesJson
    val jacobContestData get() = jsonHolder[ConfigFileType.JACOB_CONTESTS] as JacobContestsJson
    val visualWordsData get() = jsonHolder[ConfigFileType.VISUAL_WORDS] as VisualWordsJson

    private val logger = LorenzLogger("config_manager")

    private val jsonHolder = mutableMapOf<ConfigFileType, Any>()

    lateinit var processor: MoulConfigProcessor<Features>
    private var disableSaving = false

    fun firstLoad() {
        if (jsonHolder.isNotEmpty()) {
            logger.log("Loading config despite config being already loaded?")
        }
        configDirectory.mkdirs()


        for (fileType in ConfigFileType.entries) {
            jsonHolder[fileType] = firstLoadFile(fileType.file, fileType, fileType.clazz.newInstance())
        }

        // TODO use SecondPassedEvent
        fixedRateTimer(name = "skyhanni-config-auto-save", period = 60_000L, initialDelay = 60_000L) {
            saveConfig(ConfigFileType.FEATURES, "auto-save-60s")
        }

        val features = SkyHanniMod.feature
        processor = MoulConfigProcessor(SkyHanniMod.feature)
        BuiltinMoulConfigGuis.addProcessors(processor)
        UpdateManager.injectConfigProcessor(processor)
        ConfigProcessorDriver(processor).processConfig(features)

        try {
            findPositionLinks(features, mutableSetOf())
        } catch (e: Exception) {
            if (LorenzEvent.isInGuardedEventHandler)
                throw e
        }
    }

    // Some position elements don't need config links as they don't have a config option.
    private val ignoredMissingConfigLinks = listOf(
        // commands
        "features.garden.GardenConfig.cropSpeedMeterPos",
        "features.misc.MiscConfig.collectionCounterPos",
        "features.misc.MiscConfig.lockedMouseDisplay",

        // debug features
        "features.dev.DebugConfig.trackSoundPosition",
        "features.dev.DebugConfig.trackParticlePosition",
        "features.dev.DevConfig.debugPos",
        "features.dev.DevConfig.debugLocationPos",
        "features.dev.DevConfig.debugItemPos",
    )

    private fun findPositionLinks(obj: Any?, slog: MutableSet<IdentityCharacteristics<Any>>) {
        if (obj == null) return
        if (!obj.javaClass.name.startsWith("at.hannibal2.skyhanni.")) return
        val ic = IdentityCharacteristics(obj)
        if (ic in slog) return
        slog.add(ic)
        var missingConfigLink = false
        for (field in obj.javaClass.fields) {
            field.isAccessible = true
            if (field.type != Position::class.java) {
                findPositionLinks(field.get(obj), slog)
                continue
            }
            val configLink = field.getAnnotation(ConfigLink::class.java)
            if (configLink == null) {
                if (LorenzUtils.isInDevEnvironment()) {
                    var name = "${field.declaringClass.name}.${field.name}"
                    name = name.replace("at.hannibal2.skyhanni.config.", "")
                    if (name !in ignoredMissingConfigLinks) {
                        println("WEE WOO WEE WOO HIER FEHLT EIN @CONFIGLINK: $name")
                        missingConfigLink = true
                    }
                }
                continue
            }
            val position = field.get(obj) as Position
            position.setLink(configLink)
        }
        if (missingConfigLink) {
            println("")
            println("This crash is here to remind you to fix the missing @ConfigLink annotation over your new config position config element.")
            println("")
            println("Steps to fix:")
            println("1. Search for `WEE WOO WEE WOO` in the console output.")
            println("2. Either add the Config Link.")
            println("3. Or add the name to ignoredMissingConfigLinks.")
            println("")
            LorenzUtils.shutdownMinecraft("Missing Config Link")
        }
    }

    private fun firstLoadFile(file: File?, fileType: ConfigFileType, defaultValue: Any): Any {
        val fileName = fileType.fileName
        logger.log("Trying to load $fileName from $file")
        var output: Any = defaultValue

        if (file!!.exists()) {
            try {
                val inputStreamReader = InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)
                val bufferedReader = BufferedReader(inputStreamReader)

                logger.log("load-$fileName-now")

                output = if (fileType == ConfigFileType.FEATURES) {
                    val jsonObject = gson.fromJson(bufferedReader.readText(), JsonObject::class.java)
                    val newJsonObject = ConfigUpdaterMigrator.fixConfig(jsonObject)
                    val run = { gson.fromJson(newJsonObject, defaultValue.javaClass) }
                    if (LorenzUtils.isInDevEnvironment()) {
                        try {
                            run()
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            LorenzUtils.shutdownMinecraft("Config is corrupt inside development environment.")
                        }
                    } else {
                        run()
                    }
                } else {
                    gson.fromJson(bufferedReader.readText(), defaultValue.javaClass)
                }

                logger.log("Loaded $fileName from file")
            } catch (e: Exception) {
                e.printStackTrace()
                val backupFile = file.resolveSibling("$fileName-${SimpleTimeMark.now().toMillis()}-backup.json")
                logger.log("Exception while reading $file. Will load blank $fileName and save backup to $backupFile")
                logger.log("Exception was $e")
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
        val json = jsonHolder[fileType] ?: error("Could not find json object for $fileType")
        saveFile(fileType.file, fileType.fileName, json, reason)
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
            move(unit, file, reason)
        } catch (e: IOException) {
            logger.log("Could not save $fileName file to $file")
            e.printStackTrace()
        }
    }

    private fun move(unit: File, file: File, reason: String, loop: Int = 0) {
        try {
            Files.move(
                unit.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: AccessDeniedException) {
            if (loop == 5) {
                ErrorManager.logErrorWithData(
                    e,
                    "could not save config.",
                    "config save reason" to reason,
                )
                return
            }
            ChatUtils.debug("config save AccessDeniedException! (loop $loop)")
            DelayedRun.runNextTick {
                move(unit, file, reason, loop + 1)
            }
        }
    }

    fun disableSaving() {
        disableSaving = true
    }
}

enum class ConfigFileType(val fileName: String, val clazz: Class<*>) {
    FEATURES("config", Features::class.java),
    SACKS("sacks", SackData::class.java),
    FRIENDS("friends", FriendsJson::class.java),
    KNOWN_FEATURES("known_features", KnownFeaturesJson::class.java),
    JACOB_CONTESTS("jacob_contests", JacobContestsJson::class.java),
    VISUAL_WORDS("visual_words", VisualWordsJson::class.java),
    ;

    val file by lazy { File(ConfigManager.configDirectory, "$fileName.json") }
}
