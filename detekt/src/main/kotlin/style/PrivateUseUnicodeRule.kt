package style

import SkyHanniRule
import dev.detekt.api.Config
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import kotlin.streams.toList

class PrivateUseUnicodeRule(config: Config) :
    SkyHanniRule(
        config,
        "Detects the use of private-use Unicode characters in string literals."
) {
    private val extraIcons: String by config(
        "☯❣"
    )

    private val extraIconsCodePoints: List<Int> by lazy { extraIcons.codePoints().toList() }

    override fun visitStringTemplateExpression(
        expression: KtStringTemplateExpression,
    ) {
        super.visitStringTemplateExpression(expression)

        expression.entries
            .filterIsInstance<KtLiteralStringTemplateEntry>()
            .forEach { entry ->
                entry.text
                    .codePoints()
                    .filter { it.isPrivateUse() }
                    .forEach { codePoint ->
                        val displayCodePoint = codePoint.toString(16).uppercase().padStart(4, '0')
                        expression.reportIssue(
                            "String contains private-use Unicode character U+$displayCodePoint " +
                                "Use SkyblockStat/SkyblockMobType/SkyblockIcon instead."
                        )
                    }
            }
    }

    private fun Int.isPrivateUse(): Boolean {
        if (this in 0xE000..0xF8FF ||
            this in 0xF0000..0xFFFFD ||
            this in 0x100000..0x10FFFD) {
            return true
        }
        return extraIconsCodePoints.contains(this)
    }
}
