package at.hannibal2.skyhanni.features.event.carnival.fruitdigging

import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName

enum class DowsingMode {
    ANCHOR,
    MINES,
    TREASURE,
    ;

    override fun toString() = toFormattedName()
}
