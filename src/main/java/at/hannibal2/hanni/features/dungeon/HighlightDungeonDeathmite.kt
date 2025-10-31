package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.addAlpha
import at.hannibal2.hanni.utils.ColorUtils.toChromaColor
import at.hannibal2.hanni.utils.LorenzColor

@HanniModule
object HighlightDungeonDeathmite {

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!HanniMod.feature.dungeon.highlightDeathmites) return
        // TODO config option, with chroma color
        if (event.mob.name == "Deathmite") event.mob.highlight(LorenzColor.DARK_RED.toColor().addAlpha(20).toChromaColor())
    }
}
