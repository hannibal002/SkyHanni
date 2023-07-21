package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import java.lang.reflect.Field

object SkyHanniConfigSearchResetCommand {
    fun command(args: Array<String>) {
        if (args.isEmpty()) {
            LorenzUtils.chat("§c[SkyHanni] This is a config-edit command, only use it if you know what you are doing!")
            return
        }
        if (args[0] == "reset") {
            if (args.size != 2) {
                LorenzUtils.chat("§c/shconfig reset <config element>")
                return
            }
            val term = args[1]
            try {
                val (field, defaultObject, _) = getComplexField(term, Features())
                val (_, _, parent) = getComplexField(term, SkyHanniMod.feature)
                field.set(parent, defaultObject)
                LorenzUtils.chat("§eSuccessfully reset config element '$term'")
            } catch (e: Exception) {
                CopyErrorCommand.logError(e, "Could not reset config element '$term'")
            }
            return
        } else if (args[0] == "search") {
            if (args.size == 1) {
                LorenzUtils.chat("§c/shconfig search <config name> [class name]")
                return
            }
            Thread {
                try {
                    startSearch(args)
                } catch (e: Exception) {
                    CopyErrorCommand.logError(e, "Error while trying to search config")
                }
            }.start()
            return
        }

        LorenzUtils.chat("§c/shconfig <search;reset>")
    }

    private fun createFilter(condition: Boolean, searchTerm: () -> String): Pair<(String) -> Boolean, String> {
        return if (condition && searchTerm() != "all") {
            val term = searchTerm()
            Pair({ it.lowercase().contains(term) }, "'$term'")
        } else Pair({ true }, "<all>")
    }

    private fun startSearch(args: Array<String>) {
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
        LorenzUtils.chat("§eCopied search result ($size) to clipboard.")
    }

    private fun findConfigElements(
        configFilter: (String) -> Boolean,
        classFilter: (String) -> Boolean,
    ): MutableList<String> {
        val list = mutableListOf<String>()
        for ((name, obj) in loadAllFields("config", Features())) {
            if (name == "config.DISCORD") continue
            if (name == "config.GITHUB") continue
            val description = if (obj != null) {
                val className = obj.getClassName()
                if (!classFilter(className)) continue
                val objectName = obj.getObjectName()
                if (objectName.startsWith(className) && objectName.startsWith("at.hannibal2.skyhanni.config.features.")) {
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
            field = obj.javaClass.getField(entry)
            field.isAccessible = true
            parentObject = obj
            obj = field.get(obj)
        }
        if (field == null) {
            throw Error("Could not find field for '$term'")
        }
        return Triple(field, obj, parentObject)
    }

    private fun loadAllFields(parentName: String, obj: Any, depth: Int = 0): Map<String, Any?> {
        if (depth == 10) return emptyMap() // this is only a safety backup, needs increasing maybe someday
        val map = mutableMapOf<String, Any?>()
        for (field in obj.javaClass.fields) {
            val name = field.name
            val fieldName = "$parentName.$name"
            field.isAccessible = true
            val newObj = field.get(obj)
            map[fieldName] = newObj
            if (newObj != null) {
                if (newObj !is Boolean && newObj !is String && newObj !is Long && newObj !is Int) {
                    if (newObj !is Position) {
                        map.putAll(loadAllFields(fieldName, newObj, depth + 1))
                    }
                }
            }
        }

        return map
    }

    private fun Any.getClassName(): String {
        // we do not use javaClass.simpleName since we want to catch edge cases
        val name = javaClass.name
        return when (name) {
            "at.hannibal2.skyhanni.config.core.config.Position" -> "Position"
            "java.lang.Boolean" -> "Boolean"
            "java.lang.Integer" -> "Int"
            "java.lang.Long" -> "Long"
            "java.lang.String" -> "String"
            "io.github.moulberry.moulconfig.observer.Property" -> "moulconfig.Property"
            "java.util.ArrayList" -> "List"
            "java.util.HashMap" -> "Map"

            else -> name
        }
    }

    private fun Field.makeAccessible() = also { isAccessible = true }

    private fun Any.getObjectName(): String {
        if (this is Position) {
            val x = javaClass.getDeclaredField("x").makeAccessible().get(this)
            val y = javaClass.getDeclaredField("y").makeAccessible().get(this)
            return "($x, $y)"
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
            val name = value.getClassName()
            return "moulconfig.Property<$name> = ${value.getObjectName()}"
        }

        if (this is Int || this is Int) return addSeparators()

        return toString()
    }
}
