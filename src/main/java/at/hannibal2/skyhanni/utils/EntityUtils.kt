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
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.keepOnlyIn
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
//#if MC > 1.21
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.EquipmentSlot
//#else
//$$ import net.minecraft.entity.SharedMonsterAttributes
//#endif

@SkyHanniModule
object EntityUtils {

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
        val clazz = ArmorStandEntity::class.java
        val world = MinecraftCompat.localWorldOrNull ?: return emptyList()
        //#if MC < 1.21
        //$$ return world.getEntitiesWithinAABB(clazz, alignedBB)
        //#else
        return world.getEntitiesByClass(clazz, alignedBB, net.minecraft.predicate.entity.EntityPredicates.EXCEPT_SPECTATOR)
        //#endif
    }

    @Deprecated("Old. Instead use entity detection feature instead.")
    fun LivingEntity.hasBossHealth(health: Int): Boolean = this.hasMaxHealth(health, true)

    @Deprecated("Old. Instead use entity detection feature instead.")
    fun LivingEntity.hasMaxHealth(health: Int, boss: Boolean = false, maxHealth: Int = baseMaxHealth): Boolean {
        val derpyMultiplier = if (ElectionApi.isDerpy) 2 else 1
        if (maxHealth == health * derpyMultiplier) return true

        if (!boss && !DungeonApi.inDungeon()) {
            // Corrupted
            if (maxHealth == health * 3 * derpyMultiplier) return true
            // Runic
            if (maxHealth == health * 4 * derpyMultiplier) return true
            // Corrupted+Runic
            if (maxHealth == health * 12 * derpyMultiplier) return true
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

    inline fun <reified T : Entity> getEntitiesNextToPlayer(radius: Double): Sequence<T> =
        getEntitiesNearby<T>(LocationUtils.playerLocation(), radius)

    inline fun <reified T : Entity> getEntitiesNearby(location: LorenzVec, radius: Double): Sequence<T> =
        getEntities<T>().filter { it.distanceTo(location) < radius }

    inline fun <reified T : Entity> getEntitiesNearbyIgnoreY(location: LorenzVec, radius: Double): Sequence<T> =
        getEntities<T>().filter { it.distanceToIgnoreY(location) < radius }

    fun LivingEntity.isAtFullHealth() = baseMaxHealth == findHealthReal().toInt()

    @Deprecated("Use specific methods instead, such as wearingSkullTexture or holdingSkullTexture")
    fun ArmorStandEntity.hasSkullTexture(skin: String): Boolean {
        val inventory = this.getAllEquipment() ?: return false
        return inventory.any { it != null && it.getSkullTexture() == skin }
    }

    fun ArmorStandEntity.wearingSkullTexture(skin: String) = getStandHelmet()?.getSkullTexture() == skin
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

    inline fun <reified R : Entity> getEntities(): Sequence<R> = getAllEntities().filterIsInstance<R>()

    private fun ClientWorld.getAllEntities(): Iterable<Entity> =
        //#if MC < 1.14
        //$$ loadedEntityList
    //#else
    entities
    //#endif

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

    inline fun <reified T : Entity> removeInvalidEntities(list: MutableList<T>) {
        list.keepOnlyIn(getEntities<T>())
    }

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
