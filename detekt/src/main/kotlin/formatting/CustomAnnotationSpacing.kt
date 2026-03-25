package at.hannibal2.skyhanni.detektrules.formatting

import at.hannibal2.skyhanni.detektrules.PreprocessingPattern.Companion.containsPreprocessingPattern
import at.hannibal2.skyhanni.detektrules.SkyHanniRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * This rule enforces the default spacing rules for annotations but allows preprocessed comments to be between
 * an annotation and the annotated construct.
 */
class CustomAnnotationSpacing(config: Config) : SkyHanniRule(config) {
    override val issue = Issue(
        "CustomAnnotationSpacing",
        Severity.Style,
        "Enforces custom spacing rules for annotations.",
        Debt.FIVE_MINS
    )

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        val nextNodes = annotationEntry.nextLeaf()?.siblings()?.takeWhile { it is PsiWhiteSpace || it is PsiComment } ?: sequenceOf()

        val hasInvalidSpacing = nextNodes.any { nextNode ->
            when (nextNode) {
                is PsiWhiteSpace -> nextNode.isInvalid()
                is PsiComment -> nextNode.isInvalid()
                else -> false
            }
        } && !annotationEntry.text.contains("file:")

        if (hasInvalidSpacing) {
            annotationEntry.reportIssue("Annotations should occur immediately before the annotated construct.")
        }
        super.visitAnnotationEntry(annotationEntry)
    }

    private fun PsiWhiteSpace.isInvalid(): Boolean {
        return text.indexOf('\n') != text.lastIndexOf('\n')
    }

    private fun PsiComment.isInvalid(): Boolean {
        // Trailing comments (on the same line as the annotation) are always allowed
        val prev = prevLeaf()
        if (prev is PsiWhiteSpace && !prev.text.contains('\n')) return false
        return !text.containsPreprocessingPattern()
    }
}
