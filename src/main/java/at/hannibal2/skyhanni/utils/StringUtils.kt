package at.hannibal2.skyhanni.utils

object StringUtils {

    fun String.firstLetterUppercase(): String {
        if (isEmpty()) return this

        val lowercase = this.lowercase()
        val first = lowercase[0].uppercase()
        return first + lowercase.substring(1)
    }

    fun String.removeColor(): String {
        return replace("(?i)\\u00A7.", "")
    }

//    fun cleanColour(`in`: String): String? {
//        return `in`.replace("(?i)\\u00A7.".toRegex(), "")
//    }
}