package compat

import com.google.auto.service.AutoService
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class CompatRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("CompatRules")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            mapOf(
                RuleName("MinecraftCompat") to ::MinecraftCompat,
            ),
        )
    }
}
