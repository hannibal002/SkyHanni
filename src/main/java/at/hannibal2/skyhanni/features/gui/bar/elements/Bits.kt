package at.hannibal2.skyhanni.features.gui.bar.elements

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

object Bits : BarElement() {
    override val configLine: String = "§bBits"
    override fun getString(): String = "Bits: §b${BitsAPI.bits.addSeparators()}"
}
