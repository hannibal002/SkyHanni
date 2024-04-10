package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class PunchcardHighlight {
    private val config get() = SkyHanniMod.feature.rift.punchcard
    private val ownIGN get() = Minecraft.getMinecraft().thePlayer.name
    private var lastRiftServer: String = ""

    private val punchedPattern by RepoPattern.pattern(
        "rift.punchcard.new",
        "§5§lPUNCHCARD! §r§eYou punched §r§[\\da-z](?:.*?)?(?<name>\\w+)§r§[\\da-z] §r§eand both regained §r§a\\+25ф Rift Time§r§e!"
    )

    private val playerList: MutableSet<String> = mutableSetOf()

    @SubscribeEvent
    fun onRenderMobColored(event: MobEvent.Spawn.Player) {
        if (!config.enabled) return
        if (!LorenzUtils.inSkyBlock) return
        if (!IslandType.THE_RIFT.isInIsland()) return
        val size = playerList.size
        if (size >= 20) return
        val entity = event.mob
        if (!playerList.contains(entity.name) && entity.name != ownIGN) {
            val alpha = when (config.color.toChromaColor().alpha) {
                0 -> 0
                255 -> 1
                else -> 255-config.color.toChromaColor().alpha
            }
            val color = config.color.toChromaColor().withAlpha(alpha)
            RenderLivingEntityHelper.setEntityColor(entity.baseEntity, color) { IslandType.THE_RIFT.isInIsland() && playerList.size < 20 }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        punchedPattern.matchMatcher(event.message) {
            addPunch(group("name"))
        }
    }

    @SubscribeEvent
    fun onWorldSwitch(event: IslandChangeEvent) {
        DelayedRun.runDelayed(1500.milliseconds) {
            if (IslandType.THE_RIFT.isInIsland() && HypixelData.server.isNotEmpty() && lastRiftServer != HypixelData.server) {
                lastRiftServer = HypixelData.server
                playerList.clear()
            }
        }
    }

    fun clearList() { playerList.clear() }

    private fun addPunch(playerName: String) { playerList.add(playerName) }
}
