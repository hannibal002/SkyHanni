package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.core.config.PositionList
import at.hannibal2.skyhanni.data.PetDataStorage
import at.hannibal2.skyhanni.data.jsonobjects.local.FriendsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.JacobContestsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.KnownFeaturesJson
import at.hannibal2.skyhanni.data.jsonobjects.local.VisualWordsJson
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.IdentityCharacteristics
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.ReflectionUtils.makeAccessible
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringFileHandler
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapterFactory
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorKeybind
//#if MC < 1.21
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorKeybindL
//#endif
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import io.github.notenoughupdates.moulconfig.processor.ProcessedOptionImpl
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.concurrent.fixedRateTimer
import kotlin.reflect.KMutableProperty0
import kotlin.time.Duration.Companion.days

private fun GsonBuilder.registerIfBeta(create: TypeAdapterFactory): GsonBuilder {
    return if (SkyHanniMod.isBetaVersion) {
        registerTypeAdapterFactory(create)
    } else this
}

class ConfigManager {
    companion object {

        val gson: Gson = BaseGsonBuilder.gson()
//             .registerIfBeta(FeatureTogglesByDefaultAdapter)
            .create()

        val configDirectory = File("config/skyhanni")
    }

    private val logger = LorenzLogger("config_manager")

    private val jsonHolder: Map<ConfigFileType, Any> = enumMapOf()

    lateinit var processor: BlockingMoulConfigProcessor
    private var disableSaving = false

    private fun setConfigHolder(type: ConfigFileType, value: Any) {
        require(value.javaClass == type.clazz)
        @Suppress("UNCHECKED_CAST")
        (type.property as KMutableProperty0<Any>).set(value)
        (jsonHolder as MutableMap<ConfigFileType, Any>)[type] = value
    }

    fun firstLoad() {
        if (jsonHolder.isNotEmpty()) {
            logger.log("Loading config despite config being already loaded?")
        }
        configDirectory.mkdirs()


        for (fileType in ConfigFileType.entries) {
            setConfigHolder(fileType, firstLoadFile(fileType.file, fileType, fileType.clazz.newInstance()))
        }

        // TODO use SecondPassedEvent
        fixedRateTimer(name = "skyhanni-config-auto-save", period = 60_000L, initialDelay = 60_000L) {
            saveConfig(ConfigFileType.FEATURES, "auto-save-60s")
        }

        val features = SkyHanniMod.feature
        recreateConfig()

        try {
            findPositionLinks(features, mutableSetOf())
        } catch (e: Exception) {
            ErrorManager.crashInDevEnv("Couldn't load config links") { e }
        }

        deleteOldBackups()
    }

    private fun deleteOldBackups() {
        OSUtils.deleteExpiredFiles(File("skyhanni/config/backup"), SkyHanniMod.feature.dev.configBackupExpiryTime.days)
    }

    private fun findPositionLinks(obj: Any?, slog: MutableSet<IdentityCharacteristics<Any>>) {
        if (obj == null) return
        if (!obj.javaClass.name.startsWith("at.hannibal2.skyhanni.")) return
        val ic = IdentityCharacteristics(obj)
        if (ic in slog) return
        slog.add(ic)
        var missingConfigLink = false
        for (field in obj.javaClass.declaredFields.map { it.makeAccessible() }) {
            if (field.type != Position::class.java && field.type != PositionList::class.java) {
                findPositionLinks(field.get(obj), slog)
                continue
            }
            val configLink = field.getAnnotation(ConfigLink::class.java)
            if (configLink == null) {
                if (PlatformUtils.isDevEnvironment) {
                    var name = "${field.declaringClass.name}.${field.name}"
                    name = name.replace("at.hannibal2.skyhanni.config.", "")
                    val hasExplanatoryAnnotation = field.getAnnotation(NoConfigLink::class.java) != null
                    if (!hasExplanatoryAnnotation) {
                        println("WEE WOO WEE WOO HIER FEHLT EIN @CONFIGLINK: $name")
                        missingConfigLink = true
                    }
                }
                continue
            }
            if (field.type == Position::class.java) {
                val position = field.get(obj) as Position
                position.setLink(configLink)
            } else if (field.type == PositionList::class.java) {
                val list = field.get(obj) as PositionList
                list.setLink(configLink)
            }
        }
        if (missingConfigLink) {
            println("")
            println(
                "This crash is here to remind you to fix the missing " +
                    "@ConfigLink annotation over your new config position config element.",
            )
            println("")
            println("Steps to fix:")
            println("1. Search for `WEE WOO WEE WOO` in the console output.")
            println("2. Either add the Config Link.")
            println("3. Or add the @NoConfigLink annotation to the field.")
            println("")
            PlatformUtils.shutdownMinecraft("Missing Config Link")
        }
    }

    private fun firstLoadFile(file: File?, fileType: ConfigFileType, defaultValue: Any): Any {
        val fileName = fileType.fileName
        logger.log("Trying to load $fileName from $file")
        var output: Any? = defaultValue

        if (file!!.exists()) {
            try {
                val text = StringFileHandler(file).load()
                val lenientGson = BaseGsonBuilder.lenientGson().create()
                logger.log("load-$fileName-now")

                output = if (fileType == ConfigFileType.FEATURES) {
                    val jsonObject = lenientGson.fromJson(text, com.google.gson.JsonObject::class.java)
                    val newJsonObject = ConfigUpdaterMigrator.fixConfig(jsonObject)
                    val run = { lenientGson.fromJson(newJsonObject, defaultValue.javaClass) }
                    if (PlatformUtils.isDevEnvironment) {
                        try {
                            run()
                        } catch (e: Throwable) {
                            logger.log(e.stackTraceToString())
                            PlatformUtils.shutdownMinecraft("Config is corrupt inside development environment.")
                        }
                    } else {
                        run()
                    }
                } else {
                    lenientGson.fromJson(text, defaultValue.javaClass)
                }

                logger.log("Loaded $fileName from file")
            } catch (e: Exception) {
                logger.log(e.stackTraceToString())
                val backupFile = file.resolveSibling("$fileName-${SimpleTimeMark.now().toMillis()}-backup.json")
                logger.log("Exception while reading $file. Will load blank $fileName and save backup to $backupFile")
                logger.log("Exception was $e")
                try {
                    file.copyTo(backupFile)
                } catch (e: Exception) {
                    logger.log("Could not create backup for $fileName file")
                    logger.log(e.stackTraceToString())
                }
            }
        }

        if (output == defaultValue) {
            logger.log("Setting $fileName to be blank as it did not exist. It will be saved once something is written to it")
        }
        if (output == null) {
            logger.log("Setting $fileName to be blank as it was null. It will be saved once something is written to it")
            output = defaultValue
        }

        return output
    }

    fun saveConfig(fileType: ConfigFileType, reason: String) {
        val json = jsonHolder[fileType] ?: error("Could not find json object for $fileType")
        saveFile(fileType.file, fileType.fileName, json, reason)
        saveFile(fileType.backupFile, fileType.fileName, json, reason)
    }

    private fun saveFile(file: File, fileName: String, data: Any, reason: String) {
        if (disableSaving) return
        logger.log("saveConfig: $reason")
        try {
            logger.log("Saving $fileName file")
            file.parentFile.mkdirs()
            StringFileHandler(file).save(gson.toJson(data))
            logger.log("Saved $fileName file successfully")
        } catch (e: IOException) {
            logger.log("Could not save $fileName file to $file")
            logger.log(e.stackTraceToString())
        }
    }

    fun disableSaving() {
        disableSaving = true
    }

    fun recreateConfig() {
        ConfigGuiManager.editor = null
        val features = SkyHanniMod.feature
        processor = BlockingMoulConfigProcessor()
        BuiltinMoulConfigGuis.addProcessors(processor)
        UpdateManager.injectConfigProcessor(processor)
        val driver = ConfigProcessorDriver(processor)
        driver.warnForPrivateFields = false
        driver.processConfig(features)
    }
}

private fun getBackupFile(file: File): File {
    val parent = file.parentFile
    val fileName = file.nameWithoutExtension
    val now = LocalDate.now()
    val year = now.format(DateTimeFormatter.ofPattern("yyyy"))
    val month = now.format(DateTimeFormatter.ofPattern("MM"))
    val day = now.format(DateTimeFormatter.ofPattern("dd"))

    val directory = File(parent, "backup/$year/$month")

    return File(directory, "$year-$month-$day-${SkyHanniMod.VERSION}-$fileName.json")
}

enum class ConfigFileType(val fileName: String, val clazz: Class<*>, val property: KMutableProperty0<*>) {
    FEATURES("config", Features::class.java, SkyHanniMod::feature),
    SACKS("sacks", SackData::class.java, SkyHanniMod::sackData),
    FRIENDS("friends", FriendsJson::class.java, SkyHanniMod::friendsData),
    KNOWN_FEATURES("known_features", KnownFeaturesJson::class.java, SkyHanniMod::knownFeaturesData),
    JACOB_CONTESTS("jacob_contests", JacobContestsJson::class.java, SkyHanniMod::jacobContestsData),
    VISUAL_WORDS("visual_words", VisualWordsJson::class.java, SkyHanniMod::visualWordsData),
    PETS("pets", PetDataStorage::class.java, SkyHanniMod::petData),
    ;

    val file by lazy { File(ConfigManager.configDirectory, "$fileName.json") }
    val backupFile get() = getBackupFile(file)
}

class BlockingMoulConfigProcessor : MoulConfigProcessor<Features>(SkyHanniMod.feature) {
    override fun createOptionGui(
        processedOption: ProcessedOption,
        field: Field,
        option: ConfigOption,
    ): GuiOptionEditor? {
        val default = super.createOptionGui(processedOption, field, option) ?: return null
        if (processedOption !is ProcessedOptionImpl) return default
        var extraPath = ""
        val categoryParent = processedOption.category.parentCategoryId
        if (categoryParent != null) {
            extraPath = categoryParent.split(".").last() + "."
        }
        extraPath += processedOption.getPath()
        if (default is GuiOptionEditorKeybind) {
            UpdateKeybinds.keybinds.add(extraPath)
        }
        //#if MC < 1.21
        if (default is GuiOptionEditorKeybindL) {
            UpdateKeybinds.keybinds.add(extraPath)
        }
        //#endif
        if (EnforcedConfigValues.isBlockedFromEditing(extraPath)) {
            return GuiOptionEditorBlocked(default)
        }

        if (PlatformUtils.IS_LEGACY) {
            if (field.isAnnotationPresent(OnlyModern::class.java)) {
                return GuiOptionEditorHidden(default)
            }
        } else {
            if (field.isAnnotationPresent(OnlyLegacy::class.java)) {
                return GuiOptionEditorHidden(default)
            }
        }
        return default
    }
}
