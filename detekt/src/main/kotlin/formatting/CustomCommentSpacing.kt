package formatting

import PreprocessingPattern.Companion.containsPreprocessingPattern
import SkyHanniRule
import com.intellij.psi.PsiComment
import dev.detekt.api.Config

/**
 * This rule enforces the default spacing rules for comments but ignores preprocessed comments.
 */
class CustomCommentSpacing(config: Config) : SkyHanniRule(config, "Enforces custom spacing rules for comments.") {

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
