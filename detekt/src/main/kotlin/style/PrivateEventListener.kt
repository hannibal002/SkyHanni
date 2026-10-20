package style

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import utils.DetektUtils.hasAnnotation

class PrivateEventListener(config: Config) : SkyHanniRule(
    config,
    "Event listener should be private.",
) {
    override fun visitNamedFunction(function: KtNamedFunction) {
        if (function.hasAnnotation("HandleEvent") && !isEffectivelyPrivate(function)) {
            function.reportIssue(
                "Event listener functions should be private.",
            )
        }
        super.visitNamedFunction(function)
    }

    private fun isEffectivelyPrivate(function: KtNamedFunction): Boolean {
        if (function.hasModifier(KtTokens.PRIVATE_KEYWORD)) return true

        // If the function is inside a private class, it is effectively private
        val containingClass = function.getParentOfType<KtClassOrObject>(true)
        return containingClass?.hasModifier(KtTokens.PRIVATE_KEYWORD) == true
    }
}
