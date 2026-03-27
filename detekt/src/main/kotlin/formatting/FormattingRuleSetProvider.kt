package formatting

import com.google.auto.service.AutoService
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class FormattingRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("FormattingRules")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            mapOf(
                RuleName("CustomAnnotationSpacing") to ::CustomAnnotationSpacing,
                RuleName("CustomCommentSpacing") to ::CustomCommentSpacing,
                RuleName("StorageVarOrVal") to ::StorageVarOrVal,
            ),
        )
    }
}
