package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.ActionBarValueUpdate
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.intellij.lang.annotations.Language

enum class ActionBarStatsData(@Language("RegExp") rawPattern: String) {
    Health(
        // language=RegExp
        "§[c6](?<health>[\\d,]+)/[\\d,]+❤.*"
    ),
    defense(
        // language=RegExp
        ".*§a(?<defense>[\\d,]+)§a❈.*"
    ),
    mana(
        // language=RegExp
        ".*§b(?<mana>[\\d,]+)/[\\d,]+✎.*"
    ),
    riftTime(
        // language=RegExp
        "§[a7](?<riftTime>[\\dms ]+)ф.*"
    ),
    skyBlockXP(
        // language=RegExp
        ".*(§b\\+\\d+ SkyBlock XP §.\\([^()]+\\)§b \\(\\d+/\\d+\\)).*"
    )
    ;

    internal val pattern by RepoPattern.pattern("actionbar.$name", rawPattern)
    var value: String = ""

    companion object {
        @SubscribeEvent
        fun onActionBar(event: LorenzActionBarEvent) {
            if (!LorenzUtils.inSkyBlock) return

            entries.mapNotNull { value ->
                value.pattern.matchMatcher(event.message) {
                    val newValue = group(1)
                    if (value.value != newValue) {
                        value.value = newValue
                        return@mapNotNull ActionBarValueUpdate(value)
                    }
                }
                null
            }.forEach { it.postAndCatch() }
        }
    }
}
