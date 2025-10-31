package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.mob.Mob
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.events.entity.slayer.SlayerDeathEvent
import at.hannibal2.hanni.features.slayer.SlayerType
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager

@HanniModule
object OtherPlayersSlayerApi {

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val mob = event.mob

        // no death, rather despawn because too far away
        if (mob.baseEntity.health != 0f) return

        if (mob.mobType != Mob.Type.SLAYER) return

        val owner = mob.owner?.ownerName
        val tier = mob.levelOrTier
        val name = mob.name
        val slayerType = SlayerType.getByName(name) ?: run {
            ErrorManager.logErrorStateWithData(
                "Unknown slayer type found", "unknown slayer",
                "name" to name,
            )
            return
        }

        SlayerDeathEvent(slayerType, tier, owner).post()
    }
}
