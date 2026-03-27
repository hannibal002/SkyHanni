package at.hannibal2.skyhanni.detektrules.style

import at.hannibal2.skyhanni.detektrules.SkyHanniRule
import dev.detekt.api.Config
import dev.detekt.api.Debt
import dev.detekt.api.Issue
import dev.detekt.api.Severity
import dev.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class InSkyBlockEarlyReturn(config: Config) : SkyHanniRule(config) {
    override val issue = Issue(
        "InSkyBlockEarlyReturn",
        Severity.Style,
        ".inSkyBlock checks should be removed and replaced with onlyOnSkyblock = true in @HandleEvent annotation",
        Debt.FIVE_MINS
    )

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
