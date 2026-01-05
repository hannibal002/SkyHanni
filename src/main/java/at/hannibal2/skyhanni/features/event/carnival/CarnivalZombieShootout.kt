package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.findHealthReal
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.getEntityHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawHitbox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@SkyHanniModule
object CarnivalZombieShootout {

    private val config get() = SkyHanniMod.feature.event.carnival.zombieShootout

    private data class ShootoutLamp(var pos: LorenzVec, var time: SimpleTimeMark)
    private data class ShootoutZombie(val entity: Zombie, val type: ZombieType)

    private var content = Renderable.empty()
    private var drawZombies = listOf<ShootoutZombie>()
    private val zombieTimes = mutableMapOf<ShootoutZombie, SimpleTimeMark>()
    private var maxType = ZombieType.LEATHER
    private var lamp: ShootoutLamp? = null
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
        " {29}Zombie Shootout",
    )

    enum class ZombieType(val points: Int, val helmet: Item, val color: Color, val lifetime: Duration) {
        LEATHER(30, Items.LEATHER_HELMET, Color(165, 42, 42), 8.seconds), // Brown
        IRON(50, Items.IRON_HELMET, Color(192, 192, 192), 7.seconds), // Silver
        GOLD(80, Items.GOLDEN_HELMET, Color(255, 215, 0), 6.seconds), // Gold
        DIAMOND(120, Items.DIAMOND_HELMET, Color(44, 214, 250), 5.seconds) // Diamond
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled() || (!config.coloredHitboxes && !config.coloredLines && !config.zombieTimer)) return

        if (config.zombieTimer) event.renderZombieTimer()
        if (config.coloredHitboxes) event.renderHitBoxes()
        if (config.coloredLines) event.renderLines()
    }

    private fun SkyHanniRenderWorldEvent.renderZombieTimer() {
        val zombiesToRemove = mutableListOf<ShootoutZombie>()

        for ((zombie, time) in zombieTimes) {
            val lifetime = zombie.type.lifetime
            val timer = lifetime - time.passedSince()

            if (config.highestOnly && zombie.type != maxType) continue

            if (timer > 0.seconds) {
                val entity = EntityUtils.getEntityByID(zombie.entity.id) ?: continue
                val isSmall = (entity as? Zombie)?.isBaby ?: false

                val skips = lifetime / 3
                val prefix = determinePrefix(timer, lifetime, lifetime - skips, lifetime - skips * 2)
                val height = if (isSmall) entity.bbHeight / 2 else entity.bbHeight

                drawDynamicText(
                    entity.getLorenzVec().add(-0.5, height + 0.5, -0.5),
                    "$prefix${timer.toString(DurationUnit.SECONDS, 1)}",
                    scaleMultiplier = 1.25,
                )
            } else {
                if (timer < (-2).seconds) {
                    zombiesToRemove.add(zombie)
                }
            }
        }

        zombiesToRemove.forEach { zombieTimes.remove(it) }
    }

    private fun SkyHanniRenderWorldEvent.renderHitBoxes() {
        lamp?.let {
            drawWaypointFilled(it.pos, Color.RED, minimumAlpha = 1.0f)
        }

        for ((zombie, type) in drawZombies) {
            val entity = EntityUtils.getEntityByID(zombie.id) ?: continue
            val isSmall = (entity as? Zombie)?.isBaby ?: false

            val boundingBox = entity.boundingBox

            drawHitbox(
                boundingBox.inflate(0.1, 0.05, 0.0).move(0.0, 0.05, 0.0),
                type.color,
                lineWidth = 3,
                depth = false,
            )
        }
    }

    private fun SkyHanniRenderWorldEvent.renderLines() = lamp?.let {
        draw3DLine(
            exactPlayerEyeLocation(),
            it.pos.add(0.5, 0.5, 0.5),
            Color.RED,
            3,
            false,
        )
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || !config.lampTimer) return

        config.lampPosition.renderRenderable(content, posLabel = "Lantern Timer")
    }

    @HandleEvent(ServerBlockChangeEvent::class)
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!isEnabled() || !started) return

        val blockOld = event.old
        val blockNew = event.new
        if (blockOld == "redstone_lamp" && blockNew == "redstone_lamp") {
            val old = event.oldState.getValue(BlockStateProperties.LIT)
            val new = event.newState.getValue(BlockStateProperties.LIT)
            lamp = when {
                !old && new -> ShootoutLamp(event.location, SimpleTimeMark.now())
                old && !new -> null
                else -> lamp
            }
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.enabled || HypixelData.skyBlockArea != "Carnival") return

        val message = event.cleanMessage

        if (startPattern.matches(message)) {
            started = true
        } else if (endPattern.matches(message)) {
            started = false
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled() || (!config.coloredHitboxes && !config.zombieTimer && !config.lampTimer) || !event.isMod(2)) return

        if (config.coloredHitboxes || config.zombieTimer) {
            updateZombies()
        }

        if (config.lampTimer) {
            content = lamp?.let {
                updateContent(it.time)
            } ?: Renderable.empty()
        }
    }

    private fun updateZombies() {
        val nearbyZombies = getZombies()
        maxType = nearbyZombies.maxByOrNull { it.type.points }?.type ?: ZombieType.LEATHER
        val maxZombies = nearbyZombies.filter { it.type == maxType }

        drawZombies = when {
            config.coloredHitboxes && config.highestOnly -> maxZombies
            config.coloredHitboxes -> nearbyZombies
            else -> emptyList()
        }

        if (config.zombieTimer) {
            for (zombie in nearbyZombies) {
                zombieTimes.putIfAbsent(zombie, SimpleTimeMark.now())
            }
        }
    }

    private fun updateContent(time: SimpleTimeMark): Renderable {
        val lamp = ItemStack(Blocks.REDSTONE_LAMP)
        val timer = 6.seconds - time.passedSince()
        val prefix = determinePrefix(timer, 6.seconds, 4.seconds, 2.seconds)

        return Renderable.horizontal(
            Renderable.item(lamp),
            Renderable.text("§6Disappears in $prefix$timer"),
            spacing = 1,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        )
    }

    private fun getZombies() =
        EntityUtils.getEntitiesNextToPlayer<Zombie>(50.0).mapNotNull { zombie ->
            if (zombie.findHealthReal() <= 0) return@mapNotNull null
            val helmet = zombie.getEntityHelmet() ?: return@mapNotNull null
            val type = toType(helmet) ?: run {
                ErrorManager.logErrorStateWithData(
                    "Could not identify Zombie Shootout type",
                    "zombie type for zombie entity helmet is null",
                    "helmet" to helmet,
                    "helmet.displayName" to helmet.hoverName.formattedTextCompatLeadingWhiteLessResets(),
                    "helmet.item" to helmet.item,
                    "helmet.unlocalizedName" to helmet.item.descriptionId,
                )
                return@mapNotNull null
            }
            ShootoutZombie(zombie, type)
        }.toList()

    private fun determinePrefix(timer: Duration, good: Duration, mid: Duration, bad: Duration) =
        when (timer) {
            in mid..good -> "§a"
            in bad..mid -> "§e"
            else -> "§c"
        }

    private fun toType(item: ItemStack) = ZombieType.entries.find { it.helmet == item.item }

    private fun isEnabled() = config.enabled && HypixelData.skyBlockArea == "Carnival" && started
}
