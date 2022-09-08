package at.hannibal2.skyhanni.utils

object IteratorUtils {

    fun <T> getOnlyElement(it: Iterator<T>, defaultValue: T): T {
        if (!it.hasNext()) return defaultValue
        val ret = it.next()
        return if (it.hasNext()) defaultValue else ret
    }

    fun <T> getOnlyElement(it: Iterable<T>, defaultValue: T): T {
        return getOnlyElement(it.iterator(), defaultValue)
    }
}