package at.hannibal2.skyhanni.utils.compat


inline fun <reified T> List<T>.normalizeAsArray(): Array<T> = this.toTypedArray()
fun <T> Array<T>.normalizeAsArray(): Array<T> = this

