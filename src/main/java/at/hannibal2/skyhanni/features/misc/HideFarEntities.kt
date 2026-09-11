package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.combat.mobs.AreaMiniBossFeatures
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.dungeon.DungeonMobManager
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeFirst
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.monster.cubemob.MagmaCube

@SkyHanniModule
object HideFarEntities {
    private val config get() = SkyHanniMod.feature.misc.hideFarEntities

    private var ignored = mutableMapOf<Int, Double>()
    private var neverHide = mutableSetOf<Int>()

    @HandleEvent
    private fun onEntitySpawn(event: EntityEnterWorldEvent<*>) {
        if (GlobalRender.renderDisabled) return
        if (!isEnabled()) return
        updateNeverHide(event.entity)
    }

    @HandleEvent
    private fun onEntityMoveEvent(event: EntityMoveEvent<*>) {
        if (GlobalRender.renderDisabled) return
        if (!isEnabled()) return
        val (entityID, distance) = event.entity.id to event.entity.distanceToPlayer()
        if (entityID in neverHide) return
        val maxAmount = config.maxAmount.coerceAtLeast(1)
        val minDistance = config.minDistance.coerceAtLeast(3)
        if (distance < minDistance) return
        ignored += entityID to distance
        ignored = ignored.map { it.key to it.value }.sortedBy { it.second }.drop(maxAmount).toMap().toMutableMap()
    }

    /**
     * TODO mobs to add to never hide list
     * Golden/Diamond Goblins (mining islands)
     * beach ball (great and normal, from year of the seal)
     * worms/scatha in dwarven mines
     * dungeon wither+blood key
     *
     * add to damage indicator:
     * jerries (from jerry mayor event)
     * primal fear (great spook event) - add to damage indicator
     * special zealots in the end
     * kuudra boss
     * dungeon mini bosses: sa, frozen adventurer
     * 1b hp mob in dungeon
     */
    private fun updateNeverHide(entity: Entity) {
        val entityID = entity.id

        if (DungeonApi.inDungeon()) {
            when {
                entity.mob?.name == "Mort" -> neverHide += entityID
                entity is WitherBoss || entity is EnderDragon -> neverHide += entityID // M7 Withers/Wither Dragons
                entity is RemotePlayer && !entity.isNpc() -> neverHide += entityID // Party Members
                DungeonMobManager.starredVisibleMobs.contains(entity.mob) -> neverHide += entityID // All Non-Invisible Starred mobs
                DungeonMobManager.staredInvisible.contains(entity.mob) -> neverHide += entityID // Starred Fels
            }
            return
        }
        if (KuudraApi.inKuudra) {
            when {
                entity.mob?.name == "Elle" -> neverHide += entityID
                entity is RemotePlayer && !entity.isNpc() -> neverHide += entityID // Party Members
            }
            return
        }
        if (IslandType.WINTER.isInIsland()) {
            if (entity is MagmaCube) neverHide += entityID // Jerry Island Magma Cubes
            return
        }
        if (IslandType.DWARVEN_MINES.isInIsland()) {
            if (entity is Ghast || entity is IronGolem) neverHide += entityID // powder ghast & golem defender (from goblin raid event)
            return
        }

        if (entity is WitherBoss && entityID < 0) {
            neverHide += entityID
            return
        } // Always show boss bar Mobs

        if (entity is RemotePlayer && entity.name.string in PartyApi.partyMembers) {
            neverHide += entityID
            return
        }
        if (DamageIndicatorManager.getAllMobs().contains(entity)) {
            neverHide += entityID
            return
        }
        if (AreaMiniBossFeatures.currentMobs.map { it.baseEntity }.contains(entity)) {
            neverHide += entityID
            return
        }
    }

    @HandleEvent
    private fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<*>) {
        val iterator = neverHide.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next == event.entity.id) iterator.remove()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entity.id in ignored) {
            event.cancel()
        }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
