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
    /**
     * REGEX-TEST: §7Health: §a+205, §7Defense: §a+55, §7Intelligence: §a+125, §7Combat Wisdom: §a+0.75,  §8[§8⚔§8] §8[§8⚔§8], , §bArachno Resistance II, §7Grants §a+3❈ Defense §7against §aspiders§7., §bVeteran I, §7Grants §3+0.75☯ Combat Wisdom§7., , §6Tiered Bonus: Arcane Vision (2/4), §7Gives you the ability to see the runic, §7affinity of enemies., §7, §7Using the proper §bRune §7when casting spells, §7from §bRunic Items §7grants 1 stack of §6Arcane, §6Vision Ѫ§7., §7, §7Each §6Arcane Vision Ѫ §7stack grants you §c+2%, §c§7damage on your §bRunic Spells§7., §7, §7At §c10 §7stacks, the spells also explode on hit., §7, §7Lose 1 stack after §c4s §7of not gaining a stack., , §7§8This item can be reforged!, §6§lLEGENDARY LEGGINGS
     */
    private val armorStackTierBonus by RepoPattern.pattern(
        "combat.armorstack.armor",
        "§6Tiered Bonus: (?<type>.*) \\((?<amount>\\d)\\/4\\)"
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
        if (!isEnabled() || !config.showTimer) return
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

    @SubscribeEvent
    fun onActionBarValueUpdate(event: ActionBarValueUpdateEvent) {
        if (!isEnabled() || event.updated != ActionBarStatsData.ARMOR_STACK) return
        displayTimer = SimpleTimeMark.now() + armorMaxTime
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
