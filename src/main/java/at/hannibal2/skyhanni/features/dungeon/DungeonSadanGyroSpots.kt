package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.events.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class DungeonSadanGyroSpots {
    var active = false
    var bossEnterTime = SimpleTimeMark.farPast()
    var sadanSpawnTime = SimpleTimeMark.farPast()
    var firstTerraDied = false
    var firstTerraDiedTime = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (event.repeatSeconds(1)) {
            val inSadan = DungeonAPI.dungeonFloor?.equalsOneOf("F6", "M6") ?: false
            active = LorenzUtils.inDungeons && DungeonAPI.inBossRoom && inSadan
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (active) {
            if (event.message == "§c[BOSS] Sadan§r§f: You did it. I understand now, you have earned my respect.") {
                sadanSpawnTime = SimpleTimeMark.now() + 32.5.seconds
            }
        }
    }

    @SubscribeEvent
    fun onDungeonBossRoomEnter(event: DungeonBossRoomEnterEvent) {
        bossEnterTime = SimpleTimeMark.now()
        sadanSpawnTime = SimpleTimeMark.farPast()
        firstTerraDied = false
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        val name = event.entity.name
        if (active) {
            if (name == "Terracotta ") {
                if (!firstTerraDied) {
                    LorenzUtils.debug("first terra died!")
                    firstTerraDied = true
                }
            }
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        val entity = event.entity
        val name = entity.name
//        println("EntityHealthUpdateEvent: '$name' (${event.health})")
//        if (event.health <= 0) {
//            println("death: '$name'")
//
//            if (name.lowercase().contains("terra")) {
//
//                if (!firstTerraDied) {
//                    LorenzUtils.debug("first terra died!")
//                    firstTerraDied = true
//                }
//            }
//        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!active) return

//        val enteredDuration = bossEnterTime.passedSince()
//        val timeFormat = "§b" + enteredDuration.format(showMilliSeconds = true)

        if (time(bossEnterTime + 12.8.seconds, "#1 Mage Gyro", LorenzVec(-12, 68, 39), event)) {
//            return
        }
        if (time(bossEnterTime + (12.8 + 5).seconds, "#2 Archer Gyro", LorenzVec(-9, 68, 54), event)) {
//            return
        }
        if (firstTerraDied) {

//            if (time(bossEnterTime + (12.8 + 5 + 10).seconds, "#3 Mage Gyro", LorenzVec(-12, 68, 37), event)) {
            if (time(firstTerraDiedTime + 10.seconds, "#3 Mage Gyro", LorenzVec(-12, 68, 37), event)) {
//            return
            }
        }


//        if (time(bossEnterTime + (12.8 + 5 + 10 + 5).seconds, "#4 Bers Gyro", LorenzVec(-9, 68 - 1, 54), event)) {
////            return
//        }


//        if (time(bossEnterTime + (12.8 + 5 + 10 + 5 + 10).seconds, "#5 Mage Gyro", LorenzVec(-12, 68 - 1, 37), event)) {
////            return
//        }


        if (!sadanSpawnTime.isFarPast()) {
//        time(bossEnterTime + (12.8 + 5 + 10 + 5 + 10 + 20).seconds, "Sadan Gyro", LorenzVec(-9.0, 68.2, 82.0), event)
            time(sadanSpawnTime, "Sadan Gyro", LorenzVec(-9.0, 68.2, 82.0), event)
//        time(bossEnterTime + (12.8 + 5 + 10 + 5 + 10 + 20).seconds, "Sadan Gyro", LorenzVec(-9, 68, 82), event)
        }
    }

    private fun time(
        firstTime: SimpleTimeMark,
        name: String,
        first: LorenzVec,
        event: RenderWorldLastEvent,
    ) = if (!firstTime.isInPast()) {
        val duration = firstTime.timeUntil()
        val timeFormat = duration.format(showMilliSeconds = true)
        event.drawWaypointFilled(first, LorenzColor.WHITE.toColor())
        event.drawDynamicText(first, "$name $timeFormat", 1.5)
        true
    } else {
        false
    }
}
