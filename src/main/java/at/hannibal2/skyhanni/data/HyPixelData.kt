package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.client.Minecraft
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

class HyPixelData {

    companion object {
        var hypixel = false
        var skyBlock = false
        var skyBlockIsland = IslandType.UNKNOWN

        //Ironman, Stranded and Bingo
        var noTrade = false

        var ironman = false
        var stranded = false
        var bingo = false

        fun readSkyBlockArea(): String {
            return ScoreboardData.sidebarLinesFormatted()
                .firstOrNull { it.startsWith(" §7⏣ ") }
                ?.substring(5)?.removeColor()
                ?: "invalid"
        }
    }

    private var loggerIslandChange = LorenzLogger("debug/island_change")

    @SubscribeEvent
    fun onConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        hypixel = Minecraft.getMinecraft().runCatching {
            !event.isLocal && (thePlayer?.clientBrand?.lowercase()?.contains("hypixel")
                ?: currentServerData?.serverIP?.lowercase()?.contains("hypixel") ?: false)
        }.onFailure { it.printStackTrace() }.getOrDefault(false)
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        skyBlock = false
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        hypixel = false
        skyBlock = false
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!hypixel) return

        val message = event.message.removeColor().lowercase()
        if (message.startsWith("your profile was changed to:")) {
            val stripped = message.replace("your profile was changed to:", "").replace("(co-op)", "").trim()
            ProfileJoinEvent(stripped).postAndCatch()
        }
        if (message.startsWith("you are playing on profile:")) {
            val stripped = message.replace("you are playing on profile:", "").replace("(co-op)", "").trim()
            ProfileJoinEvent(stripped).postAndCatch()
        }
    }

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!hypixel) return
        if (event.phase != TickEvent.Phase.START) return

        tick++

        if (tick % 5 != 0) return

        val inSkyBlock = checkScoreboard()
        if (inSkyBlock) {
            checkIsland()
            checkSidebar()
        }

        if (inSkyBlock == skyBlock) return
        skyBlock = inSkyBlock
    }

    private fun checkSidebar() {
        ironman = false
        stranded = false
        bingo = false

        for (line in ScoreboardData.sidebarLinesFormatted()) {
            when (line) {
                " §7Ⓑ §7Bingo", // No Rank
                " §bⒷ §bBingo", // Rank 1
                " §9Ⓑ §9Bingo", // Rank 2
                " §5Ⓑ §5Bingo", // Rank 3
                " §6Ⓑ §6Bingo", // Rank 4
                -> {
                    bingo = true
                }

                // TODO implemennt stranded check

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

        var islandType = IslandType.getBySidebarName(newIsland)

        if (islandType == IslandType.PRIVATE_ISLAND) {
            if (guesting) {
                islandType = IslandType.PRIVATE_ISLAND_GUEST
            }
        }

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

    private fun checkScoreboard(): Boolean {
        val minecraft = Minecraft.getMinecraft()
        val world = minecraft.theWorld ?: return false

        val objective = world.scoreboard.getObjectiveInDisplaySlot(1) ?: return false
        val displayName = objective.displayName
        return displayName.removeColor().contains("SKYBLOCK")
    }
}