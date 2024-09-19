package at.hannibal2.skyhanni.detektrules.grammar

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class GrammarRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "grammar-rules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(ruleSetId, listOf(
            AvoidColour(config)
        ))
    }
}
