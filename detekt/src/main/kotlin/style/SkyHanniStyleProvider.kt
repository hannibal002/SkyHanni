package style

import com.google.auto.service.AutoService
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class SkyHanniStyleProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("SkyHanniStyle")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            mapOf(
                RuleName("InSkyBlockEarlyReturn") to ::InSkyBlockEarlyReturn,
                RuleName("IsInIslandEarlyReturn") to ::IsInIslandEarlyReturn,
                RuleName("OnlyOnIslandSpecificity") to ::OnlyOnIslandSpecificity,
            ),
        )
    }
}
