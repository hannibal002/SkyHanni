package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LorenzColor

@SkyHanniModule
object HighlightDungeonDeathmite {

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!SkyHanniMod.feature.dungeon.highlightDeathmites) return
        // TODO config option, with chroma color
        if (event.mob.name == "Deathmite") event.mob.highlight(LorenzColor.DARK_RED.toColor().addAlpha(20).toChromaColor())
    }
}
