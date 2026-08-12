package style

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class PrivateUseUnicodeRule(config: Config) :
    SkyHanniRule(
        config,
        "Detects the use of private-use Unicode characters in string literals. " +
            "Use SkyblockIcon instead"
) {

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
                            "String contains private-use Unicode character U+$displayCodePoint"
                        )
                    }
            }
    }

    private fun Int.isPrivateUse(): Boolean {
        return this in 0xE000..0xF8FF ||
            this in 0xF0000..0xFFFFD ||
            this in 0x100000..0x10FFFD
    }
}
