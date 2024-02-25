package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.utils.StringUtils.removeColor

enum class TabStringType {
    TITLE,
    SUB_TITLE,
    TEXT,
    PLAYER;

    companion object {

        fun fromLine(line: String): TabStringType {
            val strippedLine: String = line.removeColor()
            if (strippedLine.startsWith(" ")) {
                return TEXT
            }
            return if (TabListReader.usernamePattern.matcher(strippedLine).find()) {
                PLAYER
            } else {
                SUB_TITLE
            }
        }

        fun usernameFromLine(input: String): String {
            val usernameMatcher = TabListReader.usernamePattern.matcher(input.removeColor())
            return if (usernameMatcher.find()) usernameMatcher.group("username") else input
        }
    }
}
