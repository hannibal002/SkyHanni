package at.hannibal2.skyhanni.data.model.waypoints

import com.google.gson.annotations.Expose

class Waypoints<T>(
    @Expose
    val waypoints: MutableList<T> = mutableListOf()
) : MutableList<T> by waypoints
