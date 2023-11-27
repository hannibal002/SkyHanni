package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.utils.LorenzDebug
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException

object MobDevTracker {

    const val FILE_NAME: String = "config/skyhanni/logs/mob/Tracker.txt"

    val data = Data()

    class Data {
        var retries = 0
        var outOfRangeRetries = 0
        var spawn = 0
        var deSpawn = 0
        var misses = 0
        var startedRetries = 0
        val retriesAvg: Int
            get() = if (startedRetries != 0) retries / startedRetries else 0

        val entityNames = sortedSetOf<String>()

        fun reset() {
            retries = 0
            spawn = 0
            outOfRangeRetries = 0
            deSpawn = 0
            misses = 0
            startedRetries = 0
            entityNames.clear()
        }
    }

    fun addEntityName(mob: Mob) {
        if (mob.mobType == Mob.Type.Player) return
        val name = when (mob.mobType) {
            Mob.Type.DisplayNPC -> "DNPC"
            Mob.Type.Summon -> "SUM "
            Mob.Type.Basic -> "BASE"
            Mob.Type.Dungeon -> "DUNG"
            Mob.Type.Boss -> "BOSS"
            Mob.Type.Slayer -> "SLAY"
            Mob.Type.Player -> "PLAY"
            Mob.Type.Projectile -> "PROJ"
            Mob.Type.Special -> "SPEC"
        } + " " + mob.name
        if (data.entityNames.contains(name)) return
        data.entityNames.add(name)
    }

    fun saveToFile() {
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(data)

            // Write the JSON data to the file
            File(FILE_NAME).writeText(json)
        } catch (e: IOException) {
            LorenzDebug.log("Error saving data to file: ${e.message}")
        }
    }

    fun loadFromFile() {
        try {
            // Create the parent directory and its ancestors recursively if they don't exist
            val parentDir = File(FILE_NAME).parentFile
            parentDir.mkdirs()

            // Load data from the file
            if (File(FILE_NAME).exists()) {
                val gson = Gson()
                val json = File(FILE_NAME).readText()
                val loadedData = gson.fromJson(json, Data::class.java)
                // Update the existing data with loaded data
                data.retries = loadedData.retries
                data.outOfRangeRetries = loadedData.outOfRangeRetries
                data.spawn = loadedData.spawn
                data.deSpawn = loadedData.deSpawn
                data.misses = loadedData.misses
                data.startedRetries = loadedData.startedRetries
                data.entityNames.clear()
                data.entityNames.addAll(loadedData.entityNames)
            }
        } catch (e: IOException) {
            LorenzDebug.log("Error loading data from file: ${e.message}")
        }
    }

    override fun toString(): String {
        return "TrackerData(retries=${data.retries}, outOfRangeRetries=${data.outOfRangeRetries}, spawn=${data.spawn}, deSpawn=${data.deSpawn}, misses=${data.misses}, , startedRetries=${data.startedRetries}, retriesAvg=${data.retriesAvg}, entityNames=${data.entityNames})"
    }

}
