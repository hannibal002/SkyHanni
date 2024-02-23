package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.GuildAPI
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object IslandLeaveJoinMsgs {
    private val config get() = SkyHanniMod.feature.misc.leaveJoinMsgs

    private var players = mutableListOf<String>()
    private var rawPlayersNew = mutableListOf<String>()
    private var cleanPlayersNew = mutableListOf<String>()

    private var updatedSinceWorldSwitch = false
    private var onKnownIsland = false

    private val rawPlayerPattern by RepoPattern.pattern(
        "misc.islandleavejoinmsgs.rawplayers", "^§8\\[§\\w\\d+§8\\] (?<player>§\\w+).*$"
    )
    private val cleanPlayerPattern by RepoPattern.pattern(
        "misc.islandleavejoinmsgs.cleanplayers", "^§8\\[§r§\\w\\d+§r§8\\] §r(?<player>§\\w+).*$"
    )
    private val offlinePlayerPattern by RepoPattern.pattern(
        "misc.islandleavejoinmsgs.offlineplayers", "^§[0-9a-f]\\w+(?: §r§7\\(Offline [0-9dh+]+§r§7\\))?\$"
    )
    private val islandCategoryPattern by RepoPattern.pattern(
        "misc.islandleavejoinmsgs.islandcategory", "^\\s+§r§b§lIsland$"
    )
    private val guestCategoryPattern by RepoPattern.pattern(
        "misc.islandleavejoinmsgs.guestcategory", "^\\s+§r§5§lGuests §r§f\\(\\d+\\)$"
    )

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!config.enabled) return
        val onPrivateIslandGarden = onPrivateIsland()
        val guesting = onPrivateIsland(true)
        if (!(onPrivateIslandGarden || (config.onPublicIslands && !guesting) || (config.guestLeaveJoinMsgs && guesting))) return

        rawPlayersNew.clear()
        cleanPlayersNew.clear()

        var inIslandCategory = false
        val islandOwners = mutableListOf<String>()

        val joinMessage = " §" + if (config.leaveJoinColor) { "a" } else { "e" } + "joined §ethe island."
        val leaveMessage = " §" + if (config.leaveJoinColor) { "c" } else { "e" } + "left §ethe island."

        for (line in event.tabList) {
            if (guesting && !onKnownIsland) {
                islandCategoryPattern.matchMatcher(line) {
                    inIslandCategory = true
                }
                if (inIslandCategory) offlinePlayerPattern.matchMatcher(line) {
                    islandOwners.add(line)
                }
                guestCategoryPattern.matchMatcher(line) {
                    inIslandCategory = false
                    for (player in islandOwners) {
                        if (isPlayerKnown(player.removeColor())) {
                            onKnownIsland = true
                            return@matchMatcher
                        }
                    }
                    return@matchMatcher
                }
            }

            cleanPlayerPattern.matchMatcher(line) {
                val player = group("player")
                cleanPlayersNew.add(player)
                if (guesting && inIslandCategory) {islandOwners.add(player)}
                if (players.contains(player)) {
                    return@matchMatcher
                }
                players.add(player)            // !onPrivateIslandGarden because a vanilla message gets sent
                if (shouldSendMsg(player) && updatedSinceWorldSwitch && !onPrivateIslandGarden) {
                    ChatUtils.chat(player + joinMessage)
                }
            }
            rawPlayerPattern.matchMatcher(line) {
                rawPlayersNew.add(group("player"))
            }
        }

        if (players.size > 1) {
            updatedSinceWorldSwitch = true
        }

        if (players.isEmpty()) return

        for ((index, player) in players.withIndex().reversed()) {
            if (!rawPlayersNew.contains(player) && !cleanPlayersNew.contains(player)) {
                if (shouldSendMsg(player)) ChatUtils.chat(player + leaveMessage)
                players.removeAt(index)
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        clearPlayers()
    }

    fun clearPlayers() {
        players.clear()
        updatedSinceWorldSwitch = false
        onKnownIsland = false
    }

    private fun shouldSendMsg(player: String): Boolean {
        val cleanPlayer = player.removeColor()
        return (
            LorenzUtils.getPlayerName() != cleanPlayer &&
                if (config.onlyKnownPeople) isPlayerKnown(cleanPlayer) ||
                    (config.alwaysOnYourIsland && onPrivateIsland()) ||
                    (config.alwaysOnKnownIslands && onKnownIsland)
                else true
            )
    }

    private fun onPrivateIsland(guesting: Boolean = false) =
        if (guesting) {
            LorenzUtils.skyBlockIsland == IslandType.PRIVATE_ISLAND_GUEST || LorenzUtils.skyBlockIsland == IslandType.GARDEN_GUEST
        } else LorenzUtils.skyBlockIsland == IslandType.PRIVATE_ISLAND || LorenzUtils.skyBlockIsland == IslandType.GARDEN

    private fun isPlayerKnown(player: String): Boolean {
        return (
            FriendAPI.getAllFriends().any { it.name.contains(player) } ||
                GuildAPI.isInGuild(player) ||
                PartyAPI.partyMembers.contains(player)
            )
    }
}
