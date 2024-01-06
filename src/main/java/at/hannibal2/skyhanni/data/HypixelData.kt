package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager.Companion.gson
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonObject
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import kotlin.concurrent.thread

class HypixelData {
    private val group = RepoPattern.group("data.hypixeldata")
    private val tabListProfilePattern by group.pattern("tablistprofile", "§e§lProfile: §r§a(?<profile>.*)")
    private val lobbyTypePattern by group.pattern("lobbytype", "(?<lobbyType>.*lobby)\\d+")
    private val islandNamePattern by group.pattern("islandname", "(?:§.)*(Area|Dungeon): (?:§.)*(?<island>.*)")

    private var lastLocRaw = 0L

    companion object {
        var hypixelLive = false
        var hypixelAlpha = false
        var inLobby = false
        var inLimbo = false
        var skyBlock = false
        var skyBlockIsland = IslandType.UNKNOWN

        //Ironman, Stranded and Bingo
        var noTrade = false

        var ironman = false
        var stranded = false
        var bingo = false

        var profileName = ""
        var joinedWorld = 0L

        var skyBlockArea = "?"

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
    }

    private var loggerIslandChange = LorenzLogger("debug/island_change")

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        locrawData = null
        skyBlock = false
        inLimbo = false
        inLobby = false
        locraw.forEach { locraw[it.key] = "" }
        joinedWorld = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        hypixelLive = false
        hypixelAlpha = false
        skyBlock = false
        inLobby = false
        locraw.forEach { locraw[it.key] = "" }
        locrawData = null
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.onHypixel) return

        val message = event.message.removeColor().lowercase()
        if (message.startsWith("your profile was changed to:")) {
            val newProfile = message.replace("your profile was changed to:", "").replace("(co-op)", "").trim()
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
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) {
            // Modified from NEU.
            // NEU does not send locraw when not in SkyBlock.
            // So, as requested by Hannibal, use locraw from
            // NEU and have NEU send it.
            // Remove this when NEU dependency is removed
            val currentTime = System.currentTimeMillis()
            if (LorenzUtils.onHypixel &&
                locrawData == null &&
                currentTime - lastLocRaw > 15000
            ) {
                lastLocRaw = System.currentTimeMillis()
                thread(start = true) {
                    Thread.sleep(1000)
                    NotEnoughUpdates.INSTANCE.sendChatMessage("/locraw")
                }
            }
        }

        if (event.isMod(2) && LorenzUtils.inSkyBlock) {
            val originalLocation = ScoreboardData.sidebarLinesFormatted
                .firstOrNull { it.startsWith(" §7⏣ ") || it.startsWith(" §5ф ") }
                ?.substring(5)?.removeColor()
                ?: "?"
            skyBlockArea = LocationFixData.fixLocation(skyBlockIsland) ?: originalLocation

            checkProfileName()
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
        }

        if (inSkyBlock == skyBlock) return
        skyBlock = inSkyBlock
    }

    private fun checkProfileName(): Boolean {
        if (profileName.isEmpty()) {
            val text = TabListData.getTabList().firstOrNull { it.contains("Profile:") } ?: return true
            tabListProfilePattern.matchMatcher(text) {
                profileName = group("profile").lowercase()
                ProfileJoinEvent(profileName).postAndCatch()
            }
        }
        return false
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
        var newIsland = ""
        var guesting = false
        for (line in TabListData.getTabList()) {
            islandNamePattern.matchMatcher(line) {
                newIsland = group("island").removeColor()
            }
            if (line == " Status: §r§9Guest") {
                guesting = true
            }
        }

        val islandType = getIslandType(newIsland, guesting)
        if (skyBlockIsland != islandType) {
            IslandChangeEvent(islandType, skyBlockIsland).postAndCatch()
            if (islandType == IslandType.UNKNOWN) {
                LorenzUtils.debug("Unknown island detected: '$newIsland'")
                loggerIslandChange.log("Unknown: '$newIsland'")
            } else {
                loggerIslandChange.log(islandType.name)
            }
            skyBlockIsland = islandType
        }
    }

    private fun getIslandType(newIsland: String, guesting: Boolean): IslandType {
        val islandType = IslandType.getByNameOrUnknown(newIsland)
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
        return scoreboardTitle.contains("SKYBLOCK") ||
            scoreboardTitle.contains("SKIBLOCK") // April 1st jokes are so funny
    }

    // This code is modified from NEU, and depends on NEU (or another mod) sending /locraw.
    private val jsonBracketPattern = "^\\{.+}".toPattern()

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatMessage(event: ClientChatReceivedEvent) {
        jsonBracketPattern.matchMatcher(event.message.unformattedText) {
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
