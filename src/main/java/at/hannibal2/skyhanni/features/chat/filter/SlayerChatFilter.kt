package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object SlayerChatFilter {
    private val patternGroup = CoreChatFilter.chatFilterGroup.group("slayer")
    private val config get() = CoreChatFilter.config

    private fun isEnabled(): Boolean = config.others.get()

    @HandleEvent
    fun onIslandJoin() {
        if (!isEnabled()) return
        if (SlayerApi.activeType != null) {
            CoreChatFilter.register(filters)
        } else {
            CoreChatFilter.unregister(filters)
        }
    }

    @HandleEvent
    fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        when (event.state) {
            GRINDING,
            BOSS_FIGHT,
            FAILED,
            SLAIN,
            -> CoreChatFilter.register(filters)
            NO_ACTIVE_QUEST -> CoreChatFilter.unregister(filters)
        }
    }

    val filters =
        setOf<ChatFilter>(
            SlayerQuestFilter,
            SlayerDropFilter,
        )

    object SlayerQuestFilter : AbstractRegexChatFilter("slayer_quest") {
        override val patterns by patternGroup.list(
            "slayer-quest",
            // start
            " {2}SLAYER QUEST STARTED!",
            " {3}» Slay .* Combat XP worth of .*.",

            // end
            " {2}SLAYER QUEST COMPLETE!",
            " {3}.*Slayer LVL 9 - LVL MAXED OUT!",
            " {3}» Talk to Maddox to claim your .* Slayer XP!",
            " {2}NICE! SLAYER BOSS SLAIN!", "You received kill credit for assisting on a slayer miniboss!",

            "✆ RING... .*",
        )
    }

    object SlayerDropFilter : AbstractRegexChatFilter("slayer_drop") {
        override val patterns by patternGroup.list(
            "slayer-drop",
            // Zombie
            // TODO merge patterns together. Just because old ones are designed poorly doesn't mean new ones need to be poor as well
            "RARE DROP! \\(.*x Revenant Viscera\\) .*",
            "RARE DROP! \\(Revenant Viscera\\) .*",
            "RARE DROP! \\(.*x Foul Flesh\\) .*",
            "RARE DROP! \\(Foul Flesh\\) .*",
            "RARE DROP! Golden Powder .*",
            "VERY RARE DROP! {2}\\(.* Pestilence Rune I\\) .*",
            "VERY RARE DROP! {2}\\(Revenant Catalyst\\) .*",
            "VERY RARE DROP! {2}\\(Undead Catalyst\\) .*",
            "VERY RARE DROP! {2}\\(◆ Pestilence Rune I\\) .*",

            // Tarantula
            "RARE DROP! Arachne's Keeper Fragment (.+)",
            "RARE DROP! Travel Scroll to Spider's Den Top of Nest (.+)",
            "VERY RARE DROP! {2}\\(◆ Bite Rune I\\) (.+)",
            "RARE DROP! \\((.+)x Toxic Arrow Poison\\) (.+)",
            "RARE DROP! \\(Toxic Arrow Poison\\) (.+)",
            "VERY RARE DROP! {2}\\(Bane of Arthropods VI\\) (.+)",

            // Enderman
            "RARE DROP! \\(.*x Twilight Arrow Poison\\) .*",
            "VERY RARE DROP! {2}\\(Mana Steal I\\) .*",
            "VERY RARE DROP! {2}\\(Sinful Dice\\) .*",
            "VERY RARE DROP! {2}\\(Null Atom\\) .*",
            "VERY RARE DROP! {2}\\(Transmission Tuner\\) .*",
            "VERY RARE DROP! {2}\\(Mana Steal I\\) .*",
            "VERY RARE DROP! {2}\\(◆ Endersnake Rune I\\) .*",
            "CRAZY RARE DROP! {2}\\(Pocket Espresso Machine\\) .*",
            "VERY RARE DROP! {2}\\(◆ End Rune I\\) .*",
            "VERY RARE DROP! {2}\\(Hazmat Enderman\\) .*",

            // Blaze
            "VERY RARE DROP! {2}\\(Wisp's Ice-Flavored Water I Splash Potion\\) .*",
            "RARE DROP! \\(Bundle of Magma Arrows\\) .*",
            "VERY RARE DROP! {2}\\(\\d+x (Glowstone|Blaze Rod|Magma Cream|Nether Wart) Distillate\\) .*",
        )
    }
}
