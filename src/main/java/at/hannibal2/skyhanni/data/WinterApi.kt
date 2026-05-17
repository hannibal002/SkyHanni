package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.time.Month

@SkyHanniModule
object WinterApi {

    private val patternGroup = RepoPattern.group("event.winter")

    /**
     * REGEX-TEST: WOAH! [VIP] Georeek summoned a Reindrake from the depths!
     * REGEX-TEST: WOAH! [MVP+] DulceLyncis summoned TWO Reindrakes from the depths!
     */
    private val reindrakeSpawnPattern by patternGroup.pattern(
        "reindrake.spawn.message",
        "WOAH! .+ summoned (?:a Reindrake|TWO Reindrakes) from the depths!",
    )

    fun isReindrakeSpawnMessage(message: String) = reindrakeSpawnPattern.matches(message)

    private var inArea = false

    fun inWorkshop() = IslandType.WINTER.isInIsland()

    fun inGlacialCave() = inWorkshop() && inArea

    fun isDecember() = TimeUtils.getCurrentLocalDate().month == Month.DECEMBER

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        inArea = event.area == "Glacial Cave"
    }
}
