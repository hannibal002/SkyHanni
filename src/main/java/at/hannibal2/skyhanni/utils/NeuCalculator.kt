package at.hannibal2.hanni.utils

import at.hannibal2.hanni.utils.system.PlatformUtils
import io.github.moulberry.notenoughupdates.util.Calculator
import java.math.BigDecimal

object NeuCalculator {

    fun calculateOrNull(input: String?): BigDecimal? {
        if (input.isNullOrEmpty() || !PlatformUtils.isNeuLoaded()) return null
        return runCatching { Calculator.calculate(input) }.getOrNull()
    }
}
