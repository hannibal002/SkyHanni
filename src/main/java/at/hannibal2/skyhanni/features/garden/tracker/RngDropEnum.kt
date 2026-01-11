package at.hannibal2.skyhanni.features.garden.tracker

enum class RngDropEnum(private val displayName: String, private val matchName: String) {
    UNCOMMON("§a§lUNCOMMON DROP", "uncommon drop"),
    RARE("§9§lRARE DROP", "rare drop"),
    CRAZY_RARE("§d§lCRAZY RARE DROP", "crazy rare drop"),
    PRAY_RNGESUS("§5§lPRAY TO RNGESUS DROP", "pray to rngesus drop")
    ;

    override fun toString() = displayName
    companion object {
        fun getByNameOrNull(name: String): RngDropEnum? {
            return entries.firstOrNull {
                it.matchName.equals(name, ignoreCase = true)
            }
        }
    }
}
