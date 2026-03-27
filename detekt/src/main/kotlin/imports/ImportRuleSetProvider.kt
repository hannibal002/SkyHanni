package at.hannibal2.skyhanni.detektrules.imports

import com.google.auto.service.AutoService
import dev.detekt.api.Config
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class ImportRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "ImportRules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(ruleSetId, listOf(
            CustomImportOrdering(config),
            PreprocessingImportOrdering(config),
        ))
    }
}
