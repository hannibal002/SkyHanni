package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
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
import at.hannibal2.skyhanni.utils.ComponentMatcher
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.merge
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
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
     * REGEX-FAIL: SB Level§r§f: §r§8[§r§6419§r§8] §r§b8§r§3/§r§b100 XP
     */
    private val levelPattern by RepoPattern.pattern(
        "misc.compacttablist.advanced.level.colorless",
        "^(?!SB Level).*\\[(?<level>(?:§.)*[\\d,]+)(?:§.)*] (?<name>.*)",
    )

    internal var playerData = mutableMapOf<Component, PlayerData>()

    fun createTabLine(component: Component, type: TabStringType) = playerData[component]?.let {
        TabLine(component, type, it.createCustomName())
    } ?: TabLine(component, type)


    @HandleEvent
    private fun onConfigLoad() {
        ConditionalUtils.onToggle(
            config.hideEmblem,
            config.hideFactions,
            config.hideLevel,
            config.hideLevelBrackets,
            config.hideRankColor,
            config.markSpecialPersons,
            config.reverseSort,
            config.playerSortOrder,
            config.showBingoRankNumber,
            config.useLevelColorForName,
            SkyHanniMod.feature.dev.fancyContributors
        ) {
            playerData.clear()
            tabPlayerData.clear()
        }
    }

    // Todo split up into smaller functions
    @Suppress("CyclomaticComplexMethod")
    fun newSorting(original: List<Component>): List<Component> {
        if (KuudraApi.inKuudra) return original
        if (DungeonApi.inDungeon()) return original

        if (ignoreCustomTabList()) return original

        val newList = mutableListOf<Component>()
        val currentData = mutableMapOf<Component, PlayerData>()

        newList += original.first()

        var extraTitles = 0

        for ((index, component) in original.withIndex()) {
            if (index == 0) continue

            val span = component.intoSpan()

            // We only use plain text for detecting section headers.
            // Player parsing itself stays entirely on ComponentSpan.
            val plain = span.getText().removeColor()

            if (plain.isEmpty() || plain.contains("Server Info")) break
            if (plain == "               Info") break

            if (plain.contains("Players")) {
                extraTitles++
                continue
            }

            val data = levelPattern.matchStyledMatcher(span) {
                val levelText = groupOrThrow("level")
                val sbLevel = levelText.getText().removeColor().formatIntOrNull()
                    ?: return@matchStyledMatcher null

                readPlayerData(sbLevel, levelText, component)
            }

            data?.let {
                if (it.name != "?") {
                    tabPlayerData[it.name] = it
                }
                currentData[component] = it
            }
        }

        playerData = currentData

        val sorted = when (config.playerSortOrder.get()) {
            PlayerSortEntry.SB_LEVEL ->
                currentData.entries.sortedByDescending { it.value.sbLevel }

            PlayerSortEntry.NAME ->
                currentData.entries.sortedBy {
                    it.value.name.lowercase().replace("_", "")
                }

            PlayerSortEntry.PROFILE_TYPE ->
                currentData.entries.sortedByDescending {
                    if (it.value.ironman) 10 else it.value.bingoLevel ?: -1
                }

            PlayerSortEntry.SOCIAL_STATUS ->
                currentData.entries.sortedByDescending {
                    getSocialIcon(it.value.name).score
                }

            PlayerSortEntry.RANDOM ->
                currentData.entries.sortedBy {
                    getRandomOrder(it.value.name)
                }

            else ->
                currentData.entries.toList()
        }

        val newPlayerList = sorted.map { it.key }.toMutableList()

        if (config.reverseSort.get()) {
            newPlayerList.reverse()
        }

        if (extraTitles > 0 && newPlayerList.size >= 19) {
            newPlayerList.add(19, original.first())
        }

        newList += newPlayerList
        newList += original.drop(currentData.size + extraTitles + 1)

        return newList
    }

    private fun ComponentMatcher.readPlayerData(
        sbLevel: Int,
        levelText: ComponentSpan,
        line: Component,
    ): PlayerData = PlayerData(sbLevel).apply {
        val fullName = groupOrThrow("name")

        val words = fullName.split(' ')
        require(words.isNotEmpty())

        var index = 0
        if (words.first().getText().startsWith("[")) {
            index = 1
        }

        val coloredName = words[index]
        this.coloredName = if (index == 1) {
            words[0] + " ".asComponent().intoSpan() + coloredName
        } else {
            coloredName
        }

        this.name = coloredName.getText().removeColor()
        this.levelText = levelText

        index++

        if (words.size > index) {
            this.nameSuffix = words
                .subList(index, words.size)
                .reduce { left, right ->
                    left + " ".asComponent().intoSpan() + right
                }

            if (nameSuffix.getText().contains("♲")) {
                ironman = true
            } else {
                bingoLevel = BingoApi.getRank(line.string)
            }

            if (IslandType.CRIMSON_ISLE.isInIsland()) {
                CrimsonIsleFaction.entries.firstOrNull { it.isLine(line.string) }?.let {
                    faction = it
                    nameSuffix = nameSuffix.removeSuffix(it.icon.orEmpty())
                    nameSuffix = nameSuffix.removeSuffix(it.symbol.orEmpty())
                }
            }
        } else {
            nameSuffix = ComponentSpan.empty()
        }
    }

    fun ignoreCustomTabList(): Boolean {
        val denyKeyPressed = SkyHanniMod.feature.dev.debug.bypassAdvancedPlayerTabList.isKeyHeld()
        return GlobalRender.renderDisabled || denyKeyPressed
    }

    private fun PlayerData.createCustomName(): Component = buildList<Component> {
        fun MutableList<Component>.add(string: String?) {
            string?.takeIfNotEmpty()?.let {
                add(it.asComponent())
            }
        }
        fun MutableList<Component>.add(span: Component?) {
            span?.let {
                add(it)
            }
        }

        if (!config.hideLevel.get()) {
            val level = if (config.hideLevelBrackets.get()) levelText.intoComponent() else {
                componentBuilder {
                    appendWithColor("[", ChatFormatting.DARK_GRAY)
                    append(levelText.intoComponent())
                    appendWithColor("]", ChatFormatting.DARK_GRAY)
                }
            }
            add(level)
        }

        val playerName: Component = if (config.useLevelColorForName.get()) {
            val style = levelText.sampleStyleAtStart()
            name.asComponent().withStyle(style)
        } else if (config.hideRankColor.get()) {
            componentBuilder {
                appendWithColor(name, ChatFormatting.AQUA)
            }
        } else {
            coloredName.intoComponent()
        }
        add(playerName)

        if (config.hideEmblem.get()) {
            if (ironman) {
                add(
                    componentBuilder { appendWithColor("♲", ChatFormatting.GRAY) }
                )
            } else {
                bingoLevel?.let {
                    add(BingoApi.getBingoIcon(if (config.showBingoRankNumber.get()) it else -1))
                }
            }
        } else {
            add(nameSuffix.intoComponent())
        }

        if (IslandType.CRIMSON_ISLE.isInIsland() && !config.hideFactions.get()) {
            add(faction.icon)
        }

        if (config.markSpecialPersons.get()) {
            add(getSocialIcon(name).icon())
        }

        if (SkyHanniMod.feature.dev.fancyContributors.get()) {
            Minecraft.getInstance().connection?.getPlayerInfo(name)?.let { playerInfo ->
                ContributorManager.getSuffix(playerInfo.profile.id)?.let {
                    add(it)
                }
            }
        }

    }.merge()

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
        var coloredName: ComponentSpan = "?".asComponent().intoSpan()
        var nameSuffix: ComponentSpan = "?".asComponent().intoSpan()
        var levelText: ComponentSpan = "?".asComponent().intoSpan()
        var ironman: Boolean = false
        var bingoLevel: Int? = null
        var faction: CrimsonIsleFaction = CrimsonIsleFaction.NONE

        override fun toString(): String {
            return "PlayerData(name='$name', " +
                "coloredName=${coloredName.intoComponent()}, " +
                "nameSuffix=${nameSuffix.intoComponent()}, " +
                "levelText=${levelText.intoComponent()}, " +
                "ironman=$ironman, " +
                "bingoLevel=$bingoLevel, " +
                "faction=${faction.name})"
        }
    }

    enum class CrimsonIsleFaction(color: String?, val symbol: String?) {
        BARBARIAN("§c", "⚒"),
        MAGE("§5", "ቾ"),
        NONE(null, null)
        ;

        val icon: String? = color?.let { "$it$symbol" }

        fun isLine(line: String): Boolean {
            return line.contains(this.symbol ?: return false)
        }
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
