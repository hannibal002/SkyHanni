package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import kotlin.reflect.KClass

@Retention(RUNTIME)
@Target(FUNCTION)
annotation class HandleEvent(
    /**
     * For cases where the event properties are themselves not needed, and solely a listener for an event fire suffices.
     * To specify multiple events, use [eventTypes] instead.
     */
    @Deprecated("Use primary function name or explicit type parameter instead")
    val eventType: KClass<out AbstractSkyHanniEvent> = AbstractSkyHanniEvent::class,

    /**
     * For cases where multiple events are listened to, and properties are unnecessary.
     * To specify only one event, use [eventType] instead.
     */
    @Deprecated("Use primary function name or explicit type parameter instead")
    val eventTypes: Array<KClass<out AbstractSkyHanniEvent>> = [],

    /**
     * If the event should only be received while on SkyBlock.
     */
    val onlyOnSkyblock: Boolean = false,

    /**
     * If the event should only be handled while on SkyBlock, or while
     * outside SkyBlock with certain [OutsideSBFeature]s being enabled.
     */
    val onlyOnSkyblockOrFeatures: Array<OutsideSBFeature> = [],

    /**
     * If the event should only be received while on a specific SkyBlock island.
     * To specify multiple islands, use [onlyOnIslands] instead.
     */
    val onlyOnIsland: IslandType = ANY,

    /**
     * If the event should only be received while on an island within specified
     * [IslandTypeTag]s.
     */
    val onlyOnIslandTypeTag: Array<IslandTypeTag> = [],

    /**
     * If the event should only be received while being on specific SkyBlock islands.
     * To specify only one island, use [onlyOnIsland] instead.
     */
    vararg val onlyOnIslands: IslandType = [],

    /**
     * The order the event handler will be called in relative to the other handlers of the same event.
     * Higher priority means it will be called earlier.
     */
    val priority: Int = 0,

    /**
     * If the event is cancelled & receiveCancelled is true, then the method will still invoke.
     */
    val receiveCancelled: Boolean = false,
) {
    companion object {
        const val HIGHEST = -2 // First to execute
        const val HIGH = -1
        const val LOW = 1
        const val LOWEST = 2 // Last to execute
    }
}
