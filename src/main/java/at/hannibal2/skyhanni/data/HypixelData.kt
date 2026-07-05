package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesRepoManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.hypixelapi.HypixelLocationApi
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates
import at.hannibal2.skyhanni.data.repo.SkyHanniRepoManager
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.skyblock.ScoreboardAreaChangeEvent
import at.hannibal2.skyhanni.features.bingo.BingoApi
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.allMatchesComponent
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.getSidebarObjective
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * This class handles "am I on hypixel", and similar states.
 * For "am I in SkyBlock" and "what SkyBlock island am I on" checks see [HypixelLocationApi].
 */
@SkyHanniModule
object HypixelData {

    private val patternGroup = RepoPattern.group("data.hypixeldata")

    /**
     * REGEX-TEST: [441] Throwpo ♲
     */
    private val playerAmountOnIslandPattern by patternGroup.pattern(
        "playeramount.onisland-nocolor",
        "^\\[\\w+] .*",
    )

    /**
     * WRAPPED-REGEX-TEST: " §a✌ §7(§a11§7/20)"
     * WRAPPED-REGEX-TEST: " §a✌ §7(§e1/1§7)"
     */
    private val scoreboardVisitingAmountPattern by patternGroup.pattern(
        "scoreboard.visiting.amount",
        "\\s+§.✌ §.\\(§.(?<currentamount>\\d+)(?:§.)?/(?<maxamount>\\d+)(?:§.)?\\)",
    )

    /**
     * WRAPPED-REGEX-TEST: " §7⏣ §bVillage"
     * WRAPPED-REGEX-TEST: " §5ф §dWizard Tower"
     */
    private val skyblockAreaPattern by patternGroup.pattern(
        "skyblock.area",
        "\\s*§(?<symbol>7⏣|5ф) §(?<color>.)(?<area>.*)",
    )

    val connectedToHypixel get() = HypixelLocationApi.inHypixel

    val hypixelLive get() = connectedToHypixel && !HypixelLocationApi.inAlpha
    val hypixelAlpha get() = connectedToHypixel && HypixelLocationApi.inAlpha
    val inLobby get() = HypixelLocationApi.inLobby
    val inLimbo get() = HypixelLocationApi.inLimbo

    val serverId get() = HypixelLocationApi.serverId

    // Ironman, Stranded and Bingo
    var noTrade = false

    var ironman = false
    var stranded = false
    var bingo = false

    var profileName = ""
    var joinedWorld = SimpleTimeMark.farPast()

    var skyBlockArea: String? = null
    var skyBlockAreaWithSymbol: String? = null

    var playerAmountOnIsland = 0

    private val progressCategory = ChatProgressUpdates.category("Hypixel Data")

    val server get() = HypixelLocationApi.serverName
    val gameType get() = HypixelLocationApi.serverType?.name().orEmpty()
    val lobbyName get() = HypixelLocationApi.lobbyName.orEmpty()
    val lobbyType get() = HypixelLocationApi.lobbyType.orEmpty()
    val mode get() = HypixelLocationApi.mode.orEmpty()
    val map get() = HypixelLocationApi.map.orEmpty()

    fun getPlayersOnCurrentServer(): Int {
        var amount = 0
        val playerWidgetList = mutableListOf(
            TabWidget.PLAYER_LIST,
            TabWidget.GUESTS,
        )

        if (DungeonApi.inDungeon()) {
            playerWidgetList.add(TabWidget.DUNGEON_PARTY)
        }

        out@ for (widget in playerWidgetList) {
            for (component in widget.lines) {
                widget.pattern.matchMatcher(component) {
                    amount += group("amount").toInt()
                    continue@out
                }
            }
        }

        if (!IslandTypeTag.PERSONAL_ISLAND.isInIsland()) {
            playerAmountOnIsland = 0
        }

        return amount + playerAmountOnIsland
    }

    fun getMaxPlayersForCurrentServer(): Int {
        scoreboardVisitingAmountPattern.firstMatcher(ScoreboardData.sidebarLinesFormatted) {
            return group("maxamount").toInt() + playerAmountOnIsland
        }
        if (serverId?.startsWith("mega") == true) {
            return IslandType.maxPlayersMega
        }
        return HypixelLocationApi.island.islandData?.maxPlayers ?: IslandType.maxPlayers
    }

    @HandleEvent
    fun onWorldChange() {
        joinedWorld = SimpleTimeMark.now()
        skyBlockArea = null
        skyBlockAreaWithSymbol = null
    }

    @HandleEvent(ClientDisconnectEvent::class)
    fun onDisconnect() {
        skyBlockArea = null
        skyBlockAreaWithSymbol = null
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!SkyBlockUtils.onHypixel) return

        val message = event.cleanMessage.lowercase()
        if (message.startsWith("your profile was changed to:")) {
            val newProfile = message.replace("your profile was changed to:", "").replace("(co-op)", "").trim()
            if (profileName == newProfile) return
            profileName = newProfile
            ProfileJoinEvent(newProfile).post()
        }
        if (message.startsWith("you are playing on profile:")) {
            val newProfile = message.replace("you are playing on profile:", "").replace("(co-op)", "").trim()
            ProfileStorageData.profileJoinMessage()
            if (profileName == newProfile) return
            profileName = newProfile
            ProfileJoinEvent(newProfile).post()
        }
    }

    private fun checkProfile() {
        TabWidget.PROFILE.matchMatcherFirstLine {
            var newProfile = group("profile").lowercase().trim()
            // Hypixel shows the profile name reversed while in the Rift
            if (RiftApi.inRift()) newProfile = newProfile.reversed()
            if (profileName == newProfile) return
            profileName = newProfile
            ProfileJoinEvent(newProfile).post()
        }
    }

    // TODO rewrite everything in here
    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (SkyBlockUtils.onHypixel && SkyBlockUtils.inSkyBlock) {
            loop@ for (line in ScoreboardData.sidebarLinesFormatted) {
                skyblockAreaPattern.matchMatcher(line) {
                    val originalLocation = group("area").removeColor()
                    val area = LocationFixData.fixLocation(HypixelLocationApi.island) ?: originalLocation
                    skyBlockAreaWithSymbol = line.trim()
                    if (area != skyBlockArea) {
                        val previousArea = skyBlockArea
                        skyBlockArea = area
                        ScoreboardAreaChangeEvent(area, previousArea).post()
                    }
                    break@loop
                }
            }

            checkProfileName()
        }

        if (!SkyBlockUtils.onHypixel) return

        if (!event.isMod(5)) return

        if (HypixelLocationApi.inSkyblock) {
            checkSpecialModes()
        }
    }

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        when (event.widget) {
            TabWidget.AREA -> checkIsland(event)
            TabWidget.PROFILE -> checkProfile()
            TabWidget.COOP -> countPlayersOnIsland(event)
            TabWidget.ISLAND -> countPlayersOnIsland(event)
            else -> Unit
        }
    }

    private fun checkProfileName() {
        if (profileName.isNotEmpty()) return

        TabWidget.PROFILE.matchMatcherFirstLine {
            profileName = group("profile").lowercase().trim()
            ProfileJoinEvent(profileName).post()
        }
    }

    private fun checkSpecialModes() {
        val scoreboardTitle = getScoreboardTitle() ?: return
        if (scoreboardTitle.contains("GUEST")) return
        ironman = false
        stranded = false
        bingo = false



        if (scoreboardTitle.contains("♲")) ironman = true
        else if (scoreboardTitle.contains("☀")) stranded = true

        // remove once update is on main
        // make sure to keep the bingo part when you remove it
        for (line in ScoreboardData.sidebarLinesFormatted) {
            if (BingoApi.getRankFromScoreboard(line) != null) {
                bingo = true
            }
            when (line) {
                " §7♲ §7Ironman" -> {
                    ironman = true
                }

                " §a☀ §aStranded" -> {
                    stranded = true
                }
            }
        }

        noTrade = ironman || stranded || bingo
    }

    private var tabListDataDirty = false

    @HandleEvent(IslandJoinEvent::class)
    fun onIslandJoin() {
        tabListDataDirty = true
    }

    private fun checkIsland(event: WidgetUpdateEvent) {
        TabListData.fullyLoaded = !event.isClear()

        if (HypixelLocationApi.inSkyblock && tabListDataDirty) {
            tabListDataDirty = false
            if (TabListData.fullyLoaded) {
                TabWidget.reSendEvents()
            }
        }
    }

    fun getScoreboardTitle(): String? {
        val world = MinecraftCompat.localWorldOrNull ?: return null

        val objective = world.scoreboard.getSidebarObjective() ?: return null
        val displayName = objective.displayName.formattedTextCompat()
        return displayName
    }

    private fun countPlayersOnIsland(event: WidgetUpdateEvent) {
        if (event.isClear()) return
        playerAmountOnIsland = playerAmountOnIslandPattern.allMatchesComponent(event.lines).size
    }

    @HandleEvent(HypixelJoinEvent::class)
    fun onHypixelJoin() {
        CoroutineSettings("hypixel join repo update").launchCoroutine {
            val progress = progressCategory.start("hypixel join repo update check")
            SkyHanniRepoManager.displayRepoStatus(progress, joinEvent = true)
            EnoughUpdatesRepoManager.displayRepoStatus(progress, joinEvent = true)
            progress.end("done with checking both repos")
        }
    }
}
