package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.PurseAPI

object CustomLines {

    internal fun String.handleCustomLine(): List<String> {
        return this
            .replace("&", "§")
            .replace("%purse%", PurseAPI.getPurse().toString())
            .replace("%bits%", BitsAPI.bits.toString())

            .split("\\n")
    }
}
