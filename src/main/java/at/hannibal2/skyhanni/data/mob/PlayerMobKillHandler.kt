package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityDeathEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PlayerMobKillHandler {
    private val mobsDamagedByLocalPlayer = TimeLimitedSet<Mob>(30.seconds)

    fun reset() {
        mobsDamagedByLocalPlayer.clear()
    }

    // If another player hits a mob, this this event doesn't get triggered
    @HandleEvent
    fun onMobHurt(event: MobEvent.Hurt) {
        if (event.source.entity is EntityOtherPlayerMP) {
            mobsDamagedByLocalPlayer.remove(event.mob)
        }

        if (event.source.entity is EntityPlayerSP) {
            if (event.source.entity.isLocalPlayer) {
                mobsDamagedByLocalPlayer.add(event.mob)
            } else {
                mobsDamagedByLocalPlayer.remove(event.mob)
            }
        }
    }

    @HandleEvent
    fun onEntityDeath(event: EntityDeathEvent<Entity>) {
        val mob = event.entity.mob ?: return
        if (mob in mobsDamagedByLocalPlayer) {
            mobsDamagedByLocalPlayer.remove(mob)
            postMobKilledByLocalPlayerEvent(mob)
        }
    }

    fun postMobKilledByLocalPlayerEvent(mob: Mob) = when (mob.mobType) {
        Mob.Type.PLAYER -> MobEvent.KilledByLocalPlayer.Player(mob)
        Mob.Type.SUMMON -> MobEvent.KilledByLocalPlayer.Summon(mob)
        Mob.Type.SPECIAL -> MobEvent.KilledByLocalPlayer.Special(mob)
        Mob.Type.PROJECTILE -> MobEvent.KilledByLocalPlayer.Projectile(mob)
        Mob.Type.DISPLAY_NPC -> MobEvent.KilledByLocalPlayer.DisplayNpc(mob)
        Mob.Type.BASIC, Mob.Type.DUNGEON, Mob.Type.BOSS, Mob.Type.SLAYER -> MobEvent.KilledByLocalPlayer.SkyblockMob(mob)
    }.post()

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        mobsDamagedByLocalPlayer.clear()
    }
}
