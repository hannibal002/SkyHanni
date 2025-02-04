package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.chat.CrystalNucleusConfig.CrystalNucleusMessageTypes
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.componentListPattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.componentListPreamblePattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.componentSubmittedPattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.crystalCollectedCountPattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.crystalCollectedIdentifierPattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.crystalCollectedWrapperPattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.crystalPlacedPattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.genericKeeperMessage
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.goblinGuardExitMessagePattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.runCompletedWrapperPattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.scavengeLootPattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.scavengeSecondaryPattern
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalNucleusApi.unclosedCrystalCollected
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CrystalNucleusChatFilter {

    class NucleusChatFilterRes(private var blockMessage: String? = null, private var newMessage: String? = null) {
        fun getPair(): Pair<String?, String?> {
            return Pair(blockMessage, newMessage)
        }
    }

    private val config get() = SkyHanniMod.feature.chat.filterType.crystalNucleus

    private var unclosedRunCompleted = false
    private var crystalCount = 0
    private var crystalCollected = ""
    private var lastKeeper = ""
    private var inCompListPreamble = false

    fun block(message: String): NucleusChatFilterRes? {
        if (!isEnabled()) return null

        return blockCrystalCollected(message)
            ?: blockCrystalPlaced(message)
            ?: blockRunCompleted(message)
            ?: blockNonToolScavenge(message)
            ?: blockGoblinGuards(message)
            ?: blockNpc(message)
    }

    private fun blockCrystalCollected(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.CRYSTAL_COLLECTED)) return null
        if (crystalCollectedWrapperPattern.matches(message)) {
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
        if (scavengeSecondaryPattern.matches(message)) return NucleusChatFilterRes("non_tool_scavenge")

        return null
    }

    private fun blockGoblinGuards(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.NPC_GOBLIN_GUARDS)) return null
        if (goblinGuardExitMessagePattern.matches(message)) {
            return NucleusChatFilterRes("npc_goblin_guard")
        }

        // §c[GUARD] Ooblak§r§f: §r§eTHEY'RE STEALING THE CRYSTAL! GET THEM!
        if (!message.startsWith("§c[GUARD]")) return null

        return NucleusChatFilterRes("npc_goblin_guard")
    }

    private fun blockNpc(message: String): NucleusChatFilterRes? {
        if (!message.startsWith("§e[NPC]")) return null

        return blockProfessorRobot(message)
            ?: blockKingYolkar(message)
            ?: blockKeepers(message)
    }

    private fun blockProfessorRobot(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.NPC_PROF_ROBOT)) return null
        if (inCompListPreamble && componentListPattern.matches(message)) {
            return NucleusChatFilterRes("npc_prof_robot")
        }
        if (!message.startsWith("§e[NPC] Professor Robot")) return null

        if (componentListPreamblePattern.matches(message)) {
            inCompListPreamble = true
            DelayedRun.runDelayed(2.seconds) {
                inCompListPreamble = false
            }
            return NucleusChatFilterRes("npc_prof_robot")
        }

        componentSubmittedPattern.findMatcher(message) {
            if (message.contains("brought me all") || message.contains("This will work just fine.")) {
                return NucleusChatFilterRes("", "§e[NPC] Professor Robot§f: §rAll components submitted.")
            }

            return NucleusChatFilterRes(
                "",
                "§e[NPC] Professor Robot§f: ${group("component")} submitted. ${group("remaining")} components left.",
            )
        }

        return NucleusChatFilterRes("npc_prof_robot")
    }

    private fun blockKingYolkar(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.NPC_KING_YOLKAR)) return null
        if (!message.startsWith("§e[NPC] §6King Yolkar")) return null

        return when {
            // §e[NPC] §6King Yolkar§f: §r*rumble* *rumble*
            message.contains("*rumble* *rumble*") ->
                NucleusChatFilterRes("", "§e[NPC] §6King Yolkar§f: ...")

            // §e[NPC] §6King Yolkar§f: §rBring me back a §9Goblin Egg §rof any type and we can teach her a lesson!
            message.contains("Bring me back a §9Goblin Egg") ->
                NucleusChatFilterRes("", "§e[NPC] §6King Yolkar§f: §rBring me a §9Goblin Egg §rof any type.")

            // §e[NPC] §6King Yolkar§f: §rThis egg will help me stomach my pain.
            message.contains("This egg will help me stomach my pain.") ->
                NucleusChatFilterRes("", "§e[NPC] §6King Yolkar§f: §2King's Scent§r applied.")

            else -> NucleusChatFilterRes("npc_king_yolkar")
        }
    }

    private fun blockKeepers(message: String): NucleusChatFilterRes? {
        if (!shouldBlock(CrystalNucleusMessageTypes.NPC_DIVAN_KEEPERS)) return null
        if (!message.startsWith("§e[NPC] §6Keeper of ")) return null

        genericKeeperMessage.matchMatcher(message) {
            lastKeeper = group("keepertype")
        }

        if (message.contains("You found all of the items!")) {
            return NucleusChatFilterRes("", "§e[NPC] §6Keeper of §6$lastKeeper§f: §rAll tools submitted.")
        }

        return NucleusChatFilterRes("npc_divan_keeper")
    }

    private fun shouldBlock(type: CrystalNucleusMessageTypes) = config.modifiedMessages.contains(type)
    private fun inNucleus() = LorenzUtils.skyBlockArea == "Crystal Nucleus"
    private fun isEnabled() = config.enabled && IslandType.CRYSTAL_HOLLOWS.isInIsland()
}
