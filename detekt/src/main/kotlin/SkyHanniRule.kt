package at.hannibal2.skyhanni.detektrules

import dev.detekt.api.CodeSmell
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Rule
import org.jetbrains.kotlin.com.intellij.psi.PsiElement

abstract class SkyHanniRule(config: Config) : Rule(config) {

    protected fun PsiElement.reportIssue(message: String) {
        report(CodeSmell(issue, Entity.from(this), message))
    }
}
