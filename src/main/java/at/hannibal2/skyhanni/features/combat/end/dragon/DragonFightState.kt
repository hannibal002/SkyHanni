package at.hannibal2.skyhanni.features.combat.end.dragon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

/**
 * Plain state of the current dragon fight. Everything here is written by [DragonFightAPI] and
 * only read by the features - it holds no patterns and does no parsing of its own.
 */
@SkyHanniModule
object DragonFightState {

    /** Summoning eyes the player placed for the current dragon. */
    var eyesPlaced = 0

    var dragonSpawned = false
        set(value) {
            field = value
            if (value) eggSpawned = false
        }

    var eggSpawned = true

    var dragonType: DragonType? = null
        private set

    /** Own damage in the running fight, read from the scoreboard. */
    var yourDamage = 0.0

    fun onDragonSpawn(type: DragonType) {
        dragonSpawned = true
        dragonType = type
        DragonProfitTracker.addEyes(eyesPlaced)
    }

    fun reset() {
        eyesPlaced = 0
        dragonSpawned = false
        dragonType = null
        yourDamage = 0.0
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(78, "combat.dragon", "combat.endIsland.dragon")
        event.move(78, "combat.endstoneProtectorChat", "combat.endIsland.endstoneProtectorChat")
    }
}
