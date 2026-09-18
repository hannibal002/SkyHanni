package at.hannibal2.skyhanni.features.combat.end.dragon


/**
 * Plain state of the current dragon fight. Everything here is written by [DragonFightAPI] and
 * only read by the features - it holds no patterns and does no parsing of its own.
 */
object DragonFightState {

    /** Summoning eyes the player placed for the current dragon. */
    var eyesPlaced = 0

    /**
     * Whether the egg is still waiting to be summoned. It turns false the moment a dragon is up,
     * which is what [DragonLootDetection] waits for.
     */
    var eggSpawned = true

    var dragonType: DragonType? = null
        private set

    /** Own damage in the running fight, read from the scoreboard. */
    var yourDamage = 0.0

    fun onDragonSpawn(type: DragonType) {
        eggSpawned = false
        dragonType = type
        DragonProfitTracker.addEyes(eyesPlaced)
    }

    fun reset() {
        eyesPlaced = 0
        dragonType = null
        yourDamage = 0.0
    }
}
