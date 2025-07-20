package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.chat.SlayerFilterDropsConfig.SimpleBlazeSlayerRewardTypes
import at.hannibal2.skyhanni.config.features.chat.SlayerFilterDropsConfig.SimpleEndermanSlayerRewardTypes
import at.hannibal2.skyhanni.config.features.chat.SlayerFilterDropsConfig.SimpleTarantulaSlayerRewardTypes
import at.hannibal2.skyhanni.config.features.chat.SlayerFilterDropsConfig.SimpleZombieSlayerRewardTypes
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import java.util.regex.Pattern

@SkyHanniModule
object SlayerFilterDrops {

    private val config get() = SkyHanniMod.feature.chat.filterType.slayers.slayerDrops

    private val zombieSlayerDropPatterns = listOf(
        "§b§lRARE DROP! §r§7\\(§r§f§r§7(.*)x §r§f§r§9Revenant Viscera§r§7\\) (.*)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§9Revenant Viscera§r§7\\) (.*)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§7(.*)x §r§f§r§9Foul Flesh§r§7\\) (.*)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§9Foul Flesh§r§7\\) (.*)".toPattern(),
        "§6§lRARE DROP! §r§5Golden Powder (.*)".toPattern(),
        "§9§lVERY RARE DROP! §r§7\\(§r§f§r§2(.*) Pestilence Rune I§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! §r§7\\(§r§f§r§5Revenant Catalyst§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! §r§7\\(§r§f§r§9Undead Catalyst§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! §r§7\\(§r§f§r§2◆ Pestilence Rune I§r§7\\) §r§b(.*)".toPattern(),
    )

    private val tarantulaSlayerDropPatterns = listOf(
        "§6§lRARE DROP! §r§9Arachne's Keeper Fragment (.+)".toPattern(),
        "§6§lRARE DROP! §r§5Travel Scroll to Spider's Den Top of Nest (.+)".toPattern(),
        "§9§lVERY RARE DROP! §r§7\\(§r§f§r§a◆ Bite Rune I§r§7\\) (.+)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§7(.+)x §r§f§r§aToxic Arrow Poison§r§7\\) (.+)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§aToxic Arrow Poison§r§7\\) (.+)".toPattern(),
        "§5§lVERY RARE DROP! §r§7\\(§r§9Bane of Arthropods VI§r§7\\) (.+)".toPattern(),
    )

    private val endermanSlayerDropPatterns = listOf(
        "§b§lRARE DROP! §r§7\\(§r§f§r§7(.*)x §r§f§r§aTwilight Arrow Poison§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! §r§7\\(§r§fMana Steal I§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! §r§7\\(§r§f§r§5Sinful Dice§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! §r§7\\(§r§f§r§9Null Atom§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! §r§7\\(§r§f§r§5Transmission Tuner§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! §r§7\\(§r§fMana Steal I§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! §r§7\\(§r§f§r§5◆ Endersnake Rune I§r§7\\) (.*)".toPattern(),
        "§d§lCRAZY RARE DROP! §r§7\\(§r§f§r§fPocket Espresso Machine§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! §r§7\\(§r§f§r§5◆ End Rune I§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! §r§7\\(§r§f§r§6Hazmat Enderman§r§7\\) .*".toPattern(),
    )

    private val blazeSlayerDropPatterns = listOf(
        "§9§lVERY RARE DROP! §r§7\\(§r§f§r§fWisp's Ice-Flavored Water I Splash Potion§r§7\\) (.*)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§5Bundle of Magma Arrows§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! §r§7\\(§r§f§r§7\\d+x §r§f§r§9(Glowstone|Blaze Rod|Magma Cream|Nether Wart) Distillate§r§7\\) (.*)".toPattern(),
    )

    private var zombieRewardPatterns: Map<Pair<Pattern, SimpleZombieSlayerRewardTypes>, String> = emptyMap()
    private var tarantulaRewardPatterns: Map<Pair<Pattern, SimpleTarantulaSlayerRewardTypes>, String> = emptyMap()
    private var endermanRewardPatterns: Map<Pair<Pattern, SimpleEndermanSlayerRewardTypes>, String> = emptyMap()
    private var blazeRewardPatterns: Map<Pair<Pattern, SimpleBlazeSlayerRewardTypes>, String> = emptyMap()

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onRepoReload(event: RepositoryReloadEvent) {
        zombieRewardPatterns = mapOf(
            zombieSlayerDropPatterns[0] to SimpleZombieSlayerRewardTypes.REVENANT_VISCERA to "zombie_slayer_revenant_viscera",
            zombieSlayerDropPatterns[1] to SimpleZombieSlayerRewardTypes.REVENANT_VISCERA to "zombie_slayer_revenant_viscera",
            zombieSlayerDropPatterns[2] to SimpleZombieSlayerRewardTypes.FOUL_FLESH to "zombie_slayer_foul_flesh",
            zombieSlayerDropPatterns[3] to SimpleZombieSlayerRewardTypes.FOUL_FLESH to "zombie_slayer_foul_flesh",
            zombieSlayerDropPatterns[4] to SimpleZombieSlayerRewardTypes.GOLDEN_POWDER to "zombie_slayer_golden_powder",
            zombieSlayerDropPatterns[5] to SimpleZombieSlayerRewardTypes.PESTILENCE_RUNE to "zombie_slayer_pestilence_rune",
            zombieSlayerDropPatterns[6] to SimpleZombieSlayerRewardTypes.REVENANT_CATALYST to "zombie_slayer_revenant_catalyst",
            zombieSlayerDropPatterns[7] to SimpleZombieSlayerRewardTypes.UNDEAD_CATALYST to "zombie_slayer_undead_catalyst",
            zombieSlayerDropPatterns[8] to SimpleZombieSlayerRewardTypes.PESTILENCE_RUNE to "zombie_slayer_pestilence_rune",
        )

        tarantulaRewardPatterns = mapOf(
            tarantulaSlayerDropPatterns[0] to SimpleTarantulaSlayerRewardTypes.ARACHNES_KEEPER_FRAGMENT
                to "tarantula_slayer_arachnes_keeper_fragment",
            tarantulaSlayerDropPatterns[1] to SimpleTarantulaSlayerRewardTypes.TRAVEL_SCROLL to "tarantula_slayer_travel_scroll",
            tarantulaSlayerDropPatterns[2] to SimpleTarantulaSlayerRewardTypes.BITE_RUNE to "tarantula_slayer_bite_rune",
            tarantulaSlayerDropPatterns[3] to SimpleTarantulaSlayerRewardTypes.TOXIC_ARROW_POISON to "tarantula_slayer_toxic_arrow_poison",
            tarantulaSlayerDropPatterns[4] to SimpleTarantulaSlayerRewardTypes.TOXIC_ARROW_POISON to "tarantula_slayer_toxic_arrow_poison",
            tarantulaSlayerDropPatterns[5] to SimpleTarantulaSlayerRewardTypes.BANE_OF_ARTHROPODS to "tarantula_slayer_bane_of_arthropods",
        )
        @Suppress("MaxLineLength")
        endermanRewardPatterns = mapOf(
            endermanSlayerDropPatterns[0] to SimpleEndermanSlayerRewardTypes.TWILIGHT_ARROW_POISON to "enderman_slayer_twilight_arrow_poison",
            endermanSlayerDropPatterns[1] to SimpleEndermanSlayerRewardTypes.MANA_STEAL to "enderman_slayer_mana_steal",
            endermanSlayerDropPatterns[2] to SimpleEndermanSlayerRewardTypes.SINFUL_DICE to "enderman_slayer_sinful_dice",
            endermanSlayerDropPatterns[3] to SimpleEndermanSlayerRewardTypes.NULL_ATOM to "enderman_slayer_null_atom",
            endermanSlayerDropPatterns[4] to SimpleEndermanSlayerRewardTypes.TRANSMISSION_TUNER to "enderman_slayer_transmission_tuner",
            endermanSlayerDropPatterns[5] to SimpleEndermanSlayerRewardTypes.MANA_STEAL to "enderman_slayer_mana_steal",
            endermanSlayerDropPatterns[6] to SimpleEndermanSlayerRewardTypes.ENDERSNAKE_RUNE to "enderman_slayer_endersnake_rune",
            endermanSlayerDropPatterns[7] to SimpleEndermanSlayerRewardTypes.POCKET_ESPRESSO_MACHINE to "enderman_slayer_pocket_espresso_machine",
            endermanSlayerDropPatterns[8] to SimpleEndermanSlayerRewardTypes.END_RUNE to "enderman_slayer_end_rune",
            endermanSlayerDropPatterns[9] to SimpleEndermanSlayerRewardTypes.HAZMAT_ENDERMAN to "enderman_slayer_hazmat_enderman",
        )

        blazeRewardPatterns = mapOf(
            blazeSlayerDropPatterns[0] to SimpleBlazeSlayerRewardTypes.WISPS_ICE_FLAVORED_WATER to "blaze_slayer_wisps_ice_flavored_water",
            blazeSlayerDropPatterns[1] to SimpleBlazeSlayerRewardTypes.MAGMA_ARROWS to "blaze_slayer_magma_arrows",
            blazeSlayerDropPatterns[2] to SimpleBlazeSlayerRewardTypes.DISTILLATE to "blaze_slayer_distillate",
        )
    }

    fun block(message: String): String? {
        if (!config.enabled) return null

        for ((patternToReward, returnReason) in zombieRewardPatterns) {
            patternToReward.first.matchMatcher(message) {
                return if (config.simpleZombieSlayerTypes.contains(patternToReward.second)) returnReason
                else "no_filter"
            }
        }

        for ((patternToReward, returnReason) in tarantulaRewardPatterns) {
            patternToReward.first.matchMatcher(message) {
                return if (config.simpleTarantulaSlayerTypes.contains(patternToReward.second)) returnReason
                else "no_filter"
            }
        }

        for ((patternToReward, returnReason) in endermanRewardPatterns) {
            patternToReward.first.matchMatcher(message) {
                return if (config.simpleEndermanSlayerTypes.contains(patternToReward.second)) returnReason
                else "no_filter"
            }
        }

        for ((patternToReward, returnReason) in blazeRewardPatterns) {
            patternToReward.first.matchMatcher(message) {
                return if (config.simpleBlazeSlayerTypes.contains(patternToReward.second)) returnReason
                else "no_filter"
            }
        }

        return null
    }
}
