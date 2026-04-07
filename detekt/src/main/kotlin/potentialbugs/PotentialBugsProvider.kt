package potentialbugs

import com.google.auto.service.AutoService
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class PotentialBugsProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("potential-bugs")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            mapOf(
                RuleName("ImmutableTypesWithExpectedInteriorMutabilityInConfig") to ::ImmutableTypesWithExpectedInteriorMutabilityInConfig,
                RuleName("StorageNeedsExpose") to ::StorageNeedsExpose,
                RuleName("NonStorageDoesntNeedExpose") to ::NonStorageDoesntNeedExpose,
            ),
        )
    }
}
