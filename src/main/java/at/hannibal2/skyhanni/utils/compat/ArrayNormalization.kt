package at.hannibal2.hanni.utils.compat


inline fun <reified T> List<T>.normalizeAsArray() = this.toTypedArray()
fun <T> Array<T>.normalizeAsArray() = this

