package style

import SkyHanniRule
import dev.detekt.api.Config
import utils.DetektUtils.hasAnnotation
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

class InSkyBlockEarlyReturn(config: Config) : SkyHanniRule(
    config,
    ".inSkyBlock checks should be removed and replaced with onlyOnSkyblock = true in @HandleEvent annotation",
) {

    private fun KtIfExpression.containsInSkyBlockCheck(): Boolean = text.contains("SkyBlockUtils.inSkyBlock")
    private fun KtIfExpression.isEarlyReturn(): Boolean = then is KtReturnExpression

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (function.hasAnnotation("HandleEvent")) {
            function.bodyExpression?.accept(object : KtTreeVisitorVoid() {
                override fun visitIfExpression(expression: KtIfExpression) {
                    if (expression.containsInSkyBlockCheck() && expression.isEarlyReturn()) {
                        expression.reportIssue(
                            "This early return should be replaced with onlyOnSkyblock = true in @HandleEvent annotation"
                        )
                    }

                    super.visitIfExpression(expression)
                }
            })
        }

        super.visitNamedFunction(function)
    }
}
