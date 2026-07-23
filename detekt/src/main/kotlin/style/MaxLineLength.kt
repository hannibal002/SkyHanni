package style

import SkyHanniRule
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.nextLeaf
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Location
import dev.detekt.api.SourceLocation
import dev.detekt.api.TextLocation
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnRangeInPsiFile
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.nio.file.Path
import kotlin.io.path.Path

// Reference: https://github.com/detekt/detekt/blob/ff22c8aff2b5f14acd3a341f2b3d99a33189c117/detekt-rules-style/src/main/kotlin/dev/detekt/rules/style/MaxLineLength.kt
/**
 * This rule reports lines of code which exceed a defined maximum line length.
 *
 * Strings, comments, and KDoc contents are ignored when calculating line length.
 * If the line is inside a function, it will still count string length.
 */
class MaxLineLength(config: Config) :
    SkyHanniRule(config, "Line detected, which is longer than the defined maximum line length in the code style.") {

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val sourceFileLinesMapping = KtPsiSourceFileLinesMapping(file)
        file.text.lineSequence().withIndex()
            .filterNot { (index, line) ->
                isValidLine(file, sourceFileLinesMapping.getLineStartOffset(index), line)
            }
            .forEach { (index, line) ->
                reportIssue(description, index, line, file, sourceFileLinesMapping)
            }
    }

    private fun isValidLine(file: KtFile, offset: Int, line: String): Boolean {
        if (line.length <= DEFAULT_IDEA_LINE_LENGTH) {
            return true
        }

        val length = if (isInsideFunction(file, offset)) {
            getLineLength(file, offset, line, ignoreStrings = false)
        } else {
            getLineLength(file, offset, line, ignoreStrings = true)
        }

        return length <= DEFAULT_IDEA_LINE_LENGTH
    }

    private fun isInsideFunction(file: KtFile, offset: Int): Boolean =
        file.findElementAt(offset)?.let {
            it.getNonStrictParentOfType<KtNamedFunction>() != null ||
                it.getNonStrictParentOfType<KtPropertyAccessor>() != null ||
                it.getNonStrictParentOfType<KtAnonymousInitializer>() != null ||
                it.getNonStrictParentOfType<KtLambdaExpression>() != null
        } == true

    private fun getLineLength(
        file: KtFile,
        offset: Int,
        line: String,
        ignoreStrings: Boolean,
    ): Int {
        val endOffset = offset + line.length
        val firstLeaf = file.findElementAt(offset) ?: return line.length

        var length = 0
        var leaf: PsiElement? = firstLeaf

        while (leaf != null && leaf.textRange.startOffset < endOffset) {
            val ignored =
                leaf.getNonStrictParentOfType<KDoc>() != null ||
                    leaf.node.elementType == KtTokens.BLOCK_COMMENT ||
                    leaf.node.elementType == KtTokens.EOL_COMMENT ||
                    (ignoreStrings && leaf.getNonStrictParentOfType<KtStringTemplateExpression>() != null)

            if (
                !ignored &&
                leaf.textRange.endOffset > offset &&
                leaf.textRange.startOffset < endOffset
            ) {
                val start = maxOf(leaf.textRange.startOffset, offset)
                val end = minOf(leaf.textRange.endOffset, endOffset)
                length += end - start
            }

            leaf = leaf.nextLeaf()
        }

        return length
    }

    companion object {
        private const val DEFAULT_IDEA_LINE_LENGTH = 140
    }
}
