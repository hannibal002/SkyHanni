package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.utils.StringUtils.removeColor

enum class TabStringType() {
    TITLE,
    SUB_TITLE,
    TEXT,
    PLAYER;

    companion object {
        private val usernamePattern = "^\\[(?<sblevel>\\d+)] (?:\\[\\w+] )?(?<username>\\w+)".toPattern()

        fun fromLine(line: String): TabStringType {
            val strippedLine: String = line.removeColor()
            if (strippedLine.startsWith(" ")) {
                return TEXT
            }
            return if (usernamePattern.matcher(strippedLine).find()) {
                PLAYER
            } else {
                SUB_TITLE
            }
        }

        fun usernameFromLine(input: String): String {
            val usernameMatcher = usernamePattern.matcher(input.removeColor())
            return if (usernameMatcher.find()) usernameMatcher.group("username") else input
        }
    }
}