package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class PunchcardHighlight {
    private val config get() = SkyHanniMod.feature.rift.punchcard
    private val ownIGN get() = Minecraft.getMinecraft().thePlayer.name
    private var lastRiftServer: String = ""

    private val punchedPattern by RepoPattern.pattern(
        "rift.punchcard.new",
        "§5§lPUNCHCARD! §r§eYou punched §r§.(?:.*?)?(?<name>\\w+)§r§. §r§eand both regained §r§a\\+25ф Rift Time§r§e!"
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
            colorPlayer(entity.baseEntity)
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!IslandType.THE_RIFT.isInIsland()) return
        val matcher = punchedPattern.matcher(event.message)
        if (matcher.find()) {
            val name = matcher.group("name")
            addPunch(name)
            MobData.players.filter { it.name == name }.forEach {
                RenderLivingEntityHelper.removeEntityColor(it.baseEntity)
            }
        }
    }

    @SubscribeEvent
    fun onWorldSwitch(event: IslandChangeEvent) {
        DelayedRun.runDelayed(1500.milliseconds) {
            if (IslandType.THE_RIFT.isInIsland() && HypixelData.server.isNotEmpty() && lastRiftServer != HypixelData.server) {
                lastRiftServer = HypixelData.server
                playerList.clear()
                MobData.players.filter { it.name != ownIGN }.forEach {
                    colorPlayer(it.baseEntity)
                }
            }
        }
    }

    private fun colorPlayer(entity: EntityLivingBase) {
        if (entity.name in playerList) return
        val alpha = when (config.color.toChromaColor().alpha) {
            0 -> 0
            255 -> 1
            else -> 255-config.color.toChromaColor().alpha
        }
        val color = config.color.toChromaColor().withAlpha(alpha)
        RenderLivingEntityHelper.setEntityColor(entity, color) { IslandType.THE_RIFT.isInIsland() && playerList.size < 20 }
    }

    fun clearList() {
        playerList.clear()
        MobData.players.filter { it.name != ownIGN }.forEach {
            RenderLivingEntityHelper.removeEntityColor(it.baseEntity)
        }
    }

    private fun addPunch(playerName: String) { playerList.add(playerName) }
}
