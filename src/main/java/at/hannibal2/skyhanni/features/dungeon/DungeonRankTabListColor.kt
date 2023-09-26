package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.TabListLineRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonRankTabListColor {
    private val config get() = SkyHanniMod.feature.dungeon.tabList

    val pattern = "§r(?<playerName>.*) §r§f\\(§r§d(?<className>.*) (?<classLevel>.*)§r§f\\)§r".toPattern()

    @SubscribeEvent
    fun onTabListText(event: TabListLineRenderEvent) {
        if (!isEnabled()) return

        pattern.matchMatcher(event.text) {
            val playerName = group("playerName")
//                println("playerName: $playerName")
            val split = playerName.split(" ")
            val sbLevel = split[0]
            val cleanName = split[1].cleanPlayerName()
//                println("cleanName: $cleanName")

            val className = group("className")
//                println("className: $className")

            val classLevel = group("classLevel")
//                println("classLevel: $classLevel")
            val lvl = classLevel.romanToDecimal()
//                println("lvl: $lvl")
            val lvlColor = getColor(lvl)

            event.text = "$sbLevel §b$cleanName §7(§e$className $lvlColor$lvl§7)"
        }
    }

    fun isEnabled() = LorenzUtils.inDungeons && config.coloredClassLevel
}
