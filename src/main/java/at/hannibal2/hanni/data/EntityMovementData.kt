package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.mob.Mob
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.events.HanniWarpEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.entity.EntityMoveEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@HanniModule
object EntityMovementData {

    /**
     * REGEX-TEST: §7Sending a visit request...
     * REGEX-TEST: §7Finding player...
     * REGEX-TEST: §7Warping you to your SkyBlock island...
     */
    private val warpingPattern by RepoPattern.pattern(
        "data.entity.warping",
        "§7(?:Warping|Warping you to your SkyBlock island|Warping using transfer token|Finding player|Sending a visit request)\\.\\.\\.",
    )

    private var nextTeleport: OnNextTeleport? = null

    fun onNextTeleport(island: IslandType, action: () -> Unit) {
        nextTeleport = OnNextTeleport(island, action)
    }

    class OnNextTeleport(val island: IslandType, val action: () -> Unit) {
        val startTime: SimpleTimeMark = SimpleTimeMark.now()
    }

    private val entityLocation = mutableMapOf<EntityLivingBase, LorenzVec>()

    fun addToTrack(entity: EntityLivingBase) {
        if (entity !in entityLocation) {
            entityLocation[entity] = entity.getLorenzVec()
        }
    }

    fun addToTrack(mob: Mob) {
        addToTrack(mob.baseEntity)
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        val nextData = nextTeleport ?: return
        if (nextData.island != event.newIsland) return
        val passedSince = nextData.startTime.passedSince()
        if (passedSince > 5.seconds) {
            nextTeleport = null
            return
        }

        DelayedRun.runDelayed(100.milliseconds) {
            nextData.action()
        }
        nextTeleport = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerMove(event: EntityMoveEvent<EntityPlayerSP>) {
        if (!event.isLocalPlayer) return

        val nextData = nextTeleport ?: return

        val passedSince = nextData.startTime.passedSince()
        if (passedSince > 5.seconds) {
            nextTeleport = null
            return
        }
        if (passedSince > 50.milliseconds && nextData.island.isCurrent()) {
            nextData.action()
            nextTeleport = null
            return
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        addToTrack(MinecraftCompat.localPlayer)

        for (entity in entityLocation.keys) {
            if (entity.isDead) continue

            val newLocation = entity.getLorenzVec()
            val oldLocation = entityLocation[entity]!!
            val distance = newLocation.distance(oldLocation)
            if (distance > 0.01) {
                entityLocation[entity] = newLocation
                EntityMoveEvent(entity, oldLocation, newLocation, distance).post()
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: HanniChatEvent) {
        if (!warpingPattern.matches(event.message)) return
        DelayedRun.runNextTick {
            HanniWarpEvent.post()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        entityLocation.clear()
    }
}
