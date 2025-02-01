package at.hannibal2.skyhanni.features.event.anniversary

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.EntityLivingBase
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object Year400Features {
    private val config get() = SkyHanniMod.feature.event.anniversaryCelebration400

    private var colorInHand: CakeColor? = null
    private val playerColors = mutableMapOf<Int, CakeColor?>()
    private var lastClickedPlayer: Int? = null
    private var lastClickedPlayerTime = SimpleTimeMark.farPast()

    private val chatGroup = RepoPattern.group("event.anniversary-celebration.400.team-finder")

    /**
     * REGEX-TEST: §r§8[§2172§8] §b_EliteNefarious §9⛃§r
     * REGEX-TEST: §r§8[§f72§8] §bBee181204 §a⛃§r
     */
    private val playerColorNametagPattern by chatGroup.pattern(
        "player-color-nametag",
        ".* §(?<color>.)⛃§r",
    )

    /**
     * REGEX-TEST: This person has had too much cake today!
     */
    private val fatPlayerMessagePattern by chatGroup.pattern(
        "player-full-message",
        "This person has had too much cake today!",
    )

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        playerColors.clear()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        val new = CakeColor.entries.find { event.newItem == it.internalName }
        if (colorInHand == new) return
        colorInHand = new

        updateAllPlayers(new)
    }

    private fun updateAllPlayers(colorInHand: CakeColor?) {
        val correctColor = colorInHand?.color ?: LorenzColor.DARK_GRAY
        val correctPlayers = playerColors.filter { it.value == colorInHand }.keys

        for (entity in EntityUtils.getEntities<EntityOtherPlayerMP>()) {
            if (entity.isNpc()) continue
            val color = if (entity.entityId in correctPlayers) correctColor else LorenzColor.DARK_GRAY
            entity.setColor(color, colorInHand)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerSpawn(event: MobEvent.Spawn.Player) {
        val entity = event.mob.baseEntity
        DelayedRun.runDelayed(1.seconds) {
            addPlayer(entity)
        }
    }

    private fun addPlayer(entity: EntityLivingBase) {
        val displayName = entity.displayName.formattedText
        val colorCode = playerColorNametagPattern.matchMatcher(displayName) {
            group("color")
        } ?: run {
            if (colorInHand != null) {
                entity.setColor(LorenzColor.DARK_GRAY, null)
            }
            return
        }

        val color = colorCode.toCharArray().first().toLorenzColor()
        val cakeColor = CakeColor.entries.find { it.color == color } ?: run {
            ErrorManager.logErrorStateWithData(
                "Unknown slice of cake color",
                "Unknown slice of cake color",
                "displayName" to displayName,
                "colorCode" to colorCode,
                "color" to color,
            )
            return
        }
        playerColors[entity.entityId] = cakeColor

        colorInHand?.let {
            entity.setColor(cakeColor.color, it)
        }
    }

    private fun EntityLivingBase.setColor(color: LorenzColor, currentHand: CakeColor?) {
        RenderLivingEntityHelper.setEntityColor(this, color.toColor().addAlpha(1)) { config.teamFinder && colorInHand == currentHand }
    }

    @HandleEvent
    fun onRealPlayerDeSpawnEvent(event: MobEvent.DeSpawn.Player) {
        playerColors.remove(event.mob.baseEntity.entityId)
    }

    @HandleEvent
    fun onPunch(event: EntityClickEvent) {
        val entity = event.clickedEntity
        if (colorInHand == null) return
        if (entity !is EntityOtherPlayerMP) return
        if (entity.isNpc()) return

        lastClickedPlayer = entity.entityId
        lastClickedPlayerTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onSystemMessage(event: SystemMessageEvent) {
        if (!fatPlayerMessagePattern.matches(event.message.removeColor())) return
        if (lastClickedPlayerTime.passedSince() >= 500.milliseconds) return

        val lastPlayer = lastClickedPlayer ?: return
        playerColors[lastPlayer] = null
        lastClickedPlayer = null
        lastClickedPlayerTime = SimpleTimeMark.farPast()

        EntityUtils.getEntityByID(lastPlayer)?.let {
            if (it is EntityLivingBase) {
                it.setColor(LorenzColor.DARK_GRAY, colorInHand)
            }
        }
    }

    enum class CakeColor(val id: String, val color: LorenzColor) {
        PINK("SLICE_OF_STRAWBERRY_SHORTCAKE", LorenzColor.LIGHT_PURPLE),
        BLUE("SLICE_OF_BLUEBERRY_CAKE", LorenzColor.BLUE),
        YELLOW("SLICE_OF_CHEESECAKE", LorenzColor.YELLOW),
        GREEN("SLICE_OF_GREEN_VELVET_CAKE", LorenzColor.GREEN),
        RED("SLICE_OF_RED_VELVET_CAKE", LorenzColor.RED),
        ;

        val internalName = id.toInternalName()
    }
}
