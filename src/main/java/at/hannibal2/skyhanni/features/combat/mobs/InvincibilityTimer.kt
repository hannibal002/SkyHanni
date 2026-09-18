package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.VanquisherApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.combat.InvincibilityTimerConfig.MobType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.fishing.LivingSeaCreatureData
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.getBoxCenter
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object InvincibilityTimer {

    private val config get() = SkyHanniMod.feature.combat.mobs.invincibilityTimer

    private val seaCreatures get() = SeaCreatureDetectionApi.getSeaCreatures()
    private val vanquishers get() = VanquisherApi.getVanquishers()

    private val INVINCIBILITY = 5.seconds

    private data class InvincibilityMob(
        val spawnTime: ServerTimeMark,
        val pos: LorenzVec,
        val isOwn: Boolean,
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        if (config.mobTypes.contains(MobType.SEA_CREATURE)) {
            for (seaCreature in seaCreatures) {
                val mob = seaCreature.toInvincibilityMob() ?: continue
                renderTimer(event, mob)
            }
        }

        if (config.mobTypes.contains(MobType.VANQUISHER)) {
            for (vanquisher in vanquishers) {
                val mob = vanquisher.toInvincibilityMob() ?: continue
                renderTimer(event, mob)
            }
        }
    }

    private fun renderTimer(
        event: SkyHanniRenderWorldEvent,
        mob: InvincibilityMob,
    ) {
        val invincibilityEnd = mob.spawnTime + INVINCIBILITY
        if (invincibilityEnd.passedSince() > 1.seconds) return
        val timeLeft = invincibilityEnd.timeUntil()

        event.drawDynamicText(
            mob.pos,
            "§b${timeLeft.format(showMilliSeconds = true)}",
            scaleMultiplier = 1.3,
        )

        if (mob.isOwn) {
            event.drawDynamicText(
                mob.pos.up(0.5),
                "§aOWN MOB",
                scaleMultiplier = 1.3,
            )
        }
    }

    private fun LivingSeaCreatureData.toInvincibilityMob(): InvincibilityMob? {
        if (!exists()) return null
        if (!canBeSeen()) return null
        if (!rarity.isAtLeast(LorenzRarity.LEGENDARY)) return null
        val aabb = this.aabb ?: return null

        return InvincibilityMob(
            spawnTime = spawnTime,
            pos = aabb.getBoxCenter(),
            isOwn = isOwn,
        )
    }

    private fun VanquisherApi.VanquisherData.toInvincibilityMob(): InvincibilityMob? {
        if (!mob.baseEntity.canBeSeen()) return null

        return InvincibilityMob(
            spawnTime = spawnTime,
            pos = mob.centerCords,
            isOwn = isOwn,
        )
    }

    fun isEnabled() = config.enabled && config.mobTypes.isNotEmpty()
}
