package at.hannibal2.skyhanni.detektrules.formatting

import com.google.auto.service.AutoService
import dev.detekt.api.Config
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class FormattingRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "FormattingRules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(ruleSetId, listOf(
            CustomAnnotationSpacing(config),
            CustomCommentSpacing(config),
            StorageVarOrVal(config),
        ))
    }
}
