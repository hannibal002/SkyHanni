package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PunchcardHighlight {
    private val config get() = SkyHanniMod.feature.rift.punchcard
    private val ownIGN get() = Minecraft.getMinecraft().thePlayer.name
    private var lastRiftServer: String = ""

    private val patternGroup = RepoPattern.group("rift.punchcard")
    private val punchedPattern by patternGroup.pattern(
        "new",
        "§5§lPUNCHCARD! §r§eYou punched §r§[\\da-z](?:.*?)?(\\w+)§r§[\\da-z] §r§eand both regained §r§a\\+25ф Rift Time§r§e!"
    )
    private val repeatPattern by patternGroup.pattern(
        "repeat",
        ""
    )

    val playerList: MutableSet<String> = mutableSetOf()

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!config.enabled) return
        if (!LorenzUtils.inSkyBlock) return
        if (!IslandType.THE_RIFT.isInIsland()) return
        val size = playerList.size
        if (size >= 20) return
        val entity = event.entity
        if (entity is EntityPlayerSP) return
        if (entity is EntityPlayer && !entity.isNPC() && !hasPunchedPlayer(entity) && entity.name != ownIGN) {
            val alpha = when (config.color.toChromaColor().alpha) {
                0 -> 0
                255 -> 1
                else -> 255-config.color.toChromaColor().alpha
            }
            event.color = config.color.toChromaColor().withAlpha(alpha)
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        punchedPattern.matchMatcher(event.message) {
            addPunch(group(1))
        }
    }

    @SubscribeEvent
    fun onWorldSwitch(event: IslandChangeEvent) {
        SkyHanniMod.coroutineScope.launch {
            delay(1500)
            if (IslandType.THE_RIFT.isInIsland() && HypixelData.server.isNotEmpty() && lastRiftServer != HypixelData.server) {
                lastRiftServer = HypixelData.server
                playerList.clear()
            }
        }
    }

    private fun addPunch(playerName: String) { playerList.add(playerName) }

    private fun hasPunchedPlayer(player: EntityPlayer) = playerList.contains(player.name)
}
