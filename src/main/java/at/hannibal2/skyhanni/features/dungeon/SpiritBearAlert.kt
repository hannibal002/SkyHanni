package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SpiritBearAlert {

    private val config get() = SkyHanniMod.feature.dungeon
    private var inF4BossRoom = false

    // change mark when enter boss room
    @HandleEvent
    private fun onBossEnter(event: DungeonBossRoomEnterEvent) {
        inF4BossRoom = DungeonApi.getCurrentBoss() == DungeonFloor.F4
    }

    // reset mark when run end
    @HandleEvent
    private fun onBossEnd(event: DungeonCompleteEvent) {
        inF4BossRoom = false
    }
    //dual reset
    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() {
        inF4BossRoom = false
    }

    // show a alert when spirit bear spawn
    // and highlight spirit bear during F/M4 boss fight
    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!inF4BossRoom) return
        if (!config.spiritBearAlert) return
        if (event.mob.name != "Spirit Bear") return

        event.mob.highlight(LorenzColor.RED.toColor())
        TitleManager.sendTitle(titleText="§c§lSpirit Bear Spawn!",duration = 3.seconds)
        ChatUtils.chat("§cSpirit Bear §ehas spawned!")
        SoundUtils.playBeepSound()
    }
}
