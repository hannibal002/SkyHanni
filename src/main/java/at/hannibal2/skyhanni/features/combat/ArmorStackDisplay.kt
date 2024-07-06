package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.ActionBarValueUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@SkyHanniModule
object ArmorStackDisplay {
    private val config get() = SkyHanniMod.feature.combat.stackDisplayConfig
    private var display = ""
    private var displayTimer: SimpleTimeMark = SimpleTimeMark.farPast()
    private var armorMaxTime: Duration = ZERO

    /**
     * REGEX-TEST: §66,171/4,422❤  §6§l10ᝐ§r     §a1,295§a❈ Defense     §b525/1,355✎ §3400ʬ
     * REGEX-TEST: §66,171/4,422❤  §65ᝐ     §b-150 Mana (§6Wither Impact§b)     §b1,016/1,355✎ §3400ʬ
     */
    private val armorStackPattern by RepoPattern.pattern(
        "combat.armorstack.actionbar",
        " (?:§6|§6§l)(?<stack>\\d+[ᝐ⁑|҉Ѫ⚶])"
    )
    private val armorStackTierBonus by RepoPattern.pattern(
        "combat.armorstack.armor",
        "§6Tiered Bonus: (?<type>\\S+) \\((?<amount>\\d)\\/4\\)"
    )

    @SubscribeEvent
    fun onActionBar(event: ActionBarUpdateEvent) {
        if (!isEnabled()) return
        val stacks = armorStackPattern.findMatcher(event.actionBar) {
            "§6§l" + group("stack")
        } ?: ""
        display = stacks
    }

    @SubscribeEvent
    fun onSecond(event: SecondPassedEvent) {
        if (config.showTimer) {
            InventoryUtils.getArmor().forEach { armor ->
                armorStackTierBonus.findMatcher(armor?.getLore().toString()) {
                    group("amount")
                }?.let { result ->
                    armorMaxTime = when (result) {
                        "2" -> 4000.milliseconds
                        "3" -> 7000.milliseconds
                        "4" -> 10000.milliseconds
                        else -> ZERO
                    }
                    return
                }
            }
        }
    }

    @SubscribeEvent
    fun onActionBarValueUpdate(event: ActionBarValueUpdateEvent) {
        if (event.updated != ActionBarStatsData.ARMOR_STACK) return
        displayTimer = SimpleTimeMark.now().plus(armorMaxTime)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderString(display, posLabel = "Armor Stack Display")

        if (config.showTimer && displayTimer.isInFuture()) {
            config.timerPosition.renderString(displayTimer.timeUntil().toString(DurationUnit.SECONDS, 2), posLabel = "Armor Stack Timer Display")
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
