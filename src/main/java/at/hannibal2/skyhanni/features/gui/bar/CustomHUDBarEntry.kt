package at.hannibal2.skyhanni.features.gui.bar

import at.hannibal2.skyhanni.features.gui.bar.elements.AlignLeftRight
import at.hannibal2.skyhanni.features.gui.bar.elements.BarElement
import at.hannibal2.skyhanni.features.gui.bar.elements.Bits
import at.hannibal2.skyhanni.features.gui.bar.elements.Date
import at.hannibal2.skyhanni.features.gui.bar.elements.Location
import at.hannibal2.skyhanni.features.gui.bar.elements.Purse
import at.hannibal2.skyhanni.features.gui.bar.elements.Time

enum class CustomHUDBarEntry(val element: BarElement) {
    PURSE(Purse),
    BITS(Bits),
    LOCATION(Location),
    ALIGN_LEFT_RIGHT(AlignLeftRight),
    DATE(Date),
    TIME(Time),
    ;

    override fun toString(): String = element.configLine
}
