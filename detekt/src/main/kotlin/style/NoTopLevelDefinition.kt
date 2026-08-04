package style

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

class NoTopLevelDefinition(config: Config) :
    SkyHanniRule(config, "Do not allow top level definitions of functions or properties. Put them inside a class or object instead.") {

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (function.parent !is KtFile) return
        val receiver = function.receiverTypeReference?.text
        // Allow companion object extensions.
        if (receiver?.endsWith(".Companion") == true) {
            return
        }

        function.reportIssue("Top level function definitions are not allowed. Put them inside a class or object instead.")
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.parent is KtFile) {
            property.reportIssue("Top level property definitions are not allowed. Put them inside a class or object instead.")
        }
    }
}
