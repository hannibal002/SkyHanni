package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isEmptyInvisibleArmorStand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ItemFrame
import java.awt.Color

@PrimaryFunction("onRenderEntityOutline")
class RenderEntityOutlineEvent : SkyHanniEvent() {
    /**
     * The entities to outline. This is progressively cumulated from [.entitiesToChooseFrom]
     */
    var entitiesToOutline: HashMap<Entity, Int> = hashMapOf()

    /**
     * The entities we can outline. Note that this set and [.entitiesToOutline] are disjoint at all times.
     */
    var entitiesToChooseFrom: HashSet<Entity> = hashSetOf()

    /**
     * Whether [.entitiesToChooseFrom] has been computed already.
     */
    private var computed: Boolean = false

    /**
     * Conditionally queue entities around which to render outlines.
     * Selects from the pool of [.entitiesToChooseFrom] to speed up the predicate testing on subsequent calls.
     * Is more efficient (theoretically) than calling [.queueEntityToOutline] for each entity because lists are handled internally.
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
        val iterator: MutableIterator<Entity> = entitiesToChooseFrom.iterator()
        while (iterator.hasNext()) {
            val entity: Entity = iterator.next()
            val color: Color? = outlineColor(entity)
            if (color != null) {
                entitiesToOutline[entity] = color.rgb
                iterator.remove()
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
}
