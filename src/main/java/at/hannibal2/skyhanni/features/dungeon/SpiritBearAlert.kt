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
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SpiritBearAlert {

    private val config get() = SkyHanniMod.feature.dungeon
    private var inF4BossRoom = false //mark whether in F/M4

    // change mark when enter dungeon f/m4 boss room
    @HandleEvent
    private fun onDungeonBossRoomEnter(event: DungeonBossRoomEnterEvent) {
        if (!config.spiritBearAlert) return
        inF4BossRoom = DungeonApi.getCurrentBoss() == DungeonFloor.F4
    }

    // reset mark when a run end
    @HandleEvent
    private fun onDungeonEnd(event: DungeonCompleteEvent) {
        inF4BossRoom = false
    }

    // fallback reset if player disconnects or leaves
    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() {
        inF4BossRoom = false
    }

    /* show a alert when spirit bear spawn
    and highlight spirit bear during F/M4 boss fight
    * */
    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    private fun onNewMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!config.spiritBearAlert) return
        if (!inF4BossRoom) return
        if (event.mob.name != "Spirit Bear") return

        event.mob.highlight(LorenzColor.RED.toColor())
        TitleManager.sendTitle(titleText = "§c§lSpirit Bear Spawned!", duration = 3.seconds)
        ChatUtils.chat("Spirit Bear Spawned!")
    }
}
