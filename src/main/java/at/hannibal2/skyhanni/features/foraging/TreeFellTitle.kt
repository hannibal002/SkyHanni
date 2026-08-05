package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TreeFellTitle {
    private val config get() = SkyHanniMod.feature.foraging.trees.instantFellTreeTitle
    private var display: Renderable? = null
    private var treeFellTimestamp: SimpleTimeMark = SimpleTimeMark.farPast()

    /**
     * REGEX-TEST: PETALFALL! You felled the entire Tree!
     * REGEX-TEST: WOODPECKER! You felled the entire Tree!
     * REGEX-TEST: TIMBER! You felled the entire Tree!
     */
    private val treeFellPattern by RepoPattern.pattern(
        "foraging.trees.treefell",
        "[A-Z]+! You felled the entire Tree!"
    )

    @HandleEvent(onlyOnIslandTypeTag = [HAS_TREES])
    private fun onChat(event: SystemMessageEvent.Allow) {
        if (!isEnabled()) return
        if (treeFellPattern.matches(event.cleanMessage)) {
            val text = config.titleText.replace("&&", "§")
            display = Renderable.text(text)
            treeFellTimestamp = SimpleTimeMark.now()
        }
    }

    @HandleEvent(onlyOnIslandTypeTag = [HAS_TREES])
    private fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val display = display ?: return
        if (treeFellTimestamp.passedSince() > config.duration.seconds) {
            this.display = null
            return
        }
        config.treeFellPosition.renderRenderable(display, posLabel = "Tree Fell Title")
    }

    @HandleEvent(onlyOnIslandTypeTag = [HAS_TREES])
    private fun onWorldChange() {
        display = null
    }

    private fun isEnabled() = config.enabled
}
