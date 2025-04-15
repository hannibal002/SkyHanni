package at.hannibal2.skyhanni.detektrules.imports

import at.hannibal2.skyhanni.detektrules.PreprocessingPattern.Companion.containsPreprocessingPattern
import at.hannibal2.skyhanni.detektrules.SkyHanniRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtImportList

class PreprocessingImportOrdering(config: Config) : SkyHanniRule(config) {
    override val issue = Issue(
        "PreprocessingImportOrdering",
        Severity.Style,
        "Enforces that pre-processing comments do not accidentally get messed up.",
        Debt.FIVE_MINS,
    )

    override fun visitImportList(importList: KtImportList) {
        val parent = importList.parent
        val siblings = parent?.children?.filter { it !is PsiWhiteSpace } ?: return
        val importListIndex = siblings.indexOf(importList)

        if (importListIndex > 0) {
            val previousElement = siblings[importListIndex - 1]
            if (previousElement is PsiComment) {
                if (previousElement.text.containsPreprocessingPattern()) {
                    previousElement.reportIssue(
                        "Imports begin with a pre-processed comment, make sure to check the order of the imports. " +
                            "If this pre-processing comment is correct you can baseline this error. " +
                            "If you did accidentally change the order of the imports fix them.",
                    )
                }
            }
        }
        super.visitImportList(importList)
    }
}
