package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.isCorrupted
import at.hannibal2.skyhanni.utils.EntityUtils.isRunic
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.union
import at.hannibal2.skyhanni.utils.LorenzUtils.toSingletonListOrEmpty
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB

class Mob(
    var baseEntity: EntityLivingBase,
    val mobType: Type,
    val armorStand: EntityArmorStand? = null,
    val name: String = "",
    additionalEntities: List<EntityLivingBase>? = null,
    ownerName: String? = null,
    val hasStar: Boolean = false,
    val attribute: MobFilter.DungeonAttribute? = null,
    val levelOrTier: Int = -1,
) {

    val owner: MobUtils.OwnerShip?

    val hologram1 by lazy { MobUtils.getArmorStand(armorStand ?: baseEntity, 1) }
    val hologram2 by lazy { MobUtils.getArmorStand(armorStand ?: baseEntity, 2) }

    val extraEntities: List<EntityLivingBase>? get() = extraEntitiesList

    enum class Type {
        DisplayNPC, Summon, Basic, Dungeon, Boss, Slayer, Player, Projectile, Special;

        fun isSkyblockMob() = when (this) {
            Basic, Dungeon, Boss, Slayer -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return baseEntity.hashCode()
    }

    val isCorrupted get() = baseEntity.isCorrupted() // Can change
    val isRunic = baseEntity.isRunic() // Does not Change

    fun isInRender() = baseEntity.distanceToPlayer() < MobData.ENTITY_RENDER_RANGE_IN_BLOCKS

    fun canBeSeen() = baseEntity.canBeSeen()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Mob

        return baseEntity == other.baseEntity
    }

    private var extraEntitiesList = additionalEntities?.toMutableList()
    private var relativeBoundingBox: AxisAlignedBB?
    val boundingBox: AxisAlignedBB
        get() = (relativeBoundingBox?.offset(baseEntity.posX, baseEntity.posY, baseEntity.posZ)
            ?: baseEntity.entityBoundingBox).expandBlock()

    private val summonOwnerRegex = "Spawned by: (.*)".toRegex()

    init {
        removeExtraEntitiesFromChecking()
        relativeBoundingBox = makeRelativeBoundingBox()

        owner = (ownerName ?: if (mobType == Type.Slayer) hologram2?.let {
            summonOwnerRegex.find(it.cleanName())?.groupValues?.get(1)
        } else null)?.let { MobUtils.OwnerShip(it) }
    }

    private fun removeExtraEntitiesFromChecking() =
        extraEntities?.count { MobData.retries.contains(MobData.RetryEntityInstancing(it, 0)) }?.also {
            MobData.externRemoveOfRetryAmount += it
        }

    private fun makeRelativeBoundingBox() =
        (baseEntity.entityBoundingBox.union(extraEntities?.filter { it !is EntityArmorStand }
            ?.mapNotNull { it.entityBoundingBox }))?.offset(-baseEntity.posX, -baseEntity.posY, -baseEntity.posZ)

    fun internalAddEntity(entity: EntityLivingBase) {
        extraEntitiesList?.add(0, baseEntity) ?: run { extraEntitiesList = mutableListOf(baseEntity) }
        baseEntity = entity
        relativeBoundingBox = makeRelativeBoundingBox()
        MobData.entityToMob[entity] = this
    }

    fun internalAddEntity(entities: Collection<EntityLivingBase>) {
        val list = entities.drop(1).toMutableList().apply { add(baseEntity) }
        extraEntitiesList?.addAll(0, list) ?: run { extraEntitiesList = list }
        baseEntity = entities.first()
        relativeBoundingBox = makeRelativeBoundingBox()
        removeExtraEntitiesFromChecking()
        MobData.entityToMob.putAll(entities.associateWith { this })
    }

    fun internalUpdateOfEntity(entity: EntityLivingBase) {
        if (entity == baseEntity) baseEntity = entity else {
            extraEntitiesList?.remove(entity)
            extraEntitiesList?.add(entity)
        }
    }

    fun makeEntityToMobAssociation() =
        (baseEntity.toSingletonListOrEmpty() + (extraEntities ?: emptyList())).associateWith { this }


}
