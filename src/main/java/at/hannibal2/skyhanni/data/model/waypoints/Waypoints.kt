package at.hannibal2.skyhanni.data.model.waypoints

import com.google.gson.annotations.Expose

interface Copyable<T> {
    fun copy(): T
}

class Waypoints<T : Copyable<T>>(
    @Expose
    val waypoints: MutableList<T> = mutableListOf(),
) : MutableList<T> by waypoints {
    fun deepCopy() = transform { it.copy() }

    inline fun <R : Copyable<R>> transform(transform: (T) -> R): Waypoints<R> = Waypoints(waypoints.map { transform(it) }.toMutableList())
}
