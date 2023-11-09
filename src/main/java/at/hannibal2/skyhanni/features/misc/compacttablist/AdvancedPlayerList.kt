package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.BingoAPI
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.GuildAPI
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.jsonobjects.ContributorListJson
import com.google.common.cache.CacheBuilder
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object AdvancedPlayerList {
    private val config get() = SkyHanniMod.feature.misc.compactTabList.advancedPlayerList

    // TODO USE SH-REPO
    private val pattern = ".*\\[(?<level>.*)] §r(?<name>.*)".toPattern()

    private var playerDatas = mutableMapOf<String, PlayerData>()

    fun createTabLine(text: String, type: TabStringType) = playerDatas[text]?.let {
        TabLine(text, type, createCustomName(it))
    } ?: TabLine(text, type)

    fun newSorting(original: List<String>): List<String> {
        if (LorenzUtils.inKuudraFight) return original
        if (LorenzUtils.inDungeons) return original

        if (ignoreCustomTabList()) return original
        val newList = mutableListOf<String>()
        val currentData = mutableMapOf<String, PlayerData>()
        newList.add(original.first())

        var extraTitles = 0
        var i = 0

        for (line in original) {
            i++
            if (i == 1) continue
            if (line.isEmpty() || line.contains("Server Info")) break
            if (line.contains("§r§a§lPlayers")) {
                extraTitles++
                continue
            }
            pattern.matchMatcher(line) {
                val levelText = group("level")
                val removeColor = levelText.removeColor()
                try {
                    val playerData = PlayerData(removeColor.toInt())
                    currentData[line] = playerData

                    var index = 0
                    val fullName = group("name")
                    if (fullName.contains("[")) index++
                    val name = fullName.split(" ")
                    val coloredName = name[index]
                    if (index == 1) {
                        playerData.coloredName = name[0] + coloredName
                    } else {
                        playerData.coloredName = coloredName
                    }
                    playerData.name = coloredName.removeColor()
                    playerData.levelText = levelText
                    index++
                    if (name.size > index) {
                        val nameSuffix = name.drop(index).joinToString(" ")
                        playerData.nameSuffix = nameSuffix
                        if (nameSuffix.contains("♲")) {
                            playerData.ironman = true
                        } else {
                            playerData.bingoLevel = BingoAPI.getRank(line)
                        }
                        if (IslandType.CRIMSON_ISLE.isInIsland()) {
                            playerData.faction = if (line.contains("§c⚒")) {
                                CrimsonIsleFaction.BARBARIAN
                            } else if (line.contains("§5ቾ")) {
                                CrimsonIsleFaction.MAGE
                            } else {
                                CrimsonIsleFaction.NONE
                            }
                        }
                    } else {
                        playerData.nameSuffix = ""
                    }

                } catch (e: NumberFormatException) {
                    val message = "Special user (youtube or admin?): '$line'"
                    LorenzUtils.debug(message)
                    println(message)
                }
            }
        }
        playerDatas = currentData
        val prepare = currentData.entries

        val sorted = when (config.playerSortOrder) {

            // Rank (Default)
            1 -> prepare.sortedBy { -(it.value.sbLevel) }

            // Name (Abc)
            2 -> prepare.sortedBy { it.value.name.lowercase().replace("_", "") }

            // Ironman/Bingo
            3 -> prepare.sortedBy { -if (it.value.ironman) 10 else it.value.bingoLevel ?: -1 }

            // Party/Friends/Guild First
            4 -> prepare.sortedBy { -socialScore(it.value.name) }

            // Random
            5 -> prepare.sortedBy { getRandomOrder(it.value.name) }

            else -> prepare
        }

        var newPlayerList = sorted.map { it.key }.toMutableList()
        if (config.reverseSort) {
            newPlayerList = newPlayerList.reversed().toMutableList()
        }
        if (extraTitles > 0 && newPlayerList.size >= 19) {
            newPlayerList.add(19, original.first())
        }
        newList.addAll(newPlayerList)

        val rest = original.drop(playerDatas.size + extraTitles + 1)
        newList.addAll(rest)
        return newList
    }

    fun ignoreCustomTabList(): Boolean {
        val denyKeyPressed = SkyHanniMod.feature.dev.debug.enabled && KeyboardManager.isControlKeyDown()
        return denyKeyPressed || !SkyHanniDebugsAndTests.globalRender
    }

    private var contributors: List<String> = emptyList()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        contributors = event.getConstant<ContributorListJson>("ContributorList").usernames
    }

    private fun createCustomName(data: PlayerData): String {
        val playerName = if (config.useLevelColorForName) {
            val c = data.levelText[3]
            "§$c" + data.name
        } else if (config.hideRankColor) "§b" + data.name else data.coloredName

        val level = if (!config.hideLevel) {
            if (config.hideLevelBrackets) data.levelText else "§8[${data.levelText}§8]"
        } else ""

        var suffix = if (config.hideEmblem) {
            if (data.ironman) "§7♲" else data.bingoLevel?.let { getBingoIcon(it) } ?: ""
        } else data.nameSuffix

        if (config.markSpecialPersons) {
            val score = socialScore(data.name)
            suffix += " " + getSocialScoreIcon(score)
        }
        if (config.markSkyHanniContributors && data.name in contributors) {
            suffix += "§c:O"
        }

        if (IslandType.CRIMSON_ISLE.isInIsland() && !config.hideFactions) {
            suffix += data.faction.icon
        }

        return "$level $playerName ${suffix.trim()}"
    }

    private var randomOrderCache =
        CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES).build<String, Int>()

    private fun getRandomOrder(name: String): Int {
        val saved = randomOrderCache.getIfPresent(name)
        if (saved != null) {
            return saved
        }
        val r = (Random.nextDouble() * 500).toInt()
        randomOrderCache.put(name, r)
        return r
    }

    private fun socialScore(name: String) = when {
        LorenzUtils.getPlayerName() == name -> 10
        MarkedPlayerManager.isMarkedPlayer(name) -> 8
        PartyAPI.partyMembers.contains(name) -> 5
        FriendAPI.getAllFriends().any { it.name.contains(name) } -> 4
        GuildAPI.isInGuild(name) -> 3

        else -> 1
    }

    private fun getSocialScoreIcon(score: Int) = when (score) {
//        10 -> "§c§lME"
        10 -> ""
        8 -> "§e§lMARKED"
        5 -> "§9§lP"
        4 -> "§d§lF"
        3 -> "§2§lG"

        else -> ""
    }

    private fun getBingoIcon(rank: Int): String {
        val rankIcon = BingoAPI.getIcon(rank) ?: ""
        return if (config.showBingoRankNumber && rank != -1) {
            "$rankIcon $rank"
        } else {
            rankIcon
        }
    }

    class PlayerData(val sbLevel: Int) {
        var name: String = "?"
        var coloredName: String = "?"
        var nameSuffix: String = "?"
        var levelText: String = "?"
        var ironman: Boolean = false
        var bingoLevel: Int? = null
        var faction: CrimsonIsleFaction = CrimsonIsleFaction.NONE
    }

    enum class CrimsonIsleFaction(val icon: String) {
        BARBARIAN(" §c⚒"),
        MAGE(" §5ቾ"),
        NONE("")
    }
}
