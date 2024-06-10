package at.hannibal2.skyhanni.utils

class GenericWrapper<T>(val it: T) {
    companion object {
        @JvmStatic
        @JvmName("getSimpleTimeMark")
        fun getSimpleTimeMark(it: SimpleTimeMark) = GenericWrapper(it)
    }
}
