package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.skyhanni.data.mob.MobFilter.isDisplayNpc
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityOpacityActiveEvent
import at.hannibal2.skyhanni.events.entity.EntityOpacityEvent
import at.hannibal2.skyhanni.features.misc.CarryTracker
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity

@SkyHanniModule
object ActiveBossTransparency {

    private val config get() = SlayerApi.config.activeBossTransparency

    private var lastClickedMob: Mob? = null
    private var lastHitCarrierBoss = false

    @HandleEvent
    fun onEntityOpacityActive(event: EntityOpacityActiveEvent) {
        event.setActive(isActive())
    }

    @HandleEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (lastClickedMob == event.mob) {
            lastClickedMob = null
            lastHitCarrierBoss = false
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onClickEntity(event: EntityClickEvent) {
        if (event.action != C02PacketUseEntity.Action.ATTACK) return
        val mob = event.clickedEntity.mob ?: return

        lastClickedMob = mob
        lastHitCarrierBoss = CarryTracker.isCustomer(mob.ownerNameOrEmpty)
    }

    @HandleEvent
    fun onEntityOpacity(event: EntityOpacityEvent<EntityLivingBase>) {
        if (!isActive()) return
        val entity = event.entity

        // always show yourself
        if (entity.isLocalPlayer) return

        // always show npcs, they are static
        if (entity.isDisplayNpc()) return

        entity.mob?.let { mob ->

            // always show last clicked mob
            if (mob == lastClickedMob) return

            val type = mob.mobType
            if (type == Mob.Type.SLAYER) {
                // hide own slayer boss
                if (mob.belongsToPlayer()) return

                // hide carry boss
                if (CarryTracker.isCustomer(mob.ownerNameOrEmpty)) return

            }

            // maybe also hide other players
            if (type == Mob.Type.PLAYER) {
                // always show current slayer carry customers
                if (CarryTracker.isCustomer(mob.name)) return

                if (!config.applyToPlayers) return
            }
            if (type == Mob.Type.PLAYER && !config.applyToPlayers) return
        }

        event.opacity = config.transparencyLevel.coerceIn(15, 70)
    }

    private fun isActive() = config.enabled && (SlayerApi.isInBossFight() || lastHitCarrierBoss)
}
