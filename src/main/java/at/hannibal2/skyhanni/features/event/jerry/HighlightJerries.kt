package at.hannibal2.skyhanni.features.event.jerry

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.onEnable
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object HighlightJerries {

    private val config get() = SkyHanniMod.feature.event.jerry

    /** REGEX-TEST: Blue Jerry
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
        }
        mob.highlight(color.toColor()) { config.highlightJerries.get() }
        mob.lineToPlayer(color.toColor()) { config.lineJerries.get() }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.highlightJerries.onEnable { MobData.skyblockMobs.forEach { parseJerry(it) } }
        config.lineJerries.onEnable { MobData.skyblockMobs.forEach { parseJerry(it) } }
    }
}
