package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.combat.mobs.AreaMiniBossFeatures
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.dungeon.DungeonMobManager
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntityMagmaCube

@SkyHanniModule
object HideFarEntities {
    private val config get() = SkyHanniMod.feature.misc.hideFarEntities

    private var ignored = emptySet<Int>()
    private var neverHide = emptySet<Int>()

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(20)) {
            updateNeverHide()
        }

        val maxAmount = config.maxAmount.coerceAtLeast(1)
        val minDistance = config.minDistance.coerceAtLeast(3)

        ignored = EntityUtils.getAllEntities()
            .map { it.entityId to it.distanceToPlayer() }
            .filter { it.second > minDistance && it.first !in neverHide }
            .sortedBy { it.second }.drop(maxAmount)
            .map { it.first }.toSet()
    }

    /**
     * TODO mobs to add to never hide list
     * golden/diamond golbins (mining islands)
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
    private fun updateNeverHide() {
        val list = mutableSetOf<Entity>()
        val allEntities = EntityUtils.getAllEntities()

        if (DungeonApi.inDungeon()) {
            list += allEntities.filter { it.mob?.name == "Mort" }
            list += allEntities.filter { it is EntityWither || it is EntityDragon }
            list += DungeonMobManager.starredVisibleMobs.map { it.baseEntity }
            // other party members
            list += allEntities.filter { it is EntityOtherPlayerMP && !it.isNpc() }
        }
        if (KuudraApi.inKuudra) {
            list += allEntities.filter { it.mob?.name == "Elle" }
            // other party members
            list += allEntities.filter { it is EntityOtherPlayerMP && !it.isNpc() }
        }
        if (IslandType.WINTER.isCurrent()) {
            list += allEntities.filter { it is EntityMagmaCube }
        }
        if (IslandType.DWARVEN_MINES.isCurrent()) {
            // powder ghast & golem defender (from goblin raid event)
            list += allEntities.filter { it is EntityGhast || it is EntityIronGolem }
        }

        // Always show boss bar
        list += allEntities.filter { it is EntityWither && it.entityId < 0 }

        list += allEntities.filter { it is EntityOtherPlayerMP && it.name in PartyApi.partyMembers }
        list += DamageIndicatorManager.getAllMobs()
        list += AreaMiniBossFeatures.currentMobs.map { it.baseEntity }

        neverHide = list.map { it.entityId }.toSet()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entity.entityId in ignored) {
            event.cancel()
        }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
