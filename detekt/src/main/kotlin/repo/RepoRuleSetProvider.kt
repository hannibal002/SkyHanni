package repo

import com.google.auto.service.AutoService
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class RepoRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("RepoRules")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            mapOf(
                RuleName("SkullTexturesUseRepo") to ::SkullTexturesUseRepo,
                RuleName("RepoPatternRegexTestFailed") to ::RepoPatternRegexTestFailed,
                RuleName("RepoPatternRegexTestMissing") to ::RepoPatternRegexTestMissing,
                RuleName("RepoPatternUnnamedGroup") to ::RepoPatternUnnamedGroup,
            ),
        )
    }
}
