package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToIgnoreY
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.getAllEquipment
import at.hannibal2.skyhanni.utils.compat.getEntityLevel
import at.hannibal2.skyhanni.utils.compat.getHandItem
import at.hannibal2.skyhanni.utils.compat.getLoadedPlayers
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.compat.normalizeAsArray
import at.hannibal2.skyhanni.utils.render.FrustumUtils
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity

@SkyHanniModule
object EntityUtils {

    @Deprecated("Use Mob Detection Instead")
    fun EntityLivingBase.hasNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): Boolean = getNameTagWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity) != null

    fun getPlayerEntities(): MutableList<EntityOtherPlayerMP> {
        val list = mutableListOf<EntityOtherPlayerMP>()
        for (entity in MinecraftCompat.localWorldOrNull?.getLoadedPlayers().orEmpty()) {
            if (!entity.isNpc() && entity is EntityOtherPlayerMP) {
                list.add(entity)
            }
        }
        return list
    }

    @Deprecated("Use Mob Detection Instead")
    fun EntityLivingBase.getAllNameTagsInRadiusWith(
        contains: String,
        radius: Double = 3.0,
    ): List<EntityArmorStand> = getArmorStandsInRadius(getLorenzVec().up(3), radius).filter {
        it.name.contains(contains)
    }

    @Deprecated("Use Mob Detection Instead")
    fun EntityLivingBase.getNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): EntityArmorStand? = getAllNameTagsWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity).firstOrNull()

    @Deprecated("Use Mob Detection Instead")
    fun EntityLivingBase.getAllNameTagsWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): List<EntityArmorStand> {
        val center = getLorenzVec().up(y)
        return getArmorStandsInRadius(center, inaccuracy).filter {
            val result = it.name.contains(contains)
            if (debugWrongEntity && !result) {
                ChatUtils.consoleLog("wrong entity in aabb: '" + it.name + "'")
            }
            if (debugRightEntity && result) {
                ChatUtils.consoleLog("mob: " + center.printWithAccuracy(2))
                ChatUtils.consoleLog("nametag: " + it.getLorenzVec().printWithAccuracy(2))
                ChatUtils.consoleLog("accuracy: " + (it.getLorenzVec() - center).printWithAccuracy(3))
            }
            result
        }
    }

    private fun getArmorStandsInRadius(center: LorenzVec, radius: Double): List<EntityArmorStand> {
        val a = center.add(-radius, -radius - 3, -radius)
        val b = center.add(radius, radius + 3, radius)
        val alignedBB = a.axisAlignedTo(b)
        val clazz = EntityArmorStand::class.java
        val world = MinecraftCompat.localWorldOrNull ?: return emptyList()
        //#if MC < 1.21
        return world.getEntitiesWithinAABB(clazz, alignedBB)
        //#else
        //$$ return world.getEntitiesByClass(clazz, alignedBB, net.minecraft.predicate.entity.EntityPredicates.EXCEPT_SPECTATOR)
        //#endif
    }

    @Deprecated("Old. Instead use entity detection feature instead.")
    fun EntityLivingBase.hasBossHealth(health: Int): Boolean = this.hasMaxHealth(health, true)

    @Deprecated("Old. Instead use entity detection feature instead.")
    fun EntityLivingBase.hasMaxHealth(health: Int, boss: Boolean = false, maxHealth: Int = baseMaxHealth): Boolean {
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

    fun EntityPlayer.getSkinTexture(): String? {
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

    fun EntityLivingBase.isAtFullHealth() = baseMaxHealth == health.toInt()

    @Deprecated("Use specific methods instead, such as wearingSkullTexture or holdingSkullTexture")
    fun EntityArmorStand.hasSkullTexture(skin: String): Boolean {
        val inventory = this.getAllEquipment() ?: return false
        return inventory.any { it != null && it.getSkullTexture() == skin }
    }

    fun EntityArmorStand.wearingSkullTexture(skin: String) = getStandHelmet()?.getSkullTexture() == skin
    fun EntityArmorStand.holdingSkullTexture(skin: String) = getHandItem()?.getSkullTexture() == skin

    fun EntityPlayer.isNpc() = !isRealPlayer()

    fun EntityLivingBase.getArmorInventory(): Array<ItemStack?>? =
        if (this is EntityPlayer) inventory.armorInventory.normalizeAsArray() else null

    fun EntityEnderman.getBlockInHand(): IBlockState? = heldBlockState

    inline fun <reified R : Entity> getEntities(): Sequence<R> = getAllEntities().filterIsInstance<R>()

    private fun WorldClient.getAllEntities(): Iterable<Entity> =
        //#if MC < 1.14
        loadedEntityList
    //#else
    //$$ entitiesForRendering()
    //#endif

    fun getAllEntities(): Sequence<Entity> = MinecraftCompat.localWorldOrNull?.getAllEntities()?.let {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread) it
        // TODO: while i am here, i want to point out that copying the entity list does not constitute proper synchronization,
        //  but *does* make crashes because of it rarer.
        else it.toMutableList()
    }?.asSequence().orEmpty()

    fun getAllTileEntities(): Sequence<TileEntity> = MinecraftCompat.localWorldOrNull?.loadedTileEntityList?.let {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread) it else it.toMutableList()
    }?.asSequence()?.filterNotNull().orEmpty()

    fun Entity.canBeSeen(viewDistance: Number = 150.0): Boolean {
        if (isDead) return false
        // TODO add cache that only updates e.g. 10 times a second
        if (!FrustumUtils.isVisible(entityBoundingBox)) return false
        return getLorenzVec().up(0.5).canBeSeen(viewDistance)
    }

    fun getEntityByID(entityId: Int) = MinecraftCompat.localPlayerOrNull?.getEntityLevel()?.getEntityByID(entityId)

    fun EntityLivingBase.isCorrupted() = baseMaxHealth == health.toInt().derpy() * 3 || isRunicAndCorrupt()
    fun EntityLivingBase.isRunic() = baseMaxHealth == health.toInt().derpy() * 4 || isRunicAndCorrupt()
    fun EntityLivingBase.isRunicAndCorrupt() = baseMaxHealth == health.toInt().derpy() * 3 * 4

    fun Entity.cleanName() = this.name.removeColor()

    // TODO use derpy() on every use case
    val EntityLivingBase.baseMaxHealth: Int
        //#if MC < 1.21
        get() = this.getEntityAttribute(SharedMonsterAttributes.maxHealth).baseValue.toInt()
    //#else
    //$$ get() = this.getAttributeValue(EntityAttributes.MAX_HEALTH).toInt()
    //#endif
}
