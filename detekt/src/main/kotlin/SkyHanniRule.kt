import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule

abstract class SkyHanniRule(config: Config, description: String) : Rule(config, description) {

    protected fun PsiElement.reportIssue(message: String) {
        report(Finding(Entity.from(this), message))
    }
}
