package at.hannibal2.skyhanni.utils

class ExtendedChatColor(
    val rgb: Int,
    val hasAlpha: Boolean,
) {
    override fun toString(): String {
        val stringBuilder = StringBuilder()
        val hexCode = rgb.toUInt().toString(16)
            .padStart(8, '0')
            .drop(if (hasAlpha) 0 else 2)
        stringBuilder.append("§#")
        for (code in hexCode) {
            stringBuilder.append('§').append(code)
        }
        stringBuilder.append("§/")
        return stringBuilder.toString()
    }

}
