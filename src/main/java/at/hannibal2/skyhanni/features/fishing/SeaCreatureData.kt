package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.getLorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactBoundingBoxExtraEntities
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB

data class SeaCreatureData(
    val isOwn: Boolean,
    val seaCreature: SeaCreature,
    var entityId: Int,
    val spawnTime: SimpleTimeMark,
    var mob: Mob?,
) {

    /** This tracks the last position of the sea creature that the user was able to see. */
    var pos: LorenzVec?
        private set

    /** This tracks the last bounding box of the sea creature that the user was able to see. */
    var aabb: AABB?
        private set

    var lastUpdate: SimpleTimeMark = SimpleTimeMark.now()
        private set

    /** This tracks the real last position of the sea creature. Don't display this to the user */
    internal var actualLastPos: LorenzVec
        private set

    init {
        if (canBeSeen()) {
            aabb = mob?.boundingBox
            pos = mob?.getLorenzVec()
        } else {
            aabb = null
            pos = null
        }
        updateCanBeSeen()
        actualLastPos = mob?.getLorenzVec() ?: LorenzVec(0.0, 0.0, 0.0)
    }

    inline val name: String get() = seaCreature.name

    inline val displayName: String get() = seaCreature.displayName

    inline val isRare: Boolean get() = seaCreature.rare

    inline val rarity: LorenzRarity get() = seaCreature.rarity

    inline val health: Int? get() = mob?.health?.toInt()

    inline val despawnTime: SimpleTimeMark get() = spawnTime + SeaCreatureDetectionApi.DESPAWN_TIME

    fun isLoaded(): Boolean = entity != null

    val entity: LivingEntity? get() = mob?.baseEntity ?: EntityUtils.getEntityByID(entityId) as? LivingEntity

    private var hasDied: Boolean = false

    fun despawn() {
        SeaCreatureEvent.DeSpawn(this).post()
    }

    fun sendDeath(seenDeath: Boolean = true) {
        if (hasDied) return
        hasDied = true
        SeaCreatureEvent.Death(this, seenDeath).post()
    }

    private var hasRemoved: Boolean = false

    fun forceRemove() {
        if (hasRemoved) return
        hasRemoved = true
        SeaCreatureEvent.Remove(this).post()
    }

    private var canBeSeenCache = false

    fun canBeSeen(): Boolean = canBeSeenCache

    private fun updateCanBeSeen(): Boolean {
        val mob = mob ?: return false
        mob.baseEntity.canBeSeen()
        canBeSeenCache = mob.baseEntity.canBeSeen() || mob.extraEntities.any { it.canBeSeen() }
        return canBeSeenCache
    }

    @Suppress("HandleEventInspection")
    fun update(renderWorld: SkyHanniRenderWorldEvent) {
        lastUpdate = SimpleTimeMark.now()
        val mob = mob ?: return
        actualLastPos = mob.getLorenzVec()
        if (!updateCanBeSeen()) return
        aabb = renderWorld.exactBoundingBoxExtraEntities(mob)
        pos = renderWorld.exactLocation(mob)
    }

    override fun toString(): String {
        return buildString {
            fun appendInfo(string: String) = append("$string, ")
            append("SeaCreatureData(")
            appendInfo("isOwn=$isOwn")
            appendInfo("seaCreature=$seaCreature")
            appendInfo("entityId=$entityId")
            appendInfo("spawnTime=$spawnTime")
            appendInfo("mob=$mob")
            appendInfo("pos=$pos")
            append("aabb=$aabb")
            append(')')
        }
    }
}
