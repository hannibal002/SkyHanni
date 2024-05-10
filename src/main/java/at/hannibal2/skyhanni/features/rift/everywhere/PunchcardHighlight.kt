package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.addItemIcon
import at.hannibal2.skyhanni.utils.RenderUtils.renderSingleLineWithItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object PunchcardHighlight {
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

    private val playerList: MutableSet<String> = mutableSetOf()
    private var playerQueue = mutableListOf<String>()

    private val displayIcon by lazy { "PUNCHCARD_ARTIFACT".asInternalName().getItemStack() }
    private var display = mutableListOf<Any>()

    @SubscribeEvent
    fun onPlayerSpawn(event: MobEvent.Spawn.Player) {
        if (!config.enabled.get()) return
        if (!IslandType.THE_RIFT.isInIsland()) return
        if (config.reverse.get()) return
        val size = playerList.size
        if (size >= 20) return
        val entity = event.mob
        if (!playerList.contains(entity.name)) {
            colorPlayer(entity.baseEntity)
        }
    }

    @SubscribeEvent
    fun onToggle(event: ConfigLoadEvent) {
        config.enabled.onToggle { reloadColors() }
        config.compact.onToggle { display = drawDisplay() }
        config.color.onToggle { reloadColors() }
        config.reverseGUI.onToggle { display = drawDisplay() }
        config.reverse.onToggle { reloadColors() }
    }

    @SubscribeEvent
    fun onWorldSwitch(event: IslandChangeEvent) {
        display = drawDisplay()
        DelayedRun.runDelayed(1500.milliseconds) {
            reloadColors()
            if (IslandType.THE_RIFT.isInIsland() && HypixelData.server.isNotEmpty() && lastRiftServer != HypixelData.server) {
                lastRiftServer = HypixelData.server
                playerList.clear()
            }
        }
    }

    private fun colorPlayer(entity: EntityLivingBase, remove: Boolean = false) {
        if (remove) {
            RenderLivingEntityHelper.removeEntityColor(entity)
            return
        }
        val alpha = when (config.color.get().toChromaColor().alpha) {
            0 -> 0
            255 -> 1
            else -> 255 - config.color.get().toChromaColor().alpha
        }
        val color = config.color.get().toChromaColor().withAlpha(alpha)
        RenderLivingEntityHelper.setEntityColor(
            entity,
            color
        ) { IslandType.THE_RIFT.isInIsland() }
    }

    fun clearList() {
        playerList.clear()
        playerQueue.clear()
        if (config.reverse.get()) {
            MobData.players.forEach {
                colorPlayer(it.baseEntity)
            }
        } else {
            MobData.players.forEach {
                colorPlayer(it.baseEntity, true)
            }
        }
    }

    @SubscribeEvent
    fun onPunch(event: EntityClickEvent) {
        if (!RiftAPI.inRift()) return
        if (!config.enabled.get()) return
        val entity = event.clickedEntity
        if (entity !is AbstractClientPlayer) return
        if (entity.isNPC()) return
        val name = entity.name
        if (name in playerList || name in playerQueue) return
        playerQueue.add(name)
        listening = true
        DelayedRun.runDelayed(1.seconds) {
            if (name in playerQueue) playerQueue.remove(name)
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
            else ErrorManager.logErrorStateWithData(
                "Error finding punched player", "queuedName and capturedName were different",
                "queuedName" to queuedName,
                "capturedName" to name,
                noStackTrace = true,
                betaOnly = true
            )
            return
        }
        when {
            limitPattern.matches(message) -> addPunch(queuedName)
            repeatPattern.matches(message) -> addPunch(queuedName)
        }
    }

    private fun addPunch(playerName: String) {
        playerList.add(playerName)
        playerQueue.remove(playerName)
        val player = MobData.players.firstOrNull { it.name == playerName } ?: return
        if (!config.reverse.get()) colorPlayer(player.baseEntity, true)
        else colorPlayer(player.baseEntity)
        display = drawDisplay()
    }

    @SubscribeEvent
    fun onRenderUI(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.gui) return
        if (!RiftAPI.inRift()) return
        config.position.renderSingleLineWithItems(display, "Punchcard Overlay")
    }

    private fun drawDisplay(): MutableList<Any> {
        return mutableListOf<Any>().apply {
            addItemIcon(displayIcon)
            if (!config.compact.get()) add("Punchcard Artifact: ")

            val amount = if (!config.reverseGUI.get()) playerList.size
                        else 20 - playerList.size
            add("§d$amount")
        }
    }

    private fun reloadColors() {
        MobData.players.forEach {
            colorPlayer(it.baseEntity, true)
        }
        if (!config.enabled.get()) return
        val reverse = config.reverse.get()
        if (reverse) {
            MobData.players.filter { it.name in playerList }.forEach {
                colorPlayer(it.baseEntity)
            }
        } else {
            MobData.players.filter { it.name !in playerList }.forEach {
                colorPlayer(it.baseEntity)
            }
        }
    }
}
