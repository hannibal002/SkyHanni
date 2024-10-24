package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.chat.CrystalNucleusConfig.CrystalNucleusMessageTypes
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CrystalNucleusChatFilter {

    class NucleusChatFilterRes(private var blockMessage: String? = null, private var newMessage: String? = null) {
        fun getPair(): Pair<String?, String?> {
            return Pair(blockMessage, newMessage)
        }
    }

    private val config get() = SkyHanniMod.feature.chat.filterType.crystalNucleus
    private val patternGroup = RepoPattern.group("filter.crystalnucleus")

    private var unclosedRunCompleted = false
    private var unclosedCrystalCollected = false
    private var crystalCount = 0
    private var crystalCollected = ""

    /**
     * REGEX-TEST: §3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val runCompletedWrapperPattern by patternGroup.pattern(
        "run.completed",
        "§3§l▬{64}",
    )

    /**
     * REGEX-TEST: §5§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val crystalCollectedWrapperPattern by patternGroup.pattern(
        "crystal.collected.wrapper",
        "§5§l▬{64}",
    )

    /**
     * REGEX-TEST: §f                       §r§5§l✦ CRYSTAL FOUND §r§7(1§r§7/5§r§7)
     */
    private val crystalCollectedCountPattern by patternGroup.pattern(
        "crystal.collected.count",
        "§f *§r§5§l✦ CRYSTAL FOUND §r§7\\((?<count>\\d)§r§7/5§r§7\\)",
    )

    /**
     * REGEX-TEST: §f                              §r§5Amethyst Crystal
     * REGEX-TEST: §f                              §r§bSapphire Crystal
     * REGEX-TEST: §f                                §r§6Amber Crystal
     * REGEX-TEST: §f                                §r§eTopaz Crystal
     * REGEX-TEST: §f                                §r§aJade Crystal
     */
    private val crystalCollectedIdentifierPattern by patternGroup.pattern(
        "crystal.collected.id",
        "§f *§r(?<crystal>(.* Crystal)) *",
    )

    /**
     * REGEX-TEST: §5§l✦ §r§dYou placed the §r§bSapphire Crystal§r§d!
     */
    private val crystalPlacedPattern by patternGroup.pattern(
        "crystal.placed",
        "§5§l✦ §r§dYou placed the §r(?<crystal>.* Crystal)§r§d!",
    )

    /**
     * REGEX-TEST: §aYou found §r§cScavenged Diamond Axe §r§awith your §r§cMetal Detector§r§a!
     * REGEX-TEST: §aYou found §r§cScavenged Emerald Hammer §r§awith your §r§cMetal Detector§r§a!
     * REGEX-TEST: §aYou found §r§a☘ Flawed Jade Gemstone §r§8x2 §r§awith your §r§cMetal Detector§r§a!
     */
    private val scavengeLootPattern by patternGroup.pattern(
        "divan.scavenge",
        "§aYou found §r(?<loot>.*) §r§awith your §r§cMetal Detector§r§a!",
    )

    /**
     * REGEX-TEST: Thanks for bringing me the §9Synthetic Heart§r! Bring me 5 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9Robotron Reflector§r! Bring me 5 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9Superlite Motor§r! Bring me 4 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9Synthetic Heart§r! Bring me 3 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9FTX 3070§r! Bring me 2 more components to fix the giant!
     * REGEX-TEST: Thanks for bringing me the §9Electron Transmitter§r! Bring me one more component to fix the giant!
     * REGEX-TEST: §rYou've brought me all of the components!
     * REGEX-TEST: §rYou've brought me all of the components... I think? To be honest, I kind of lost count...
     * REGEX-TEST: Wait a minute. This will work just fine.
     */
    @Suppress("MaxLineLength")
    private val componentSubmittedPattern by patternGroup.pattern(
        "precursor.submitted",
        ".*(Wait a minute. This will work just fine.|You've brought me all|me the (?<component>.*)§r! Bring me (?<remaining>(\\d|one)) more).*",
    )

    fun block(message: String): NucleusChatFilterRes? {
        if (!isEnabled()) return null

        return blockCrystalCollected(message)
            ?: blockCrystalPlaced(message)
            ?: blockRunCompleted(message)
            ?: blockNonToolScavenge(message)
            ?: blockNPC(message)
    }

    private fun blockCrystalCollected(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.CRYSTAL_COLLECTED)) return null
        if (crystalCollectedWrapperPattern.matches(message)) {
            unclosedCrystalCollected = !unclosedCrystalCollected
            return NucleusChatFilterRes("crystal_collected")
        }

        if (!unclosedCrystalCollected) return null

        crystalCollectedCountPattern.matchMatcher(message) {
            crystalCount = group("count").toInt()
        }

        crystalCollectedIdentifierPattern.matchMatcher(message) {
            crystalCollected = group("crystal")
            return NucleusChatFilterRes("", "§5§l✦ $crystalCollected §5found§d! §7(§a$crystalCount§7/§a5§7)")
        }

        return NucleusChatFilterRes("crystal_collected")
    }

    private fun blockCrystalPlaced(message: String): NucleusChatFilterRes? {
        if (!inNucleus()) return null
        if (!shouldBlock(CrystalNucleusMessageTypes.CRYSTAL_PLACED)) return null

        if (message == "  §r§dKeep exploring the §r§5Crystal Hollows §r§dto find the rest!") return NucleusChatFilterRes("crystal_placed")
        crystalPlacedPattern.matchMatcher(message) {
            return NucleusChatFilterRes("", "§5§l✦ ${group("crystal")} §5placed§d!")
        }
        return null
    }

    private fun blockRunCompleted(message: String): NucleusChatFilterRes? {
        if (!inNucleus()) return null
        if (!shouldBlock(CrystalNucleusMessageTypes.RUN_COMPLETED)) return null

        if (runCompletedWrapperPattern.matches(message)) {
            unclosedRunCompleted = !unclosedRunCompleted
            return NucleusChatFilterRes("run_completed")
        }

        if (message == "§7Pick it up near the §r§5Nucleus Vault§r§7!") return NucleusChatFilterRes("", "§5Crystal Nucleus Run complete§d!")
        if (!unclosedRunCompleted) return null

        return NucleusChatFilterRes("run_completed")
    }

    private fun blockNonToolScavenge(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.NON_TOOL_SCAVENGE)) return null

        scavengeLootPattern.matchMatcher(message) {
            if (!group("loot").startsWith("§cScavenged")) return NucleusChatFilterRes("non_tool_scavenge")
        }

        return null
    }

    private fun blockNPC(message: String): NucleusChatFilterRes? {
        if (!message.startsWith("§e[NPC]")) return null

        blockProfessorRobot(message)?.let { return it }
        blockKingYolkar(message)?.let { return it }
        blockKeepers(message)?.let { return it }
        blockGoblinGuards(message)?.let { return it }

        return null
    }

    private fun blockProfessorRobot(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.NPC_PROF_ROBOT)) return null
        if (!message.startsWith("§e[NPC] Professor Robot")) return null

        componentSubmittedPattern.matchMatcher(message) {
            if (message.contains("brought me all") || message.contains("This will work just fine.")) {
                return NucleusChatFilterRes("", "§e[NPC] Professor Robot§f: §rAll components submitted.")
            } else {
                return NucleusChatFilterRes(
                    "",
                    "§e[NPC] Professor Robot§f: ${group("component")} submitted. ${group("remaining")} components left.",
                )
            }
        }

        return NucleusChatFilterRes("npc_prof_robot")
    }

    private fun blockKingYolkar(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.NPC_KING_YOLKAR)) return null
        if (!message.startsWith("§e[NPC] §6King Yolkar")) return null

        // §e[NPC] §6King Yolkar§f: §r*rumble* *rumble*
        if (message.contains("*rumble* *rumble*")) {
            return NucleusChatFilterRes("", "§e[NPC] §6King Yolkar§f: ...")
        }
        // §e[NPC] §6King Yolkar§f: §rBring me back a §9Goblin Egg §rof any type and we can teach her a lesson!
        if (message.contains("Bring me back a §9Goblin Egg")) {
            return NucleusChatFilterRes("", "§e[NPC] §6King Yolkar§f: §rBring me §a3 §9Goblin Egg §rof any type.")
        }
        // §e[NPC] §6King Yolkar§f: §rThis egg will help me stomach my pain.
        if (message.contains("This egg will help me stomach my pain.")) {
            return NucleusChatFilterRes("", "§e[NPC] §6King Yolkar§f: §2King's Scent§r applied.")
        }

        return NucleusChatFilterRes("npc_king_yolkar")
    }

    private fun blockGoblinGuards(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.NPC_GOBLIN_GUARDS)) return null
        if (!message.startsWith("§c[GUARD]")) return null
        return NucleusChatFilterRes("npc_goblin_guard")
    }

    private fun blockKeepers(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.NPC_DIVAN_KEEPERS)) return null
        if (!message.startsWith("§e[NPC] §6Keeper of ")) return null

        if (message.contains("You found all of the items!")) {
            return NucleusChatFilterRes("", "§e[NPC] §6Keeper of §k§6Gold§f: §rAll tools submitted.")
        }

        return NucleusChatFilterRes("npc_divan_keeper")
    }

    private fun shouldBlock(type: CrystalNucleusMessageTypes) = config.modifiedMessages.contains(type)
    private fun inNucleus() = LorenzUtils.skyBlockArea == "Crystal Nucleus"
    private fun isEnabled() = config.enabled && IslandType.CRYSTAL_HOLLOWS.isInIsland()
}
