package at.hannibal2.skyhanni.items

import at.hannibal2.skyhanni.utils.LorenzDebug
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class VanillaItemManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        private val vanillaItems: MutableList<String> = ArrayList()

        fun isVanillaItem(internalName: String): Boolean {
            return vanillaItems.contains(internalName)
        }
    }

    init {
        load()
    }

    private fun load() {
        vanillaItems.clear()
        val itemDirectory = File("config/notenoughupdates/repo/items")
        if (!itemDirectory.isDirectory) return
        val files = itemDirectory.listFiles() ?: return
        for (file in files) {
            val jsonObject = getJsonFromFile(file)
            if (jsonObject != null) {
                if (jsonObject.has("vanilla") && jsonObject["vanilla"].asBoolean) {
                    val name = file.name
                    val internalName = name.split(".")[0]
                    vanillaItems.add(internalName)
                }
            }
        }

    }

    private fun getJsonFromFile(file: File): JsonObject? {
        try {
            BufferedReader(InputStreamReader(FileInputStream(file),
                StandardCharsets.UTF_8
            )).use { reader -> return gson.fromJson(reader, JsonObject::class.java) }
        } catch (e: Exception) {
            return null
        }
    }
}