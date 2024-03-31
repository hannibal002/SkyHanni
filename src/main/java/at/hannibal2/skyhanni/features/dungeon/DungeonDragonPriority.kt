package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class DungeonDragonPriority {

    private val config get() = SkyHanniMod.feature.dungeon.dragPrio

    private val startPattern by RepoPattern.pattern(
        "dungeons.startPhase5",
        "(.+)§r§a picked the §r§cCorrupted Blue Relic§r§a!"
    )

    enum class DragonInfo(var hasSpawned: Boolean) {
        POWER(false),
        FLAME(false),
        ICE(false),
        SOUL(false),
        APEX(false)
    }

    private var inBerserkTeam = false
    private var inArcherTeam = false
    private var isHealer = false

    private val waitDuration = 2000.milliseconds

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (!startPattern.matches(event.message)) return
        reset()
        startP5()
    }

    private fun startP5() {
        if (!config.saySplit) return
        val currentClass = DungeonAPI.playerClass
        when (currentClass) {
            DungeonAPI.DungeonClass.MAGE -> inBerserkTeam = true
            DungeonAPI.DungeonClass.BERSERK -> inBerserkTeam = true
            DungeonAPI.DungeonClass.ARCHER -> inArcherTeam = true
            DungeonAPI.DungeonClass.TANK -> inArcherTeam = true
            DungeonAPI.DungeonClass.HEALER -> isHealer = true
            else -> return //throw error
        }
        DelayedRun.runDelayed(waitDuration) {
            val currentPower = 5//get power
        }
    }
    private fun reset() {
        DragonInfo.entries.forEach { it.hasSpawned = false }
        inArcherTeam = false
        inBerserkTeam = false
        isHealer = false
    }
}