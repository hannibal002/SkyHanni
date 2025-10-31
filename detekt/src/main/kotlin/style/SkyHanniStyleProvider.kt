package at.hannibal2.hanni.detektrules.style

import com.google.auto.service.AutoService
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class HanniStyleProvider : RuleSetProvider {
    override val ruleSetId: String = "HanniStyle"

    override fun instance(config: Config): RuleSet {
        return RuleSet(ruleSetId, listOf(
            InSkyBlockEarlyReturn(config),
            IsInIslandEarlyReturn(config),
            OnlyOnIslandSpecificity(config),
        ))
    }
}
