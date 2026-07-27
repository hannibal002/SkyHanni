package compat

import SkyHanniRule
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.config
import dev.detekt.api.valuesWithReason
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.KaCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundArrayAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaDelegatedPropertyCall
import org.jetbrains.kotlin.analysis.api.resolution.KaForLoopCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.psiUtil.isDotSelector
import org.jetbrains.kotlin.resolve.calls.util.asCallableReferenceExpression
import org.jetbrains.kotlin.resolve.calls.util.getCalleeExpressionIfAny
import utils.FunctionMatcher
import utils.FunctionMatcher.Companion.fromFunctionSignature

/**
 * This file is copied from:
 * https://github.com/detekt/detekt/blob/main/detekt-rules-style/src/main/kotlin/dev/detekt/rules/style/ForbiddenMethodCall.kt
 * With any differences having a comment "SKYHANNI:" in front of it.
 *
 * Reports all method or property invocations that are forbidden.
 */
// TODO: Replace with the offical detekt rule when it supports public java properties
class ForbiddenMethodCall(config: Config) :
    SkyHanniRule(
        config,
        "Mark forbidden methods or properties. A forbidden method/property could be unstable, " +
            "experimental or unsafe and hence you might want to mark it as forbidden."
    ),
    RequiresAnalysisApi {

    @Configuration(
        "List of fully qualified method or property signatures which are forbidden."
    )
    private val methods: List<ForbiddenMethod> by config(
        valuesWithReason(
            "kotlin.io.print" to "print does not allow you to configure the output stream. Use a logger instead.",
            "kotlin.io.println" to "println does not allow you to configure the output stream. Use a logger instead.",
            "java.math.BigDecimal.<init>(kotlin.Double)" to
                "using `BigDecimal(Double)` can result in unexpected floating point precision behavior.",
            "java.math.BigDecimal.<init>(kotlin.String)" to
                "using `BigDecimal(String)` can result in a NumberFormatException.",
            "kotlin.system.measureTimeMillis" to
                "It is marked as obsolete. Use `kotlin.time.measureTime` instead.",
        )
    ) { list ->
        list.map {
            ForbiddenMethod(
                fromFunctionSignature(it.value),
                it.reason
            )
        }
    }

    // SKYHANNI: Ignore the compat folder
    private fun shouldIgnore(element: KtExpression): Boolean {
        val filePath = element.containingFile.virtualFile.path
        return filePath.contains("at\\hannibal2\\skyhanni\\utils\\compat") ||
            filePath.contains("at/hannibal2/skyhanni/utils/compat")
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        check(expression)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)
        check(expression.operationReference)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        if (expression.getCalleeExpressionIfAny()?.isDotSelector() == true) {
            check(expression)
        }
    }

    override fun visitPrefixExpression(expression: KtPrefixExpression) {
        super.visitPrefixExpression(expression)
        check(expression.operationReference)
    }

    override fun visitPostfixExpression(expression: KtPostfixExpression) {
        super.visitPostfixExpression(expression)
        check(expression.operationReference)
    }

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
        super.visitCallableReferenceExpression(expression)
        check(expression.callableReference)
    }

    private fun check(expression: KtExpression) {
        // SKYHANNI: Ignore the compat folder
        if (shouldIgnore(expression)) return
        analyze(expression) {
            val call = expression.resolveToCall()
                ?: expression.asCallableReferenceExpression()?.resolveToCall()
                ?: return

            val successfulCall = call.successfulCallOrNull<KaCall>()
                ?: return

            getCallInfos(successfulCall).forEach { (_, symbol) ->
                val symbol = symbol ?: return@forEach
                val forbiddenMethod = methods.find { method ->
                    method.value.match(null, symbol)
                }

                if (forbiddenMethod != null) {
                    val message = if (forbiddenMethod.reason != null) {
                        "The method `${forbiddenMethod.value}` has been forbidden: ${forbiddenMethod.reason}"
                    } else {
                        "The method `${forbiddenMethod.value}` has been forbidden in the detekt config."
                    }

                    // SKYHANNI: we got reportIssue from the SkyHanniRule
                    expression.reportIssue(message)
                }
            }
        }
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.getCallInfos(
        kaCall: KaCall,
    ): Sequence<Pair<KaPropertySymbol?, KaCallableSymbol?>> =
        sequence {
            val symbols = when (kaCall) {
                is KaCallableMemberCall<*, *> -> {
                    val expressionSymbol = kaCall.partiallyAppliedSymbol.symbol

                    sequenceOf(expressionSymbol)
                        .plus(expressionSymbol.allOverriddenSymbols)
                        .map {
                            // SKYHANNI: Made it support direct property access (java field access) as well as getter/setter access
                            if (it is KaPropertySymbol) {
                                it to it
                            } else {
                                null to it
                            }
                        }
                }

                is KaCompoundAccessCall -> sequenceOf(
                    null to kaCall.compoundOperation.operationCall.symbol
                )

                is KaCompoundArrayAccessCall -> null
                is KaCompoundVariableAccessCall -> null
                is KaDelegatedPropertyCall -> null
                is KaForLoopCall -> null
            } ?: return@sequence

            yieldAll(symbols)
        }

    internal data class ForbiddenMethod(
        val value: FunctionMatcher,
        val reason: String?,
    )
}
