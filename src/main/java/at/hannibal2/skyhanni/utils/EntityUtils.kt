package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getEquipmentSlots
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getHandItem
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getHelmet
import at.hannibal2.skyhanni.utils.compat.EntityCompat.realHealth
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.normalizeAsArray
import at.hannibal2.skyhanni.utils.render.FrustumUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB

@RequiresOptIn(
    "getAllEntities or getEntities should only be used when necessary," +
        "as they can be expensive, since they iterate through all entities in world.",
)
annotation class AllEntitiesGetter

@SkyHanniModule
object EntityUtils {
    inline val ALWAYS get(): (Entity) -> Boolean = { true }

    // TODO remove this relatively heavy call everywhere
    @Deprecated("Use Mob Detection instead")
    @Suppress("DEPRECATION")
    fun LivingEntity.hasNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): Boolean = getNameTagWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity) != null

    fun getPlayerEntities(): List<RemotePlayer> =
        MinecraftCompat.localWorldOrNull
            ?.players()
            ?.filterIsInstance<RemotePlayer>()
            ?.filterNot { it.isNpc() }
            .orEmpty()

    @Deprecated("Use Mob Detection instead")
    fun LivingEntity.getAllNameTagsInRadiusWith(
        contains: String,
        radius: Double = 3.0,
    ): List<ArmorStand> =
        getArmorStandsInRadius(getLorenzVec().up(3), radius).filter { contains in it.cleanName }

    @Deprecated("Use Mob Detection instead")
    @Suppress("DEPRECATION")
    fun LivingEntity.getNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): ArmorStand? = getAllNameTagsWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity).firstOrNull()

    @Deprecated("Use Mob Detection instead")
    fun LivingEntity.getAllNameTagsWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): List<ArmorStand> {
        val center = getLorenzVec().up(y)
        return getArmorStandsInRadius(center, inaccuracy).filter {
            val name = it.name.formattedTextCompatLessResets()
            val result = contains in name
            if (debugWrongEntity && !result) {
                ChatUtils.consoleLog("wrong entity in aabb: '$name'")
            }
            if (debugRightEntity && result) {
                ChatUtils.consoleLog("mob: ${center.printWithAccuracy(2)}")
                ChatUtils.consoleLog("nametag: ${it.getLorenzVec().printWithAccuracy(2)}")
                ChatUtils.consoleLog("accuracy: ${(it.getLorenzVec() - center).printWithAccuracy(3)}")
            }
            result
        }
    }

    private fun getArmorStandsInRadius(center: LorenzVec, radius: Double): List<ArmorStand> {
        val a = center.add(-radius, -radius - 3, -radius)
        val b = center.add(radius, radius + 3, radius)
        val alignedBB = a.axisAlignedTo(b)
        return getEntitiesInBoundingBox<ArmorStand>(alignedBB)
    }

    @Deprecated("Use entity detection feature instead")
    @Suppress("DEPRECATION")
    fun LivingEntity.hasBossHealth(health: Int): Boolean = this.hasMaxHealth(health, true)

    @Deprecated("Use entity detection feature instead")
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

    fun Player.getSkinTexture(): String? = gameProfile.properties.get("textures").firstOrNull()?.value

    inline fun <reified T : Entity> getEntitiesNearby(
        radius: Double,
        noinline predicate: (T) -> Boolean = ALWAYS,
    ): List<T> = LocationUtils.playerLocation().getEntitiesNearby<T>(radius, predicate)

    // First filters for a bounding box because it's faster, and then filters based on distance
    inline fun <reified T : Entity> LorenzVec.getEntitiesNearby(
        radius: Double,
        noinline predicate: (T) -> Boolean = ALWAYS,
    ): List<T> = getEntitiesInBox<T>(this, radius) { it.distanceTo(this) < radius && predicate(it) }

    fun LivingEntity.isAtFullHealth() = baseMaxHealth == realHealth.toInt()

    @Deprecated("Use specific methods instead, such as wearingSkullTexture or holdingSkullTexture")
    fun LivingEntity.hasSkullTexture(skin: String?): Boolean =
        skin != null && getEquipmentSlots().values.any { it?.getSkullTexture() == skin }

    fun LivingEntity.getWornSkullTexture(): String? = getHelmet()?.getSkullTexture()
    fun LivingEntity.wearingSkullTexture(skin: String?) = skin != null && getWornSkullTexture() == skin
    fun LivingEntity.holdingSkullTexture(skin: String?) = skin != null && getHandItem()?.getSkullTexture() == skin

    fun Player.isNpc() = !isRealPlayer()

    fun LivingEntity.getArmorInventory(): Array<SafeItemStack?>? {
        if (this !is Player) return null
        return buildList {
            add(inventory.equipment.get(EquipmentSlot.FEET).orNull())
            add(inventory.equipment.get(EquipmentSlot.LEGS).orNull())
            add(inventory.equipment.get(EquipmentSlot.CHEST).orNull())
            add(inventory.equipment.get(EquipmentSlot.HEAD).orNull())
        }.normalizeAsArray()
    }

    fun EnderMan.getBlockInHand(): BlockState? = carriedBlock

    @OptIn(AllEntitiesGetter::class)
    inline fun <reified R : Entity> getEntities(): Sequence<R> = getAllEntities().filterIsInstance<R>()

    inline fun <reified E : Entity> getEntitiesInBox(
        pos: LorenzVec,
        radius: Double,
        noinline predicate: (E) -> Boolean = ALWAYS,
    ): List<E> = getEntitiesInBoundingBox(pos.boundingCenter(radius), predicate)

    /**
     * More efficient than filtering by type, and then for distance, as Minecraft already first
     * filters the chunks that contain the [aabb], and then filters both for entity type and with
     * the predicate for entities inside those chunks.
     */
    inline fun <reified E : Entity> getEntitiesInBoundingBox(
        aabb: AABB,
        noinline predicate: (E) -> Boolean = ALWAYS,
    ): List<E> = MinecraftCompat.localWorldOrNull?.getEntitiesOfClass(
        E::class.java, aabb, predicate,
    ).orEmpty()

    @OptIn(AllEntitiesGetter::class)
    fun getAllEntities(): Sequence<Entity> = MinecraftCompat.localWorldOrNull?.entitiesForRendering()?.let {
        if (Minecraft.getInstance().isSameThread) it
        // TODO: while I am here, I want to point out that copying the entity list does not
        //  constitute proper synchronization, but *does* make crashes because of it rarer.
        else it.toMutableList()
    }?.asSequence().orEmpty()

    fun getAllTileEntities(): Sequence<BlockEntity> {
        val world = MinecraftCompat.localWorldOrNull ?: return emptySequence()
        val blockEntityTickers = world.blockEntityTickers.let {
            if (Minecraft.getInstance().isSameThread) it else it.toMutableList()
        }.asSequence()

        return blockEntityTickers.mapNotNull { invoker ->
            // This can be null due to other mods
            @Suppress("UNNECESSARY_SAFE_CALL")
            invoker.pos?.let(world::getBlockEntity)
        }
    }

    fun Entity.canBeSeen(viewDistance: Number = 150.0, vecYOffset: Double = 0.5, ignoreFrustum: Boolean = false): Boolean {
        if (isRemoved) return false
        // TODO add cache that only updates e.g. 10 times a second
        if (!ignoreFrustum && !FrustumUtils.isVisible(boundingBox)) return false
        return getLorenzVec().up(vecYOffset).canBeSeen(viewDistance)
    }

    fun getEntityByID(entityId: Int): Entity? = MinecraftCompat.localWorldOrNull?.getEntity(entityId)

    fun LivingEntity.isCorrupted() = baseMaxHealth == realHealth.toInt().derpy() * 3 || isRunicAndCorrupt()
    fun LivingEntity.isRunic() = baseMaxHealth == realHealth.toInt().derpy() * 4 || isRunicAndCorrupt()
    fun LivingEntity.isRunicAndCorrupt() = baseMaxHealth == realHealth.toInt().derpy() * 3 * 4

    val Entity.cleanName: String get() = name.string.removeColor()

    // TODO use derpy() on every use case
    val LivingEntity.baseMaxHealth: Int get() = getAttributeBaseValue(Attributes.MAX_HEALTH).toInt()

    inline val Entity.spawnTime: ServerTimeMark get() = ServerTimeMark.now() - tickCount.ticks

    fun LivingEntity.hasVisibleEquipment(): Boolean = EquipmentSlot.entries.any { getItemBySlot(it).isNotEmpty() }

    fun Entity.isEmptyInvisibleArmorStand(): Boolean = this is ArmorStand && isInvisible && !hasVisibleEquipment()
}
