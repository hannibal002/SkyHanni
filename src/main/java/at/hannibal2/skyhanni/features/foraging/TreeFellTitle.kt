package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.convertToFormatted
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TreeFellTitle {
    private val config get() = SkyHanniMod.feature.foraging.trees.timberTitle

    /**
     * REGEX-TEST: TIMBER! You felled the entire Tree!
     */
    private val treeFellPattern by RepoPattern.pattern(
        "foraging.trees.treefell",
        "TIMBER! You felled the entire Tree!",
    )

    @HandleEvent(onlyOnIslandTypeTag = [HAS_TREES])
    private fun onSystemMessage(event: SystemMessageEvent.Allow) {
        if (!isEnabled()) return
        if (!treeFellPattern.matches(event.cleanMessage)) return
        val text = config.titleText.convertToFormatted()
        TitleManager.sendTitle(titleText = text, duration = config.duration.seconds)
    }

    private fun isEnabled() = config.enabled
}
