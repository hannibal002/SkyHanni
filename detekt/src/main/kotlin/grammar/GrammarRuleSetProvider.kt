package grammar

import com.google.auto.service.AutoService
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class GrammarRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("GrammarRules")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            mapOf(
                RuleName("AvoidBritishSpelling") to ::AvoidBritishSpelling,
            ),
        )
    }
}
