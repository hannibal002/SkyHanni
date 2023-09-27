package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.client.Minecraft
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

class HypixelData {
    private val tabListProfilePattern = "§e§lProfile: §r§a(?<profile>.*)".toPattern()
    private val westVillageFarmArea = AxisAlignedBB(-54.0, 69.0, -115.0, -40.0, 75.0, -127.0)
    private val howlingCaveArea = AxisAlignedBB(-401.0, 50.0, -104.0, -337.0, 90.0, 36.0)
    private val fakeZealotBruiserHideoutArea = AxisAlignedBB(-520.0, 66.0, -332.0, -558.0, 85.0, -280.0)
    private val realZealotBruiserHideoutArea = AxisAlignedBB(-552.0, 50.0, -245.0, -580.0, 72.0, -209.0)

    companion object {
        var hypixelLive = false
        var hypixelAlpha = false
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
    }

    private var loggerIslandChange = LorenzLogger("debug/island_change")

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        skyBlock = false
        joinedWorld = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        hypixelLive = false
        hypixelAlpha = false
        skyBlock = false
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
        if (event.isMod(2)) {
            if (LorenzUtils.inSkyBlock) {
                val originalLocation = ScoreboardData.sidebarLinesFormatted
                    .firstOrNull { it.startsWith(" §7⏣ ") || it.startsWith(" §5ф ") }
                    ?.substring(5)?.removeColor()
                    ?: "?"

                skyBlockArea = when {
                    skyBlockIsland == IslandType.THE_RIFT && westVillageFarmArea.isPlayerInside() -> "Dreadfarm"
                    skyBlockIsland == IslandType.THE_PARK && howlingCaveArea.isPlayerInside() -> "Howling Cave"
                    skyBlockIsland == IslandType.THE_END && fakeZealotBruiserHideoutArea.isPlayerInside() -> "The End"
                    skyBlockIsland == IslandType.THE_END && realZealotBruiserHideoutArea.isPlayerInside() -> "Zealot Bruiser Hideout"

                    else -> originalLocation
                }

                checkProfileName()
            }
        }

        if (!event.isMod(5)) return

        if (!LorenzUtils.onHypixel) {
            checkHypixel()
            if (LorenzUtils.onHypixel) {
                HypixelJoinEvent().postAndCatch()
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
            when (line) {
                " §7Ⓑ §7Bingo", // No Rank
                " §aⒷ §aBingo", // Rank 1
                " §9Ⓑ §9Bingo", // Rank 2
                " §5Ⓑ §5Bingo", // Rank 3
                " §6Ⓑ §6Bingo", // Rank 4
                -> {
                    bingo = true
                }

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
            if (line.startsWith("§b§lArea: ")) {
                newIsland = line.split(": ")[1].removeColor()
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
        val islandType = IslandType.getBySidebarName(newIsland)
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
}