package at.hannibal2.skyhanni.data.model

enum class ComposterUpgrade(val displayName: String) {
    COMPOSTER_SPEED("Composter Speed"),
    ORGANIC_MATTER_CAP("Organic Matter Cap"),
    MULTI_DROP("Multi Drop"),
    COST_REDUCTION("Cost Reduction"),
    FUEL_CAP("Fuel Cap"),
    ;

    companion object {
        private fun regexValues() = values().joinToString("|") { it.displayName }

        val regex = "§a(?<name>${regexValues()})(?: (?<level>.*))?".toPattern()

        fun getByName(name: String) = values().firstOrNull { it.displayName == name }
    }
}