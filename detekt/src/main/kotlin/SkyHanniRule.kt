import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Location
import dev.detekt.api.Rule
import dev.detekt.api.SourceLocation
import dev.detekt.api.TextLocation
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnRangeInPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.nio.file.Path
import kotlin.io.path.Path

abstract class SkyHanniRule(config: Config, description: String) : Rule(config, description) {

    protected fun PsiElement.reportIssue(message: String) {
        report(Finding(Entity.from(this), message))
    }

    // Reference: https://github.com/detekt/detekt/
    protected fun reportIssue(
        message: String,
        lineIndex: Int,
        line: String,
        file: KtFile,
        sourceFileLinesMapping: KtPsiSourceFileLinesMapping? = null,
    ) {
        val sourceFileLinesMapping = sourceFileLinesMapping ?: KtPsiSourceFileLinesMapping(file)

        val offset = sourceFileLinesMapping.getLineStartOffset(lineIndex)
        val ktElement = findFirstMeaningfulKtElementInParents(file, offset, line) ?: file
        val textRange = TextRange(offset, offset + line.length)
        val lineAndColumnRange = getLineAndColumnRangeInPsiFile(file, textRange)

        val location = Location(
            source = SourceLocation(lineAndColumnRange.start.line, lineAndColumnRange.start.column),
            endSource = SourceLocation(lineAndColumnRange.end.line, lineAndColumnRange.end.column),
            text = TextLocation(offset, offset + line.length),
            path = file.absolutePath(),
        )

        report(Finding(Entity.from(ktElement, location), message))
    }

    companion object {
        private val BLANK_OR_QUOTES = """[\s"]*""".toRegex()

        private fun findFirstMeaningfulKtElementInParents(
            file: KtFile,
            offset: Int,
            line: String,
        ): PsiElement? =
            findKtElementInParents(file, offset, line)
                .firstOrNull { !BLANK_OR_QUOTES.matches(it.text) }

        private fun KtFile.absolutePath(): Path = Path(virtualFilePath)

        private fun findKtElementInParents(file: KtFile, offset: Int, line: String): Sequence<PsiElement> =
            file.elementsInRange(TextRange.create(offset, offset + line.length))
                .asSequence()
                .plus(file.findElementAt(offset))
                .mapNotNull { it?.getNonStrictParentOfType() }

    }
}
