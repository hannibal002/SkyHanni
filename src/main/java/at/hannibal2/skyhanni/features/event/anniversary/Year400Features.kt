package at.hannibal2.skyhanni.features.event.anniversary

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.EntityLivingBase
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object Year400Features {
    private val config get() = SkyHanniMod.feature.event.anniversaryCelebration400

    private var colorInHand: CakeColor? = null
    private val playerColors = mutableMapOf<Mob, CakeColor?>()
    private var lastClickedPlayer: Mob? = null
    private var lastClickedPlayerTime = SimpleTimeMark.farPast()

    private val chatGroup = RepoPattern.group("event.anniversary-celebration.400.team-finder")

    /**
     * REGEX-TEST: §r§8[§2172§8] §b_EliteNefarious §9⛃§r
     * REGEX-TEST: §r§8[§f72§8] §bBee181204 §a⛃§r
     * REGEX-TEST: §r§8[§729§8] §ajeeerzy §e⛃§7♲§r
     */
    private val playerColorNametagPattern by chatGroup.pattern(
        "player-color-nametag",
        ".* §(?<color>.)⛃(?:§7♲)?§r",
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

        if (new != null) {
            updateAllPlayers(new)
        } else {
            for (mob in MobData.players) {
                mob.highlight(null)
            }
        }
    }

    private fun updateAllPlayers(colorInHand: CakeColor) {
        val correctColor = colorInHand.color

        val correctPlayers = playerColors.filter { it.value == colorInHand }.keys

        for (mob in MobData.players) {
            val color = if (mob in correctPlayers) correctColor else LorenzColor.DARK_GRAY
            mob.setColor(color, colorInHand)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        for (mob in MobData.players) {
            if (mob !in playerColors) {
                addPlayer(mob, mob.baseEntity)
            }
        }
    }

    private fun addPlayer(mob: Mob, entity: EntityLivingBase) {
        val displayName = entity.displayName.formattedText
        val colorCode = playerColorNametagPattern.matchMatcher(displayName) {
            group("color")
        } ?: run {
            if (colorInHand != null) {
                mob.setColor(LorenzColor.DARK_GRAY, null)
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
        playerColors[mob] = cakeColor

        val colorInHand = colorInHand ?: return
        val lorenzColor = if (colorInHand == cakeColor) colorInHand.color else LorenzColor.DARK_GRAY
        mob.setColor(lorenzColor, colorInHand)
    }

    private fun Mob.setColor(color: LorenzColor, currentHand: CakeColor?) {
        highlight(color.toColor().addAlpha(1)) { config.teamFinder && colorInHand == currentHand }
    }

    @HandleEvent
    fun onRealPlayerDeSpawnEvent(event: MobEvent.DeSpawn.Player) {
        playerColors.remove(event.mob)
    }

    @HandleEvent
    fun onPunch(event: EntityClickEvent) {
        val entity = event.clickedEntity
        if (colorInHand == null) return
        if (entity !is EntityOtherPlayerMP) return
        if (entity.isNpc()) return

        val mob = entity.mob ?: return
        lastClickedPlayer = mob
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

        lastPlayer.setColor(LorenzColor.DARK_GRAY, colorInHand)
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
