package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PunchcardHighlight {
    private val config get() = SkyHanniMod.feature.rift.punchcard
    private var lastRiftServer: String = ""

    private var listening = false

    private val patternGroup = RepoPattern.group("rift.punchcard")
    private val punchedPattern by patternGroup.pattern(
        "new",
        "§5§lPUNCHCARD! §r§eYou punched §r§.(?:.*?)?(?<name>\\w+)§r§. §r§eand both regained §r§a\\+25ф Rift Time§r§e!"
    )
    private val repeatPattern by patternGroup.pattern(
        "repeat",
        "§c§lAWKWARD! §r§cThis player has already been punched by you... somehow!"
    )
    private val limitPattern by patternGroup.pattern(
        "limit",
        "§c§lUH OH! §r§cYou reached the limit of 20 players you can punch in one session!"
    )

    val playerList: MutableSet<String> = mutableSetOf()

    @SubscribeEvent
    fun onPlayerSpawn(event: MobEvent.Spawn.Player) {
        if (!config.enabled.get()) return
        if (!IslandType.THE_RIFT.isInIsland()) return
        val size = playerList.size
        if (size >= 20) return
        val entity = event.mob
        if (!playerList.contains(entity.name)) {
            colorPlayer(entity.baseEntity)
        }
    }

    @SubscribeEvent
    fun onToggle(event: ConfigLoadEvent) {
        config.enabled.onToggle { toggleConfig() }
    }

    private fun toggleConfig() {
        if (config.enabled.get()) {
            MobData.players.forEach {
                colorPlayer(it.baseEntity)
            }
        } else {
            MobData.players.forEach {
                RenderLivingEntityHelper.removeEntityColor(it.baseEntity)
            }
        }
    }

    @SubscribeEvent
    fun onWorldSwitch(event: IslandChangeEvent) {
        DelayedRun.runDelayed(500.milliseconds) {
            if (IslandType.THE_RIFT.isInIsland() && HypixelData.server.isNotEmpty() && lastRiftServer != HypixelData.server) {
                lastRiftServer = HypixelData.server
                playerList.clear()
                MobData.players.forEach {
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
            else -> 255 - config.color.toChromaColor().alpha
        }
        val color = config.color.toChromaColor().withAlpha(alpha)
        RenderLivingEntityHelper.setEntityColor(
            entity,
            color
        ) { IslandType.THE_RIFT.isInIsland() && playerList.size < 20 }
    }

    fun clearList() {
        playerList.clear()
        playerQueue.clear()
        MobData.players.forEach {
            RenderLivingEntityHelper.removeEntityColor(it.baseEntity)
        }
    }

    var playerQueue = mutableListOf<String>()

    @SubscribeEvent
    fun onPunch(event: EntityClickEvent) {
        if (!RiftAPI.inRift()) return
        if (!config.enabled.get()) return
        val entity = event.clickedEntity
        if (entity !is AbstractClientPlayer) return
        if (entity.isNPC()) {
            ChatUtils.chat("was npc oops")
            return
        }
        val name = entity.name
        if (name in playerList) return
        playerQueue.add(name)
        listening = true
        ChatUtils.chat("listening")
        DelayedRun.runDelayed(2.seconds) {
            ChatUtils.chat("not listening")
            listening = false
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!IslandType.THE_RIFT.isInIsland()) return
        if (!listening) return
        if (playerQueue.size == 0) return
        val message = event.message
        val queuedName = playerQueue[0]
        punchedPattern.matchMatcher(message) {
            val name = group("name")
            if (queuedName == name) addPunch(name)
            else println("help! '$name' '$queuedName'") //throw error
            return
        }
        when {
            limitPattern.matches(message) -> addPunch(queuedName)
            repeatPattern.matches(message) -> addPunch(queuedName)
        }
    }

    private fun addPunch(playerName: String) {
        playerList.add(playerName)
        val player = MobData.players.firstOrNull { it.name == playerName } ?: return
        RenderLivingEntityHelper.removeEntityColor(player.baseEntity)
        playerQueue.removeAt(0)
    }
}
