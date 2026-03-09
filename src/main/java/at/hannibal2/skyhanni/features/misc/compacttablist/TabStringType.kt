package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component

enum class TabStringType {
    TITLE,
    SUB_TITLE,
    TEXT,
    PLAYER;

    companion object {

        fun fromComponent(component: Component): TabStringType {
            val unformattedLine: String = component.string.removeColor()
            if (unformattedLine.startsWith(" ")) {
                return TEXT
            }
            return if (TabListReader.usernamePattern.matcher(unformattedLine).find()) {
                PLAYER
            } else {
                SUB_TITLE
            }
        }

        fun usernameFromComponent(component: Component): String {
            val usernameMatcher = TabListReader.usernamePattern.matcher(component.string)
            return if (usernameMatcher.find()) usernameMatcher.group("username") else component.string
        }
    }
}
