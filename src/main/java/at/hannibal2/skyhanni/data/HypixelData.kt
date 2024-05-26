package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager.Companion.gson
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchFirst
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonObject
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

class HypixelData {

    private val patternGroup = RepoPattern.group("data.hypixeldata")
    private val islandNamePattern by patternGroup.pattern(
        "islandname",
        "(?:§.)*(Area|Dungeon): (?:§.)*(?<island>.*)"
    )

    private var lastLocRaw = SimpleTimeMark.farPast()

    companion object {
        private val patternGroup = RepoPattern.group("data.hypixeldata")
        private val serverIdScoreboardPattern by patternGroup.pattern(
            "serverid.scoreboard",
            "§7\\d+/\\d+/\\d+ §8(?<servertype>[mM])(?<serverid>\\S+).*"
        )
        private val serverIdTablistPattern by patternGroup.pattern(
            "serverid.tablist",
            " Server: §r§8(?<serverid>\\S+)"
        )
        private val lobbyTypePattern by patternGroup.pattern(
            "lobbytype",
            "(?<lobbyType>.*lobby)\\d+"
        )
        private val playerAmountPattern by patternGroup.pattern(
            "playeramount",
            "^\\s*(?:§.)+Players (?:§.)+\\((?<amount>\\d+)\\)\\s*$"
        )
        private val playerAmountCoopPattern by patternGroup.pattern(
            "playeramount.coop",
            "^\\s*(?:§.)*Coop (?:§.)*\\((?<amount>\\d+)\\)\\s*$"
        )
        private val playerAmountGuestingPattern by patternGroup.pattern(
            "playeramount.guesting",
            "^\\s*(?:§.)*Guests (?:§.)*\\((?<amount>\\d+)\\)\\s*$"
        )
        /**
         * REGEX-TEST:           §r§b§lParty §r§f(4)
         */
        private val dungeonPartyAmountPattern by patternGroup.pattern(
            "playeramount.dungeonparty",
            "^\\s*(?:§.)+Party (?:§.)+\\((?<amount>\\d+)\\)\\s*$"
        )
        private val soloProfileAmountPattern by patternGroup.pattern(
            "solo.profile.amount",
            "^\\s*(?:§.)*Island\\s*$"
        )
        private val scoreboardVisitingAmoutPattern by patternGroup.pattern(
            "scoreboard.visiting.amount",
            "\\s+§.✌ §.\\(§.(?<currentamount>\\d+)§./(?<maxamount>\\d+)\\)"
        )
        private val guestPattern by patternGroup.pattern(
            "guesting.scoreboard",
            "SKYBLOCK GUEST"
        )
        private val scoreboardTitlePattern by patternGroup.pattern(
            "scoreboard.title",
            "SK[YI]BLOCK(?: CO-OP| GUEST)?"
        )

        /**
         * REGEX-TEST:  §7⏣ §bVillage
         * REGEX-TEST:  §5ф §dWizard Tower
         */
        private val skyblockAreaPattern by patternGroup.pattern(
            "skyblock.area",
            "\\s*§(?<symbol>7⏣|5ф) §(?<color>.)(?<area>.*)"
        )

        var hypixelLive = false
        var hypixelAlpha = false
        var inLobby = false
        var inLimbo = false
        var skyBlock = false
        var skyBlockIsland = IslandType.UNKNOWN
        var serverId: String? = null

        // Ironman, Stranded and Bingo
        var noTrade = false

        var ironman = false
        var stranded = false
        var bingo = false

        var profileName = ""
        var joinedWorld = SimpleTimeMark.farPast()

        var skyBlockArea: String? = null
        var skyBlockAreaWithSymbol: String? = null

        // Data from locraw
        var locrawData: JsonObject? = null
        private var locraw: MutableMap<String, String> = mutableMapOf(
            "server" to "",
            "gametype" to "",
            "lobbyname" to "",
            "lobbytype" to "",
            "mode" to "",
            "map" to ""
        )

        val server get() = locraw["server"] ?: ""
        val gameType get() = locraw["gametype"] ?: ""
        val lobbyName get() = locraw["lobbyname"] ?: ""
        val lobbyType get() = locraw["lobbytype"] ?: ""
        val mode get() = locraw["mode"] ?: ""
        val map get() = locraw["map"] ?: ""

        fun checkCurrentServerId() {
            if (!LorenzUtils.inSkyBlock) return
            if (serverId != null) return
            if (LorenzUtils.lastWorldSwitch.passedSince() < 1.seconds) return
            if (!TabListData.fullyLoaded) return

            ScoreboardData.sidebarLinesFormatted.matchFirst(serverIdScoreboardPattern) {
                val serverType = if (group("servertype") == "M") "mega" else "mini"
                serverId = "$serverType${group("serverid")}"
                return
            }

            TabListData.getTabList().matchFirst(serverIdTablistPattern) {
                serverId = group("serverid")
                return
            }

            ErrorManager.logErrorWithData(
                Exception("NoServerId"), "Could not find server id",
                "islandType" to LorenzUtils.skyBlockIsland,
                "tablist" to TabListData.getTabList(),
                "scoreboard" to ScoreboardData.sidebarLinesFormatted
            )
        }

        fun getPlayersOnCurrentServer(): Int {
            var amount = 0
            val playerPatternList = mutableListOf(
                playerAmountPattern,
                playerAmountCoopPattern,
                playerAmountGuestingPattern,
            )
            if (DungeonAPI.inDungeon()) {
                playerPatternList.add(dungeonPartyAmountPattern)
            }

            out@ for (pattern in playerPatternList) {
                for (line in TabListData.getTabList()) {
                    pattern.matchMatcher(line) {
                        amount += group("amount").toInt()
                        continue@out
                    }
                }
            }
            amount += TabListData.getTabList().count { soloProfileAmountPattern.matches(it) }

            return amount
        }

        fun getMaxPlayersForCurrentServer(): Int {
            ScoreboardData.sidebarLinesFormatted.matchFirst(scoreboardVisitingAmoutPattern) {
                return group("maxamount").toInt()
            }

            return when (skyBlockIsland) {
                IslandType.MINESHAFT -> 4
                IslandType.CATACOMBS -> 5
                IslandType.CRYSTAL_HOLLOWS -> 24
                IslandType.CRIMSON_ISLE -> 24
                else -> if (serverId?.startsWith("mega") == true) 80 else 26
            }
        }

        // This code is modified from NEU, and depends on NEU (or another mod) sending /locraw.
        private val jsonBracketPattern = "^\\{.+}".toPattern()

        //todo convert to proper json object
        fun checkForLocraw(message: String) {
            jsonBracketPattern.matchMatcher(message.removeColor()) {
                try {
                    val obj: JsonObject = gson.fromJson(group(), JsonObject::class.java)
                    if (obj.has("server")) {
                        locrawData = obj
                        locraw.keys.forEach { key ->
                            locraw[key] = obj[key]?.asString ?: ""
                        }
                        inLimbo = locraw["server"] == "limbo"
                        inLobby = locraw["lobbyname"] != ""

                        if (inLobby) {
                            locraw["lobbyname"]?.let {
                                lobbyTypePattern.matchMatcher(it) {
                                    locraw["lobbytype"] = group("lobbyType")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    ErrorManager.logErrorWithData(e, "Failed to parse locraw data")
                }
            }
        }
    }

    private var loggerIslandChange = LorenzLogger("debug/island_change")

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        locrawData = null
        skyBlock = false
        inLimbo = false
        inLobby = false
        locraw.forEach { locraw[it.key] = "" }
        joinedWorld = SimpleTimeMark.now()
        serverId = null
        skyBlockArea = null
        skyBlockAreaWithSymbol = null
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        hypixelLive = false
        hypixelAlpha = false
        skyBlock = false
        inLobby = false
        locraw.forEach { locraw[it.key] = "" }
        locrawData = null
        skyBlockArea = null
        skyBlockAreaWithSymbol = null
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.onHypixel) return

        val message = event.message.removeColor().lowercase()
        if (message.startsWith("your profile was changed to:")) {
            val newProfile = message.replace("your profile was changed to:", "").replace("(co-op)", "").trim()
            if (profileName == newProfile) return
            profileName = newProfile
            ProfileJoinEvent(newProfile).postAndCatch()
        }
        if (message.startsWith("you are playing on profile:")) {
            val newProfile = message.replace("you are playing on profile:", "").replace("(co-op)", "").trim()
            if (profileName == newProfile) return
            profileName = newProfile
            ProfileJoinEvent(newProfile).postAndCatch()
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        event.tabList.matchFirst(UtilsPatterns.tabListProfilePattern) {
            var newProfile = group("profile").lowercase()

            // Hypixel shows the profile name reversed while in the Rift
            if (RiftAPI.inRift()) newProfile = newProfile.reversed()
            if (profileName == newProfile) return
            profileName = newProfile
            ProfileJoinEvent(newProfile).postAndCatch()
        }
    }

    @SubscribeEvent
    // TODO rewrite everything in here
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) {
            // Modified from NEU.
            // NEU does not send locraw when not in SkyBlock.
            // So, as requested by Hannibal, use locraw from
            // NEU and have NEU send it.
            // Remove this when NEU dependency is removed
            if (LorenzUtils.onHypixel &&
                locrawData == null &&
                lastLocRaw.passedSince() > 15.seconds
            ) {
                lastLocRaw = SimpleTimeMark.now()
                thread(start = true) {
                    Thread.sleep(1000)
                    NotEnoughUpdates.INSTANCE.sendChatMessage("/locraw")
                }
            }
        }

        if (LorenzUtils.onHypixel) {
            if (LorenzUtils.inSkyBlock) {
                loop@ for (line in ScoreboardData.sidebarLinesFormatted) {
                    skyblockAreaPattern.matchMatcher(line) {
                        val originalLocation = group("area")
                        skyBlockArea = LocationFixData.fixLocation(skyBlockIsland) ?: originalLocation
                        skyBlockAreaWithSymbol = line.trim()
                        break@loop
                    }
                }

                checkProfileName()
            }
        }

        if (!event.isMod(5)) return

        if (!LorenzUtils.onHypixel) {
            checkHypixel()
            if (LorenzUtils.onHypixel) {
                HypixelJoinEvent().postAndCatch()
                SkyHanniMod.repo.displayRepoStatus(true)
            }
        }
        if (!LorenzUtils.onHypixel) return

        val inSkyBlock = checkScoreboard()
        if (inSkyBlock) {
            checkIsland()
            checkSidebar()
            checkCurrentServerId()
        }

        if (inSkyBlock == skyBlock) return
        skyBlock = inSkyBlock
    }

    private fun checkProfileName() {
        if (profileName.isNotEmpty()) return

        TabListData.getTabList().matchFirst(UtilsPatterns.tabListProfilePattern) {
            profileName = group("profile").lowercase()
            ProfileJoinEvent(profileName).postAndCatch()
        }
    }

    private fun checkHypixel() {
        val list = ScoreboardData.sidebarLinesFormatted
        if (list.isEmpty()) return

        val last = list.last()
        hypixelLive = last == "§ewww.hypixel.net"
        hypixelAlpha = last == "§ealpha.hypixel.net"
    }

    private fun checkSidebar() {
        ironman = false
        stranded = false
        bingo = false

        for (line in ScoreboardData.sidebarLinesFormatted) {
            if (BingoAPI.getRankFromScoreboard(line) != null) {
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

    private fun checkIsland() {
        var foundIsland = ""
        TabListData.fullyLoaded = false

        TabListData.getTabList().matchFirst(islandNamePattern) {
            foundIsland = group("island").removeColor()
            TabListData.fullyLoaded = true
        }

        // Can not use color coding, because of the color effect (§f§lSKYB§6§lL§e§lOCK§A§L GUEST)
        val guesting = guestPattern.matches(ScoreboardData.objectiveTitle.removeColor())
        val islandType = getIslandType(foundIsland, guesting)
        if (skyBlockIsland != islandType) {
            IslandChangeEvent(islandType, skyBlockIsland).postAndCatch()
            if (islandType == IslandType.UNKNOWN) {
                ChatUtils.debug("Unknown island detected: '$foundIsland'")
                loggerIslandChange.log("Unknown: '$foundIsland'")
            } else {
                loggerIslandChange.log(islandType.name)
            }
            skyBlockIsland = islandType
        }
    }

    private fun getIslandType(name: String, guesting: Boolean): IslandType {
        val islandType = IslandType.getByNameOrUnknown(name)
        if (guesting) {
            if (islandType == IslandType.PRIVATE_ISLAND) return IslandType.PRIVATE_ISLAND_GUEST
            if (islandType == IslandType.GARDEN) return IslandType.GARDEN_GUEST
        }
        return islandType
    }

    private fun checkScoreboard(): Boolean {
        val minecraft = Minecraft.getMinecraft()
        val world = minecraft.theWorld ?: return false

        val objective = world.scoreboard.getObjectiveInDisplaySlot(1) ?: return false
        val displayName = objective.displayName
        val scoreboardTitle = displayName.removeColor()
        return scoreboardTitlePattern.matches(scoreboardTitle)
    }
}
