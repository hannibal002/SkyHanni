package imports

import PreprocessingPattern.Companion.containsPreprocessingPattern
import SkyHanniRule
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtImportList

class PreprocessingImportOrdering(config: Config) : SkyHanniRule(config, "Enforces that pre-processing comments do not accidentally get messed up.") {

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
