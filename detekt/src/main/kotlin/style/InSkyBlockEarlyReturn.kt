package style

import SkyHanniRule
import dev.detekt.api.Config
import utils.DetektUtils.hasAnnotation
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class InSkyBlockEarlyReturn(config: Config) : SkyHanniRule(
    config,
    ".inSkyBlock checks should be removed and replaced with onlyOnSkyblock = true in @HandleEvent annotation",
) {

    private fun KtExpression.containsInSkyBlockCheck(): Boolean = text.contains("SkyBlockUtils.inSkyBlock")
    private fun KtExpression.isEarlyReturn(): Boolean = this is KtIfExpression && then is KtReturnExpression

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (function.hasAnnotation("HandleEvent")) {
            val bodyExpressions = function.bodyExpression?.collectDescendantsOfType<KtIfExpression>() ?: return

            for (ifExpression in bodyExpressions) {
                if (ifExpression.containsInSkyBlockCheck() && ifExpression.isEarlyReturn()) {
                    ifExpression.reportIssue("This early return should be replaced with onlyOnSkyblock = true in @HandleEvent annotation")
                }
            }
        }

        super.visitNamedFunction(function)
    }
}
