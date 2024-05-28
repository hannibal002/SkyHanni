package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.EntityMoveEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWarpEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EntityMovementData {

    private val warpingPattern by RepoPattern.pattern(
        "data.entity.warping",
        "§7(?:Warping|Warping you to your SkyBlock island|Warping using transfer token|Finding player|Sending a visit request)\\.\\.\\."
    )

    private val entityLocation = mutableMapOf<Entity, LorenzVec>()

    fun addToTrack(entity: Entity) {
        if (entity !in entityLocation) {
            entityLocation[entity] = entity.getLorenzVec()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        addToTrack(Minecraft.getMinecraft().thePlayer)

        for (entity in entityLocation.keys) {
            if (entity.isDead) continue

            val newLocation = entity.getLorenzVec()
            val oldLocation = entityLocation[entity]!!
            val distance = newLocation.distance(oldLocation)
            if (distance > 0.01) {
                entityLocation[entity] = newLocation
                EntityMoveEvent(entity, oldLocation, newLocation, distance).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!warpingPattern.matches(event.message)) return
        DelayedRun.runNextTick {
            LorenzWarpEvent().postAndCatch()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        entityLocation.clear()
    }
}
