package at.hannibal2.skyhanni.utils

import io.github.moulberry.notenoughupdates.util.Calculator
import java.math.BigDecimal

object NEUCalculator {

    // TODO add if (!usingNEU()) null
    fun calculateOrNull(input: String?): BigDecimal? {
        if (input.isNullOrEmpty()) return null
        return runCatching { Calculator.calculate(input) }.getOrNull()
    }
}
