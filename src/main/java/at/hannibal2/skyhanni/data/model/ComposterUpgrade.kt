package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class ComposterUpgrade(val displayName: String, val slotNumber: Int) {
    COMPOSTER_SPEED("Composter Speed", 20),
    MULTI_DROP("Multi Drop", 21),
    FUEL_CAP("Fuel Cap", 22),
    ORGANIC_MATTER_CAP("Organic Matter Cap", 23),
    COST_REDUCTION("Cost Reduction", 24),
    ;

    companion object {

        private fun regexValues() = entries.joinToString("|") { it.displayName }

        val composterUpgradePattern by RepoPattern.pattern(
            "composter.upgrade",
            "(?<name>${regexValues()})(?: (?<level>.*))?",
        )

        fun getByName(name: String) = entries.firstOrNull { it.displayName == name }
    }
}
