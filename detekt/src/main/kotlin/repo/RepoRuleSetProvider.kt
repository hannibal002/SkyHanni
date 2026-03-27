package at.hannibal2.skyhanni.detektrules.repo

import com.google.auto.service.AutoService
import dev.detekt.api.Config
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class RepoRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "RepoRules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                SkullTexturesUseRepo(config),
                RepoPatternRegexTestFailed(config),
                RepoPatternRegexTestMissing(config),
                RepoPatternUnnamedGroup(config),
            ),
        )
    }
}
