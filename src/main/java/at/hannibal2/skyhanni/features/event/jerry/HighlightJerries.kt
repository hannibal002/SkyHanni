package at.hannibal2.hanni.features.event.jerry

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.mob.Mob
import at.hannibal2.hanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.hanni.data.mob.MobData
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ConditionalUtils.onEnable
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RegexUtils.matchGroup
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object HighlightJerries {

    private val config get() = HanniMod.feature.event.jerry

    /**
     * REGEX-TEST: Blue Jerry
     */
    private val jerryPattern by RepoPattern.pattern("jerry.highlight", "(?<color>\\w+) Jerry")

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!config.highlightJerries.get() && !config.lineJerries.get()) return
        parseJerry(event.mob)
    }

    private fun parseJerry(mob: Mob) {
        val type = jerryPattern.matchGroup(mob.name, "color") ?: return
        if (!mob.belongsToPlayer()) return
        val color = when (type) {
            "Green" -> LorenzColor.GREEN
            "Blue" -> LorenzColor.BLUE
            "Purple" -> LorenzColor.DARK_PURPLE
            "Golden" -> LorenzColor.GOLD
            else -> return
        }.toChromaColor()
        mob.highlight(color) { config.highlightJerries.get() }
        mob.lineToPlayer(color) { config.lineJerries.get() }
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        config.highlightJerries.onEnable { MobData.skyblockMobs.forEach { parseJerry(it) } }
        config.lineJerries.onEnable { MobData.skyblockMobs.forEach { parseJerry(it) } }
    }
}
