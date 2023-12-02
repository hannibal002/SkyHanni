package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.makeAccessible
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.JsonElement
import io.github.moulberry.notenoughupdates.util.Shimmy
import kotlinx.coroutines.launch
import java.lang.reflect.Field
import java.lang.reflect.Modifier

// TODO in the future change something here
object SkyHanniConfigSearchResetCommand {

    private var lastCommand = emptyArray<String>()

    fun command(args: Array<String>) {
        SkyHanniMod.coroutineScope.launch {
            LorenzUtils.chat(runCommand(args), false)
        }
        lastCommand = args
    }

    private suspend fun runCommand(args: Array<String>): String {
        if (args.isEmpty()) {
            return "§cThis is a powerful config-edit command, only use it if you know what you are doing!"
        }

        return when (args[0].lowercase()) {
            "reset" -> resetCommand(args)
            "search" -> searchCommand(args)
            "set" -> setCommand(args)

            else -> "§c/shconfig <search;reset;set>"
        }
    }

    private fun resetCommand(args: Array<String>): String {
        if (args.size != 2) return "§c/shconfig reset <config element>"
        val term = args[1]
        if (term.startsWith("playerSpecific")) return "§cCannot reset playerSpecific! Use §e/shconfig set §cinstead."
        if (term.startsWith("profileSpecific")) return "§cCannot reset profileSpecific! Use §e/shconfig set §cinstead."

        return try {
            val (field, defaultObject, _) = getComplexField(term, Features())
            val (_, _, parent) = getComplexField(term, SkyHanniMod.feature)
            val affectedElements = findConfigElements({ it.startsWith("$term.") }, { true }).size
            if (affectedElements > 3 && !args.contentEquals(lastCommand)) {
                return "§cThis will change $affectedElements config elements! Use the command again to confirm."
            }
            field.set(parent, defaultObject)
            "§eSuccessfully reset config element '$term'"
        } catch (e: Exception) {
            ErrorManager.logError(e, "Could not reset config element '$term'")
            "§cCould not reset config element '$term'"
        }
    }

    private fun searchCommand(args: Array<String>): String {
        if (args.size == 1) return "§c/shconfig search <config name> [class name]"

        return try {
            startSearch(args)
        } catch (e: Exception) {
            ErrorManager.logError(e, "Error while trying to search config")
            "§cError while trying to search config"
        }
    }

    private suspend fun setCommand(args: Array<String>): String {
        if (args.size < 3) return "§c/shconfig set <config name> <json element>"
        val term = args[1]
        var rawJson = args.drop(2).joinToString(" ")
        if (rawJson == "clipboard") {
            val readFromClipboard = OSUtils.readFromClipboard() ?: return "§cClipboard has no string!"
            rawJson = readFromClipboard
        }

        val root: Any = when {
            term.startsWith("config") -> SkyHanniMod.feature

            term.startsWith("playerSpecific") -> {
                ProfileStorageData.playerSpecific ?: return "§cplayerSpecific is null!"
            }

            term.startsWith("profileSpecific") -> {
                ProfileStorageData.profileSpecific ?: return "§cprofileSpecific is null!"
            }

            else -> return "§cUnknown config location!"
        }

        val affectedElements = findConfigElements({ it.startsWith("$term.") }, { true }).size
        if (affectedElements > 3 && !args.contentEquals(lastCommand)) {
            return "§cThis will change $affectedElements config elements! Use the command again to confirm."
        }

        val element = ConfigManager.gson.fromJson(rawJson, JsonElement::class.java)
        val list = term.split(".").drop(1)
        val shimmy = Shimmy.makeShimmy(root, list) ?: return "§cCould not change config element '$term', not found!"
        return try {
            shimmy.setJson(element)
            "§eChanged config element $term."
        } catch (e: Exception) {
            ErrorManager.logError(e, "Could not change config element '$term' to '$rawJson'")
            "§cCould not change config element '$term' to '$rawJson'"
        }
    }

    private fun createFilter(condition: Boolean, searchTerm: () -> String): Pair<(String) -> Boolean, String> {
        return if (condition && searchTerm() != "all") {
            val term = searchTerm()
            Pair({ it.lowercase().contains(term) }, "'$term'")
        } else Pair({ true }, "<all>")
    }

    private fun startSearch(args: Array<String>): String {
        val (configFilter, configSearchTerm) = createFilter(true) { args[1].lowercase() }
        val (classFilter, classSearchTerm) = createFilter(args.size == 3) { args[2].lowercase() }

        val elements = findConfigElements(configFilter, classFilter)
        val builder = StringBuilder()
        builder.append("```\n")
        builder.append("Search config for SkyHanni ${SkyHanniMod.version}\n")
        builder.append("configSearchTerm: $configSearchTerm\n")
        builder.append("classSearchTerm: $classSearchTerm\n")
        builder.append("\n")
        val size = elements.size
        builder.append("Found $size config elements:\n")
        for (entry in elements) {
            builder.append(entry)
            builder.append("\n")
        }
        builder.append("```")
        OSUtils.copyToClipboard(builder.toString())
        return "§eCopied search result ($size) to clipboard."
    }

    private fun findConfigElements(
        configFilter: (String) -> Boolean,
        classFilter: (String) -> Boolean,
    ): MutableList<String> {
        val list = mutableListOf<String>()

        val map = buildMap {
            putAll(loadAllFields("config", SkyHanniMod.feature))

            val playerSpecific = ProfileStorageData.playerSpecific
            if (playerSpecific != null) {
                putAll(loadAllFields("playerSpecific", playerSpecific))
            } else {
                this["playerSpecific"] = null
            }

            val profileSpecific = ProfileStorageData.profileSpecific
            if (profileSpecific != null) {
                putAll(loadAllFields("profileSpecific", profileSpecific))
            } else {
                this["profileSpecific"] = null
            }
        }

        for ((name, obj) in map) {
            if (name == "config.DISCORD") continue
            if (name == "config.GITHUB") continue

            // this is the old, unused storage area
            if (name.startsWith("config.hidden")) continue

            if (name == "config.storage.players") continue
            if (name == "playerSpecific.profiles") continue

            val description = if (obj != null) {
                val className = obj.getClassName()
                if (!classFilter(className)) continue
                val objectName = obj.getObjectName()
                if (obj !is Runnable && objectName.startsWith(className) && (objectName.startsWith("at.hannibal2.skyhanni.config.features.") ||
                        objectName.startsWith("at.hannibal2.skyhanni.config.Storage"))
                ) {
                    "<category>"
                } else {
                    "$className = $objectName"
                }
            } else "null"

            if (configFilter(name)) {
                list.add("$name $description")
            }
        }
        return list
    }

    private fun getComplexField(term: String, startObject: Any): Triple<Field, Any, Any> {
        var parentObject = startObject
        var obj = startObject
        val line = term.split(".").drop(1)
        var field: Field? = null
        for (entry in line) {
            field = obj.javaClass.getField(entry).makeAccessible()
            parentObject = obj
            obj = field.get(obj)
        }
        if (field == null) {
            throw Error("Could not find field for '$term'")
        }
        return Triple(field, obj, parentObject)
    }

    private fun loadAllFields(parentName: String, obj: Any, depth: Int = 0): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        if (depth == 8) { // this is only a backup for safety, needs increasing someday maybe
            map["$parentName.<end of depth>"] = null
            return map
        }
        for (field in obj.javaClass.fields) {
            if ((field.modifiers and Modifier.STATIC) != 0) continue

            val name = field.name
            val fieldName = "$parentName.$name"
            val newObj = field.makeAccessible().get(obj)
            map[fieldName] = newObj
            if (newObj != null && newObj !is Boolean && newObj !is String && newObj !is Long && newObj !is Int && newObj !is Double && newObj !is Position && !newObj.javaClass.isEnum) {
                map.putAll(loadAllFields(fieldName, newObj, depth + 1))
            }
        }

        return map
    }

    private fun Any.getClassName(): String {
        if (this is io.github.moulberry.moulconfig.observer.Property<*>) {
            val value = javaClass.getDeclaredField("value").makeAccessible().get(this)
            val name = value.getClassName()
            return "moulconfig.Property<$name>"
        }

        if (this is Runnable) return "Runnable"

        // we don't use javaClass.simpleName since we want to catch edge cases
        return when (val name = javaClass.name) {
            "at.hannibal2.skyhanni.config.core.config.Position" -> "Position"
            "java.lang.Boolean" -> "Boolean"
            "java.lang.Integer" -> "Int"
            "java.lang.Long" -> "Long"
            "java.lang.String" -> "String"
            "java.lang.Double" -> "Double"
            "io.github.moulberry.moulconfig.observer.Property" -> "moulconfig.Property"
            "com.google.gson.internal.LinkedTreeMap" -> "LinkedTreeMap"
            "java.util.ArrayList" -> "List"
            "java.util.HashMap" -> "Map"

            else -> name
        }
    }

    private fun Any.getObjectName(): String {
        if (this is Position) {
            val x = javaClass.getDeclaredField("x").makeAccessible().get(this)
            val y = javaClass.getDeclaredField("y").makeAccessible().get(this)
            val scale = javaClass.getDeclaredField("scale").makeAccessible().get(this)
            return "($x, $y, $scale)"
        }

        if (this is String) {
            return if (toString() == "") {
                "<empty string>"
            } else {
                "'$this'"
            }
        }

        if (this is io.github.moulberry.moulconfig.observer.Property<*>) {
            val value = javaClass.getDeclaredField("value").makeAccessible().get(this)
            return value.getObjectName()
        }

        if (this is Int) return addSeparators()
        if (this is Long) return addSeparators()

        if (this is Runnable) return "<Runnable>"

        return toString()
    }
}
