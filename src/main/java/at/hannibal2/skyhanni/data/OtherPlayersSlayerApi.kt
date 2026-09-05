package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobCategory
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.combat.OtherPlayersSlayerEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager

@SkyHanniModule
object OtherPlayersSlayerApi {

    private val spawnedMobs = mutableSetOf<Int>()

    @HandleEvent
    fun onWorldChange() {
        spawnedMobs.clear()
    }

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (spawnedMobs.contains(event.mob.id)) return

        detectAndPost(event.mob, OtherPlayersSlayerEvent::Spawn)
        spawnedMobs.add(event.mob.id)
    }

    @HandleEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob.health == 0f) {
            detectAndPost(event.mob, OtherPlayersSlayerEvent::Death)
            spawnedMobs.remove(event.mob.id)
        }
    }

    private fun detectAndPost(mob: Mob, eventType: (SlayerType, Int, String) -> OtherPlayersSlayerEvent) {
        if (mob.category != MobCategory.SLAYER) return

        val slayerType = SlayerType.getByName(mob.name) ?: run {
            ErrorManager.logErrorStateWithData(
                "Unknown slayer type found", "unknown slayer",
                "name" to mob.name,
            )
            return
        }
        val tier = mob.levelOrTier
        val owner = mob.ownerNameOrEmpty

        eventType(slayerType, tier, owner).post()
    }
}
