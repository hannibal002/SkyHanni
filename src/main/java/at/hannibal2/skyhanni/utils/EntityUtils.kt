package at.hannibal2.skyhanni.utils import at.hannibal2.skyhanni.utils.compat.deceased import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets import at.hannibal2.skyhanni.utils.compat.findHealthReal

import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToIgnoreY
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.getAllEquipment
import at.hannibal2.skyhanni.utils.compat.getEntityLevel
import at.hannibal2.skyhanni.utils.compat.getHandItem
import at.hannibal2.skyhanni.utils.compat.getLoadedPlayers
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.compat.normalizeAsArray
import at.hannibal2.skyhanni.utils.render.FrustumUtils
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.mob.EndermanEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.math.Box
//#if MC > 1.21
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.EquipmentSlot
//#else
//$$ import net.minecraft.entity.SharedMonsterAttributes
//$$
//#endif

@RequiresOptIn(
    "getAllEntities or getEntities should only be used when necessary," +
        "as they can be expensive, since they iterate through all entities in world."
)
annotation class AllEntitiesGetter

@SkyHanniModule
object EntityUtils {

    inline val ALWAYS get(): (Entity) -> Boolean = { true }

    // TODO remove this relatively heavy call everywhere
    @Deprecated("Use Mob Detection Instead")
    fun LivingEntity.hasNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): Boolean = getNameTagWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity) != null

    fun getPlayerEntities(): MutableList<OtherClientPlayerEntity> {
        val list = mutableListOf<OtherClientPlayerEntity>()
        for (entity in MinecraftCompat.localWorldOrNull?.getLoadedPlayers().orEmpty()) {
            if (!entity.isNpc() && entity is OtherClientPlayerEntity) {
                list.add(entity)
            }
        }
        return list
    }

    @Deprecated("Use Mob Detection Instead")
    fun LivingEntity.getAllNameTagsInRadiusWith(
        contains: String,
        radius: Double = 3.0,
    ): List<ArmorStandEntity> = getArmorStandsInRadius(getLorenzVec().up(3), radius).filter {
        it.name.formattedTextCompatLessResets().contains(contains)
    }

    @Deprecated("Use Mob Detection Instead")
    fun LivingEntity.getNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): ArmorStandEntity? = getAllNameTagsWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity).firstOrNull()

    @Deprecated("Use Mob Detection Instead")
    fun LivingEntity.getAllNameTagsWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): List<ArmorStandEntity> {
        val center = getLorenzVec().up(y)
        return getArmorStandsInRadius(center, inaccuracy).filter {
            val result = it.name.formattedTextCompatLessResets().contains(contains)
            if (debugWrongEntity && !result) {
                ChatUtils.consoleLog("wrong entity in aabb: '" + it.name.formattedTextCompatLessResets() + "'")
            }
            if (debugRightEntity && result) {
                ChatUtils.consoleLog("mob: " + center.printWithAccuracy(2))
                ChatUtils.consoleLog("nametag: " + it.getLorenzVec().printWithAccuracy(2))
                ChatUtils.consoleLog("accuracy: " + (it.getLorenzVec() - center).printWithAccuracy(3))
            }
            result
        }
    }

    private fun getArmorStandsInRadius(center: LorenzVec, radius: Double): List<ArmorStandEntity> {
        val a = center.add(-radius, -radius - 3, -radius)
        val b = center.add(radius, radius + 3, radius)
        val alignedBB = a.axisAlignedTo(b)
        return getEntitiesInBoundingBox<ArmorStandEntity>(alignedBB)
    }

    @Deprecated("Old. Instead use entity detection feature instead.")
    fun LivingEntity.hasBossHealth(health: Int): Boolean = this.hasMaxHealth(health, true)

    @Deprecated("Old. Instead use entity detection feature instead.")
    fun LivingEntity.hasMaxHealth(health: Int, boss: Boolean = false, maxHealth: Int = baseMaxHealth): Boolean {
        val derpyMultiplier = if (ElectionApi.isDerpy) 2.0 else if (ElectionApi.isAura) 1.1 else 1.0
        if (maxHealth == (health * derpyMultiplier).toInt()) return true

        if (!boss && !DungeonApi.inDungeon()) {
            // Corrupted
            if (maxHealth == (health * 3 * derpyMultiplier).toInt()) return true
            // Runic
            if (maxHealth == (health * 4 * derpyMultiplier).toInt()) return true
            // Corrupted+Runic
            if (maxHealth == (health * 12 * derpyMultiplier).toInt()) return true
        }

        return false
    }

    fun PlayerEntity.getSkinTexture(): String? {
        val gameProfile = gameProfile ?: return null

        return gameProfile.properties.entries()
            .filter { it.key == "textures" }
            .map { it.value }
            .firstOrNull { it.name == "textures" }?.value
    }

    inline fun <reified T : Entity> getEntitiesNextToPlayer(radius: Double, noinline predicate: (T) -> Boolean = ALWAYS): List<T> =
        getEntitiesNearby<T>(LocationUtils.playerLocation(), radius, predicate)

    // First filters for a bounding box because it's faster, and then filters based on distance
    inline fun <reified T : Entity> getEntitiesNearby(
        location: LorenzVec,
        radius: Double,
        noinline predicate: (T) -> Boolean = ALWAYS,
    ): List<T> {
        return getEntitiesInBox<T>(location, radius) { it.distanceTo(location) < radius && predicate(it) }
    }

    @AllEntitiesGetter
    inline fun <reified T : Entity> getEntitiesNearbyIgnoreY(location: LorenzVec, radius: Double): Sequence<T> =
        getEntities<T>().filter { it.distanceToIgnoreY(location) < radius }

    fun LivingEntity.isAtFullHealth() = baseMaxHealth == findHealthReal().toInt()

    @Deprecated("Use specific methods instead, such as wearingSkullTexture or holdingSkullTexture")
    fun ArmorStandEntity.hasSkullTexture(skin: String): Boolean {
        val inventory = this.getAllEquipment() ?: return false
        return inventory.any { it != null && it.getSkullTexture() == skin }
    }

    fun ArmorStandEntity.getWornSkullTexture(): String? = getStandHelmet()?.getSkullTexture()
    fun ArmorStandEntity.wearingSkullTexture(skin: String) = getWornSkullTexture() == skin
    fun ArmorStandEntity.holdingSkullTexture(skin: String) = getHandItem()?.getSkullTexture() == skin

    fun PlayerEntity.isNpc() = !isRealPlayer()

    //#if MC < 1.21
    //$$ fun LivingEntity.getArmorInventory(): Array<ItemStack?>? =
    //$$     if (this is PlayerEntity) inventory.armor.normalizeAsArray() else null
    //#else
    fun LivingEntity.getArmorInventory(): Array<ItemStack?>? {
        if (this !is PlayerEntity) return null
        return buildList {
            add(inventory.equipment.get(EquipmentSlot.FEET).orNull())
            add(inventory.equipment.get(EquipmentSlot.LEGS).orNull())
            add(inventory.equipment.get(EquipmentSlot.CHEST).orNull())
            add(inventory.equipment.get(EquipmentSlot.HEAD).orNull())
        }.normalizeAsArray()
    }
    //#endif

    fun EndermanEntity.getBlockInHand(): BlockState? = carriedBlock

    @AllEntitiesGetter
    inline fun <reified R : Entity> getEntities(): Sequence<R> = getAllEntities().filterIsInstance<R>()

    inline fun <reified E : Entity> getEntitiesInBox(pos: LorenzVec, radius: Double, noinline predicate: (E) -> Boolean = ALWAYS): List<E> {
        return getEntitiesInBoundingBox(pos.boundingCenter(radius), predicate)
    }

    // More efficient than filtering by type, and then for distance, as Minecraft already first filters the chunks that contain the aabb,
    // and then filters both for entity type and with the predicate for entities inside those chunks.
    inline fun <reified E : Entity> getEntitiesInBoundingBox(aabb: Box, noinline predicate: (E) -> Boolean = ALWAYS): List<E> {
        val world = MinecraftCompat.localWorldOrNull ?: return emptyList()
        //#if MC < 1.21
        //$$ return world.getEntitiesWithinAABB(E::class.java, aabb) { it != null && predicate(it) }
        //#else
        return world.getEntitiesByClass<E>(E::class.java, aabb, predicate)
        //#endif
    }

    private fun ClientWorld.getAllEntities(): Iterable<Entity> =
        //#if MC < 1.14
        //$$ loadedEntityList
    //#else
    entities
    //#endif

    @AllEntitiesGetter
    fun getAllEntities(): Sequence<Entity> = MinecraftCompat.localWorldOrNull?.getAllEntities()?.let {
        if (MinecraftClient.getInstance().isOnThread) it
        // TODO: while i am here, i want to point out that copying the entity list does not constitute proper synchronization,
        //  but *does* make crashes because of it rarer.
        else it.toMutableList()
    }?.asSequence().orEmpty()

    //#if MC < 1.21
    //$$ fun getAllTileEntities(): Sequence<BlockEntity> = MinecraftCompat.localWorldOrNull?.loadedTileEntityList?.let {
    //$$     if (MinecraftClient.getInstance().isOnThread) it else it.toMutableList()
    //$$ }?.asSequence()?.filterNotNull().orEmpty()
    //#else
    fun getAllTileEntities(): Sequence<BlockEntity> {
        val world = MinecraftCompat.localWorldOrNull ?: return emptySequence()
        val blockEntityTickers = world.blockEntityTickers.let {
            if (MinecraftClient.getInstance().isOnThread) it else it.toMutableList()
        }.asSequence().filterNotNull()

        return blockEntityTickers.mapNotNull { invoker -> invoker.pos?.let { world.getBlockEntity(it) } }
    }
    //#endif

    fun Entity.canBeSeen(viewDistance: Number = 150.0, vecYOffset: Double = 0.5, ignoreFrustum: Boolean = false): Boolean {
        if (deceased) return false
        // TODO add cache that only updates e.g. 10 times a second
        if (!ignoreFrustum && !FrustumUtils.isVisible(boundingBox)) return false
        return getLorenzVec().up(vecYOffset).canBeSeen(viewDistance)
    }

    fun getEntityByID(entityId: Int): Entity? = MinecraftCompat.localPlayerOrNull?.getEntityLevel()?.getEntityById(entityId)

    fun LivingEntity.isCorrupted() = baseMaxHealth == findHealthReal().toInt().derpy() * 3 || isRunicAndCorrupt()
    fun LivingEntity.isRunic() = baseMaxHealth == findHealthReal().toInt().derpy() * 4 || isRunicAndCorrupt()
    fun LivingEntity.isRunicAndCorrupt() = baseMaxHealth == findHealthReal().toInt().derpy() * 3 * 4

    fun Entity.cleanName() = this.name.formattedTextCompatLessResets().removeColor()

    // TODO use derpy() on every use case
    val LivingEntity.baseMaxHealth: Int
        //#if MC < 1.21
        //$$ get() = this.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue.toInt()
    //#else
    get() = this.getAttributeBaseValue(EntityAttributes.MAX_HEALTH).toInt()
    //#endif
}
