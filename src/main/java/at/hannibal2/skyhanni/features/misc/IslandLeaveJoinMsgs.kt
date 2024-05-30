package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object IslandLeaveJoinMsgs {
    private val config get() = SkyHanniMod.feature.misc.leaveJoinMsgs

    private var players = mutableListOf<String>()

    private var updatedSinceWorldSwitch = false
    private var onKnownIsland = false

    private val patternGroup = RepoPattern.group("misc.islandleavejoinmsgs")
    private val rawPlayerPattern by patternGroup.pattern(
        "rawplayers",
        "§8\\[§[0-9a-f]\\d+§8\\] (?<player>§[0-9a-f]\\w+).*"
    )
    private val cleanPlayerPattern by patternGroup.pattern(
        "cleanplayers",
        "§8\\[§r§[0-9a-f]\\d+§r§8\\] §r(?<player>§[0-9a-f]\\w+).*"
    )
    private val offlinePlayerPattern by patternGroup.pattern(
        "offlineplayers",
        "(?<player>§[0-9a-f]\\w+)(?: §r§7\\(Offline [0-9Mdh+]+§r§7\\))?"
    )
    private val islandCategoryPattern by patternGroup.pattern(
        "islandcategory",
        "\\s+§r§b§lIsland"
    )
    private val guestCategoryPattern by patternGroup.pattern(
        "guestcategory",
        "\\s+§r§5§lGuests §r§f\\((?<totalGuests>\\d+)\\)"
    )
    private val playersCategoryPattern by patternGroup.pattern(
        "playerscategory",
        "\\s+§r§a§lPlayers §r§f\\((?<totalPlayers>\\d+)\\)"
    )

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!config.enabled) return
        val onPrivateWorld = IslandType.onPrivateWorld()
        val guesting = IslandType.onPrivateWorld(guesting = true)
        if (!(onPrivateWorld || (config.onPublicIslands && !guesting) || (config.guestLeaveJoinMsgs && guesting))) return

        var totalPlayers = -1

        val playersNew = mutableListOf<String>()

        var inIslandCategory = false
        val islandOwners = mutableListOf<String>()

        val islandMessage = when (LorenzUtils.skyBlockIsland) {
            IslandType.PRIVATE_ISLAND -> "your island."
            IslandType.GARDEN -> "your garden."
            else -> "the island."
        }
        val joinMessage = " §${if (config.leaveJoinColor) "a" else "e"}joined §e$islandMessage"
        val leaveMessage = " §${if (config.leaveJoinColor) "c" else "e"}left §e$islandMessage"

        for (line in event.tabList) {
            if (guesting) {
                islandCategoryPattern.matchMatcher(line) {
                    inIslandCategory = true
                }
                if (inIslandCategory) offlinePlayerPattern.matchMatcher(line) {
                    islandOwners.add(group("player"))
                }
                guestCategoryPattern.matchMatcher(line) {
                    inIslandCategory = false
                    totalPlayers = group("totalGuests").toInt()
                    for (player in islandOwners) {
                        if (isPlayerKnown(player.removeColor())) {
                            onKnownIsland = true
                            return@matchMatcher
                        }
                    }
                    onKnownIsland = false
                    return@matchMatcher
                }
            } else {
                playersCategoryPattern.matchMatcher(line) {
                    totalPlayers = group("totalPlayers").toInt()
                }
            }

            cleanPlayerPattern.matchMatcher(line) {
                val player = group("player")
                playersNew.add(player)
                if (guesting && inIslandCategory) { islandOwners.add(player) }
                if (players.contains(player)) return@matchMatcher
                players.add(player)         // !onPrivateIslandGarden because a vanilla message gets sent
                if (shouldSendMsg(player) && updatedSinceWorldSwitch && !onPrivateWorld) {
                    ChatUtils.chat("${player.cleanPlayerName(displayName = true)}$joinMessage")
                }
            }
            rawPlayerPattern.matchMatcher(line) {
                val player = group("player")
                if (playersNew.contains(player)) return@matchMatcher
                playersNew.add(player)
            }
        }

        if (players.isEmpty()) return

        // rather arbitrary multiplier to fix totalPlayers sometimes having a couple more than players
        if (players.size >= totalPlayers * 0.9 || players.size >= 37 && LorenzUtils.lastWorldSwitch.passedSince() > 2.seconds) {
            updatedSinceWorldSwitch = true
        }

        for (player in players.filter { !playersNew.contains(it) }) {
            if (shouldSendMsg(player)) ChatUtils.chat("$player$leaveMessage")
            players.remove(player)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        players.clear()
        updatedSinceWorldSwitch = false
        onKnownIsland = false
    }

    private fun shouldSendMsg(player: String): Boolean {
        val cleanPlayer = player.removeColor()

        if (LorenzUtils.getPlayerName() == cleanPlayer) return false
        if (!config.onlyKnownPeople) return true
        return isPlayerKnown(cleanPlayer) || (config.alwaysOnYourIsland && IslandType.onPrivateWorld()) || (config.alwaysOnKnownIslands && onKnownIsland)
    }

    private fun isPlayerKnown(player: String): Boolean = AdvancedPlayerList.getSocialIcon(player) != AdvancedPlayerList.SocialIcon.OTHER
}
