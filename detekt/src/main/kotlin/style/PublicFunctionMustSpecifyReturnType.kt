package style

import SkyHanniRule
import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.decompiler.psi.text.getAllModifierLists
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import utils.DetektUtils.hasAnnotation

class PublicFunctionMustSpecifyReturnType(config: Config) :
    SkyHanniRule(
        config,
        "Public functions should have an explicit return type. " +
                "Inferred return types can easily be changed by mistake which may lead to breaking changes.",
    ), RequiresAnalysisApi {

    private val ignoredModifiers = listOf(
        KtTokens.OVERRIDE_KEYWORD,
        KtTokens.OPERATOR_KEYWORD,
        KtTokens.PRIVATE_KEYWORD,
        KtTokens.INTERNAL_KEYWORD,
        KtTokens.PROTECTED_KEYWORD,
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        function.check()
        super.visitNamedFunction(function)
    }

    private fun KtNamedFunction.check() {
        modifierList?.let {
            ignoredModifiers.forEach { modifier ->
                if (it.hasModifier(modifier)) return
            }
        }

        if (typeReference != null) return
        if (containingClassOrObject?.isLocal == true) return

        if (isLocal || hasBlockBody() || bodyExpression?.text == "Unit") return
        // stuff like `fun foo() = bar()` is fine
        if (bodyExpression is KtCallExpression) return

        if (hasAnnotation("HandleEvent")) return

        analyze(this) {
            if (symbol.returnType.isUnitType) return
        }

        reportIssue(
            "Public function '${nameAsSafeName.asString()}' without explicit return type.",
        )
    }
}
