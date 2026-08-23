package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isEmptyInvisibleArmorStand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ItemFrame
import java.awt.Color

class RenderEntityOutlineEvent(theType: Type?, potentialEntities: HashSet<Entity> = hashSetOf()) : SkyHanniEvent() {

    /**
     * The phase of the event (see [Type]
     */
    var type: Type? = null

    /**
     * The entities to outline. This is progressively cumulated from [.entitiesToChooseFrom]
     */
    var entitiesToOutline: HashMap<Entity, Color> = hashMapOf()

    /**
     * The entities we can outline. Note that this set and [.entitiesToOutline] are disjoint at all times.
     */
    var entitiesToChooseFrom: HashSet<Entity> = hashSetOf()

    /**
     * Whether [.entitiesToChooseFrom] has been computed already.
     */
    private var computed: Boolean = false

    /**
     * Constructs the event, given the type and optional entities to outline.
     *
     *
     * This will modify {@param potentialEntities} internally, so make a copy before passing it if necessary.
     *
     * @param theType of the event (see [Type]
     */
    init {
        type = theType
        entitiesToChooseFrom = potentialEntities
        if (!potentialEntities.isEmpty()) {
            entitiesToOutline = HashMap(potentialEntities.size)
        }
    }

    /**
     * Conditionally queue entities around which to render entities
     * Selects from the pool of [.entitiesToChooseFrom] to speed up the predicate testing on subsequent calls.
     * Is more efficient (theoretically) than calling [.queueEntityToOutline] for each entity because lists are handled internally.
     *
     *
     * This function loops through all entities and so is not very efficient.
     * It's advisable to encapsulate calls to this function with global checks (those not dependent on an individual entity) for efficiency purposes.
     *
     * @param outlineColor a function to test
     */
    fun queueEntitiesToOutline(outlineColor: ((entity: Entity) -> Color?)? = null) {
        if (outlineColor == null) {
            return
        }
        computeAndCacheEntitiesToChooseFrom()
        val itr: MutableIterator<Entity> = entitiesToChooseFrom.iterator()
        while (itr.hasNext()) {
            val e: Entity = itr.next()
            val i: Color? = outlineColor(e)
            if (i != null) {
                entitiesToOutline[e] = i
                itr.remove()
            }
        }
    }

    /**
     * Used for on-the-fly generation of entities. Driven by event handlers in a decentralized fashion
     */
    private fun computeAndCacheEntitiesToChooseFrom(force: Boolean = false) {
        if (computed && !force) return
        computed = true

        @OptIn(AllEntitiesGetter::class)
        val entities: List<Entity> = EntityUtils.getAllEntities().toList()
        // Only render outlines around non-null entities within the camera frustum
        entitiesToChooseFrom = HashSet(entities.size)
        // Empty invisible armor stands are common and never render an outlineable model
        for (entity in entities) {
            if (!entity.isEmptyInvisibleArmorStand() && entity !is ItemFrame) {
                entitiesToChooseFrom.add(entity)
            }
        }
        entitiesToOutline = HashMap(entitiesToChooseFrom.size)
    }

    /**
     * The phase of the event.
     * [.XRAY] means that this directly precedes entities whose outlines are rendered through walls (Vanilla 1.9+)
     * [.NO_XRAY] means that this directly precedes entities whose outlines are rendered only when visible to the client
     */
    enum class Type {
        XRAY,
        NO_XRAY
    }
}
