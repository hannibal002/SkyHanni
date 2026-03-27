package at.hannibal2.skyhanni.detektrules.formatting

import at.hannibal2.skyhanni.detektrules.PreprocessingPattern.Companion.containsPreprocessingPattern
import at.hannibal2.skyhanni.detektrules.SkyHanniRule
import dev.detekt.api.CodeSmell
import dev.detekt.api.Config
import dev.detekt.api.Debt
import dev.detekt.api.Entity
import dev.detekt.api.Issue
import dev.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiComment

/**
 * This rule enforces the default spacing rules for comments but ignores preprocessed comments.
 */
class CustomCommentSpacing(config: Config) : SkyHanniRule(config) {
    override val issue = Issue(
        "CustomCommentSpacing",
        Severity.Style,
        "Enforces custom spacing rules for comments.",
        Debt.FIVE_MINS
    )

    override fun visitComment(comment: PsiComment) {
        if (comment.text.containsPreprocessingPattern()) return
        if (!commentRegex.matches(comment.text)) {
            comment.reportIssue("Expected space after opening comment.")
        }

        // Fallback to super (ostensibly a no-check)
        super.visitComment(comment)
    }

    companion object {
        /**
         * REGEX-TEST: // Test comment
         * REGEX-TEST: /* Test comment */
         */
        val commentRegex = Regex("""^(?:\/{2}|\/\*)(?:\s.*|$)""", RegexOption.DOT_MATCHES_ALL)
    }
}
