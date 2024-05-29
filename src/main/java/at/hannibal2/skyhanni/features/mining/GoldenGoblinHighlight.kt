package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class GoldenGoblinHighlight {

    private val config get() = SkyHanniMod.feature.mining.highlightYourGoldenGoblin

    private val goblinPattern by RepoPattern.pattern("mining.mob.golden.goblin", "Golden Goblin|Diamond Goblin")

    private fun isEnabled() = LorenzUtils.inMiningIsland() && config

    private val timeOut = 10.seconds

    private var lastChatMessage = SimpleTimeMark.farPast()
    private var lastGoblinSpawn = SimpleTimeMark.farPast()
    private var lastGoblin: Mob? = null

    @SubscribeEvent
    fun onChatEvent(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (!MiningNotifications.goldenGoblinSpawn.matches(event.message) &&
            !MiningNotifications.diamondGoblinSpawn.matches(event.message)
        ) return
        lastChatMessage = SimpleTimeMark.now()
        handle()
    }

    @SubscribeEvent
    fun onMobEvent(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled()) return
        if (!goblinPattern.matches(event.mob.name)) return
        lastGoblin = event.mob
        lastGoblinSpawn = SimpleTimeMark.now()
        handle()
    }

    private fun handle() {
        if (lastChatMessage.passedSince() > timeOut || lastGoblinSpawn.passedSince() > timeOut) return
        lastChatMessage = SimpleTimeMark.farPast()
        lastGoblinSpawn = SimpleTimeMark.farPast()
        lastGoblin?.highlight(LorenzColor.GREEN.toColor())
        lastGoblin = null
    }

}
