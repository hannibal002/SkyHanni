package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ArmorStackDisplay {
    private val config get() = SkyHanniMod.feature.combat.stackDisplayConfig
    private var display: Renderable? = null

    /**
     * REGEX-TEST: §66,171/4,422❤  §6§l10ᝐ§r     §a1,295§a❈ Defense     §b525/1,355✎ §3400ʬ
     * REGEX-TEST: §66,171/4,422❤  §65ᝐ     §b-150 Mana (§6Wither Impact§b)     §b1,016/1,355✎ §3400ʬ
     */
    private val armorStackPattern by RepoPattern.pattern(
        "combat.armorstack.actionbar",
        " (?:§6|§6§l)(?<stack>\\d+[ᝐ⁑|҉Ѫ⚶])",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onActionBar(event: ActionBarUpdateEvent) {
        if (!config.enabled) return
        val stacks = armorStackPattern.findMatcher(event.actionBar) {
            "§6§l" + group("stack")
        }.orEmpty()
        display = Renderable.text(stacks)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        val display = display ?: return
        config.position.renderRenderable(display, posLabel = "Armor Stack Display")
    }
}
