package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.client.Minecraft
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

class HypixelData {

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

        fun readSkyBlockArea(): String {
            return ScoreboardData.sidebarLinesFormatted
                .firstOrNull { it.startsWith(" §7⏣ ") }
                ?.substring(5)?.removeColor()
                ?: "invalid"
        }
    }

    private var loggerIslandChange = LorenzLogger("debug/island_change")

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        skyBlock = false
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        hypixelLive = false
        hypixelAlpha = false
        skyBlock = false
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.isHypixel) return

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

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        tick++
        if (tick % 5 != 0) return

        checkHypixel()

        if (!LorenzUtils.isHypixel) return

        val inSkyBlock = checkScoreboard()
        if (inSkyBlock) {
            checkIsland()
            checkSidebar()
        }

        if (inSkyBlock == skyBlock) return
        skyBlock = inSkyBlock
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
                println("Unknown island detected: '$newIsland'")
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