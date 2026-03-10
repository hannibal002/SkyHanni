package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.compacttablist.AdvancedPlayerListConfig.PlayerSortEntry
import at.hannibal2.skyhanni.data.FriendApi
import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.data.GuildApi
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.features.bingo.BingoApi
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.misc.ContributorManager
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.util.regex.Matcher
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object AdvancedPlayerList {

    val tabPlayerData = mutableMapOf<String, PlayerData>()

    private val config get() = SkyHanniMod.feature.gui.compactTabList.advancedPlayerList

    /**
     * REGEX-TEST: [290] Skirtwearer ꀾ♲
     * REGEX-TEST: [14] ColombianoGood Ⓑ
     * REGEX-TEST: [218] nightdives
     * REGEX-TEST: [281] [YOUTUBE] Remittal
     */
    private val levelPattern by RepoPattern.pattern(
        "misc.compacttablist.advanced.level.colorless",
        ".*\\[(?<level>[\\d,]+)](?: \\[\\w+\\])? (?<name>.*)",
    )

    private var playerData = mutableMapOf<Component, PlayerData>()

    fun createTabLine(component: Component, type: TabStringType) = playerData[component]?.let {
        TabLine(component, type, createCustomName(it))
    } ?: TabLine(component, type)

    // Todo split up into smaller functions
    @Suppress("CyclomaticComplexMethod")
    fun newSorting(original: List<Component>): List<Component> {
        if (KuudraApi.inKuudra) return original
        if (DungeonApi.inDungeon()) return original

        if (ignoreCustomTabList()) return original
        val newList = mutableListOf<Component>()
        val currentData = mutableMapOf<Component, PlayerData>()
        newList.add(original.first())

        var extraTitles = 0
        var i = 0

        for (component in original) {
            val line = component.formattedTextCompat()
            i++
            if (i == 1) continue
            if (line.isEmpty() || line.contains("Server Info")) break
            if (line == "               Info") break
            if (line.contains("Players")) {
                extraTitles++
                continue
            }
            val playerData: PlayerData? = levelPattern.matchMatcher(line) {
                val levelText = group("level")
                val removeColor = levelText.removeColor()
                val sbLevel = removeColor.formatIntOrNull() ?: return@matchMatcher null
                readPlayerData(sbLevel, levelText, line)
            }
            playerData?.let {
                val name = it.name
                if (name != "?") {
                    tabPlayerData[name] = it
                }
                currentData[component] = it
            }
        }
        playerData = currentData
        val prepare = currentData.entries

        val sorted = when (config.playerSortOrder) {
            // SB Level
            PlayerSortEntry.SB_LEVEL -> prepare.sortedBy { -(it.value.sbLevel) }

            // Name (Abc)
            PlayerSortEntry.NAME -> prepare.sortedBy {
                it.value.name.lowercase().replace("_", "")
            }

            // Ironman/Bingo
            PlayerSortEntry.PROFILE_TYPE -> prepare.sortedBy {
                -if (it.value.ironman) 10 else it.value.bingoLevel ?: -1
            }

            // Party/Friends/Guild First
            PlayerSortEntry.SOCIAL_STATUS -> prepare.sortedBy { -getSocialIcon(it.value.name).score }

            // Random
            PlayerSortEntry.RANDOM -> prepare.sortedBy { getRandomOrder(it.value.name) }

            // Rank (Default)
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

        val rest = original.drop(playerData.size + extraTitles + 1)
        newList.addAll(rest)
        return newList
    }

    private fun Matcher.readPlayerData(
        sbLevel: Int,
        levelText: String,
        line: String,
    ): PlayerData {
        val playerData = PlayerData(sbLevel)
        var index = 0
        val fullName = group("name")
        if (fullName.contains("[")) index++
        val name = fullName.split(" ")
        val coloredName = name[index]
        if (index == 1) {
            playerData.coloredName = name[0] + " " + coloredName
        } else {
            playerData.coloredName = coloredName
        }
        playerData.name = coloredName.removeColor()
        playerData.levelText = levelText
        index++
        if (name.size > index) {
            var nameSuffix = name.drop(index).joinToString(" ")
            if (nameSuffix.contains("♲")) {
                playerData.ironman = true
            } else {
                playerData.bingoLevel = BingoApi.getRank(line)
            }
            if (IslandType.CRIMSON_ISLE.isCurrent()) {
                playerData.faction = if (line.contains("§c⚒")) {
                    nameSuffix = nameSuffix.replace("§c⚒", "")
                    CrimsonIsleFaction.BARBARIAN
                } else if (line.contains("§5ቾ")) {
                    nameSuffix = nameSuffix.replace("§5ቾ", "")
                    CrimsonIsleFaction.MAGE
                } else {
                    CrimsonIsleFaction.NONE
                }
            }
            playerData.nameSuffix = nameSuffix
        } else {
            playerData.nameSuffix = ""
        }
        return playerData
    }

    fun ignoreCustomTabList(): Boolean {
        val denyKeyPressed = SkyHanniMod.feature.dev.debug.bypassAdvancedPlayerTabList.isKeyHeld()
        return GlobalRender.renderDisabled || denyKeyPressed
    }

    private fun createCustomName(data: PlayerData): Component {

        val playerName = if (config.useLevelColorForName) {
            val c = data.levelText[3]
            "§$c" + data.name
        } else if (config.hideRankColor) "§b" + data.name else data.coloredName

        val level = if (!config.hideLevel) {
            if (config.hideLevelBrackets) data.levelText else "§8[${data.levelText}§8]"
        } else ""

        val suffix = if (config.hideEmblem) {
            if (data.ironman) Component.literal("§7♲") else data.bingoLevel?.let {
                Component.literal(BingoApi.getBingoIcon(if (config.showBingoRankNumber) it else -1))
            } ?: Component.empty()
        } else Component.literal(data.nameSuffix)

        if (config.markSpecialPersons) {
            suffix.append(" ${getSocialIcon(data.name).icon()}")
        }

        if (SkyHanniMod.feature.dev.fancyContributors) {
            Minecraft.getInstance().connection?.getPlayerInfo(data.name)?.let { playerInfo ->
                ContributorManager.getSuffix(playerInfo.profile.id)?.let {
                    suffix.append(" ").append(it)
                }
            }
        }

        if (IslandType.CRIMSON_ISLE.isCurrent() && !config.hideFactions) {
            suffix.append(data.faction.icon.orEmpty())
        }

        // todo: level and player name should also really be components
        return Component.literal("$level $playerName ").append(suffix)
    }

    private val randomOrderCache = TimeLimitedCache<String, Int>(20.minutes)

    private fun getRandomOrder(name: String) = randomOrderCache.getOrPut(name) {
        (Random.nextDouble() * 500).toInt()
    }

    private fun getSocialIcon(name: String) = when {
        PlayerUtils.getName() == name -> SocialIcon.ME
        MarkedPlayerManager.isMarkedPlayer(name) -> SocialIcon.MARKED
        PartyApi.partyMembers.contains(name) -> SocialIcon.PARTY
        FriendApi.getAllFriends().any { it.name.equals(name, ignoreCase = true) } -> SocialIcon.FRIEND
        GuildApi.isInGuild(name) -> SocialIcon.GUILD
        else -> SocialIcon.OTHER
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

    enum class CrimsonIsleFaction(val icon: String?) {
        BARBARIAN(" §c⚒"),
        MAGE(" §5ቾ"),
        NONE(null)
    }

    enum class SocialIcon(val icon: () -> String, val score: Int) {
        ME("", 10),
        MARKED({ "${MarkedPlayerManager.config.chatColor.getChatColor()}§lMARKED" }, 8),
        PARTY("§9§lP", 5),
        FRIEND("§d§lF", 4),
        GUILD("§2§lG", 3),
        OTHER("", 1)
        ;

        constructor(icon: String, score: Int) : this({ icon }, score)
    }
}
