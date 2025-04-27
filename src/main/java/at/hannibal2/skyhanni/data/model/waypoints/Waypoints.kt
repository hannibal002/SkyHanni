package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.annotations.Expose

@JvmInline
value class Waypoints<T>(
    @Expose
    val waypoints: MutableList<T> = mutableListOf()
) : MutableList<T> by waypoints
