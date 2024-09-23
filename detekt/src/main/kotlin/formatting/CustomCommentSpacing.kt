package at.hannibal2.skyhanni.detektrules.formatting

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiComment

class CustomCommentSpacing(config: Config) : Rule(config) {
    override val issue = Issue(
        "CustomCommentSpacing",
        Severity.Style,
        "Enforces custom spacing rules for comments.",
        Debt.FIVE_MINS
    )

    private val allowedPatterns = listOf(
        "#if",
        "#else",
        "#elseif",
        "#endif",
    )

    override fun visitComment(comment: PsiComment) {
        val commentText = comment.text.trimStart('/')
        if (allowedPatterns.any { commentText.startsWith(it) }) {
            return
        }

        if (commentText.length > 1 && !commentText.substring(2).startsWith(" ")) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(comment),
                    "Expected space after opening comment."
                )
            )
        }

        // Fallback to super (ostensibly a no-check)
        super.visitComment(comment)
    }
}
