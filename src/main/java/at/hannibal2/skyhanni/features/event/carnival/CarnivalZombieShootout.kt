package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawHitbox
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

@SkyHanniModule
object CarnivalZombieShootout {

    private val config get() = SkyHanniMod.feature.event.carnival.zombieShootout

    private var display = emptyList<Renderable>()
    private var lantern: Pair<LorenzVec, SimpleTimeMark>? = null
    private var started = false

    private val patternGroup = RepoPattern.group("event.carnival")

    /**
     * REGEX-TEST: [NPC] Carnival Cowboy: Good luck, pal!
     */
    private val startPattern by patternGroup.pattern(
        "shootout.start",
        "\\[NPC] Carnival Cowboy: Good luck, pal!",
    )

    /**
     * REGEX-TEST:                              Zombie Shootout
     */
    private val endPattern by patternGroup.pattern(
        "shootout.end",
        "^ {29}Zombie Shootout\$",
    )

    enum class ZombieType(val points: Int, val helmet: String, val color: Color) {
        LEATHER(30, "Leather Cap", Color(165, 42, 42)),  //Brown
        IRON(50, "Iron Helmet", Color(192, 192, 192)),  //Silver
        GOLD(80, "Golden Helmet", Color(255, 215, 0)),  //Gold
        DIAMOND(120, "Diamond Helmet", Color(185, 242, 255)) //Diamond
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled() || !started || !config.coloredHitboxes) return

        lantern?.let {
            event.drawWaypointFilled(it.first, Color.RED, minimumAlpha = 1.0f)
        }

        val nearbyZombies = EntityUtils.getEntitiesNextToPlayer<EntityZombie>(50.0).mapNotNull { zombie ->
                if (zombie.health <= 0) return@mapNotNull null
                val armor = zombie.getCurrentArmor(3) ?: return@mapNotNull null
                val type = toType(armor) ?: return@mapNotNull null
                zombie to type
            }.toMap()

        val drawZombies = when (config.highestOnly) {
            false -> nearbyZombies
            true -> {
                val drawType = nearbyZombies.values.maxByOrNull { it.points } ?: return
                nearbyZombies.filterValues { it == drawType }
            }
        }

        drawZombies.forEach { (zombie, type) ->
            val entity = EntityUtils.getEntityByID(zombie.entityId) ?: return@forEach
            val color = type.color

            event.drawHitbox(
                entity.entityBoundingBox.expand(0.1, 0.05, 0.0).offset(0.0, 0.05, 0.0),
                3,
                color,
                false,
            )
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || !started || !config.lanternTimer) return

        lantern?.let { (_, time) ->
            val lamp = ItemStack(Blocks.redstone_lamp)
            val timer = (6 - (SimpleTimeMark.now() - time).inPartialSeconds).round(1)
            val prefix = when (timer) {
                in 4.0..6.0 -> "§a"
                in 2.0..4.0 -> "§e"
                else -> "§c"
            }

            val content = Renderable.horizontalContainer(
                listOf(
                    Renderable.itemStack(lamp),
                    Renderable.string("§6Disappears in $prefix${timer}s"),
                ),
                1,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )

            config.lanternPosition.renderRenderables(listOf(content), posLabel = "Lantern Timer")
        }
    }

    @SubscribeEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!isEnabled() || !started) return

        val old = event.old
        val new = event.new

        lantern = when {
            old == "redstone_lamp" && new == "lit_redstone_lamp" -> event.location to SimpleTimeMark.now()
            old == "lit_redstone_lamp" && new == "redstone_lamp" -> null
            else -> lantern
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message.removeColor()

        if (startPattern.matches(message)) {
            started = true
        } else if (endPattern.matches(message)) {
            started = false
        }
    }

    private fun toType(item: ItemStack) = ZombieType.entries.find { it.helmet == item.displayName }

    private fun isEnabled() = config.enabled && LorenzUtils.skyBlockArea == "Carnival"
}
