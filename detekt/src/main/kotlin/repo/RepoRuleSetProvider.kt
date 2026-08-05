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
        val context = RepoPatternContext()

        return RuleSet(
            ruleSetId,
            mapOf(
                RuleName("SkullTexturesUseRepo") to ::SkullTexturesUseRepo,
                RuleName("RepoPatternRegexTestFailed") to { config -> RepoPatternRegexTestFailed(config, context) },
                RuleName("RepoPatternRegexTestMissing") to { config -> RepoPatternRegexTestMissing(config, context) },
                RuleName("RepoPatternUnnamedGroup") to { config -> RepoPatternUnnamedGroup(config, context) },
                RuleName("RepoPatternPassesShapeRequirement") to { config -> RepoPatternPassesShapeRequirement(config, context) },
                RuleName("RepoPatternVariableNameStyle") to { config -> RepoPatternVariableNameStyle(config, context) },
            ),
        )
    }
}
