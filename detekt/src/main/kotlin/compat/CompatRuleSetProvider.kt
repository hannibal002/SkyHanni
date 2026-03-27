package at.hannibal2.skyhanni.detektrules.compat

import com.google.auto.service.AutoService
import dev.detekt.api.Config
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetProvider

@AutoService(RuleSetProvider::class)
class CompatRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "CompatRules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                MinecraftCompat(config),
                VanillaItemStackImport(config),
            ),
        )
    }
}
