package imports

import com.google.auto.service.AutoService
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class ImportRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("ImportRules")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            mapOf(
                RuleName("CustomImportOrdering") to ::CustomImportOrdering,
                RuleName("PreprocessingImportOrdering") to ::PreprocessingImportOrdering,
            ),
        )
    }
}
