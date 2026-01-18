package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.fishing.LivingSeaCreatureData

sealed class SeaCreatureEvent(val seaCreature: LivingSeaCreatureData) : SkyHanniEvent() {

    /** Gets called when a Sea Creature is initially detected. */
    class Spawn(seaCreature: LivingSeaCreatureData) : SeaCreatureEvent(seaCreature)

    /** Gets called when a Sea Creature's mob disappears, no matter the cause. */
    class DeSpawn(seaCreature: LivingSeaCreatureData) : SeaCreatureEvent(seaCreature)

    /** Gets called when a Sea Creature is removed from the sea creature list entirely. */
    class Remove(seaCreature: LivingSeaCreatureData) : SeaCreatureEvent(seaCreature)

    /** Gets called when a Sea Creature dies. */
    class Death(seaCreature: LivingSeaCreatureData, val seenDeath: Boolean) : SeaCreatureEvent(seaCreature)

    /** Gets called when a Sea Creature is re-detected after despawning. */
    class ReDetect(seaCreature: LivingSeaCreatureData) : SeaCreatureEvent(seaCreature)

    inline val name: String get() = seaCreature.name
    inline val isOwn: Boolean get() = seaCreature.isOwn
    inline val isRare: Boolean get() = seaCreature.isRare
}
