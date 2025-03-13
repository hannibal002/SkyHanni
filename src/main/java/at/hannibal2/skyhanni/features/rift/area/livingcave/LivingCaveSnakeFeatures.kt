package at.hannibal2.skyhanni.features.rift.area.livingcave

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.features.rift.area.livingcave.snake.LivingCaveSnake
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.drainForEach
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import java.util.concurrent.ConcurrentLinkedQueue

@SkyHanniModule
object LivingCaveSnakeFeatures {
    private val config get() = RiftApi.config.area.livingCave.snakeHelper
    private val snakes = mutableListOf<LivingCaveSnake>()

    private val originalBlocks = mutableMapOf<LorenzVec, Block>()

    var selectedSnake: LivingCaveSnake? = null

    private val FROZEN_WATER_PUNGI = "FROZEN_WATER_PUNGI".toInternalName()

    // TODO maybe move this in repo
    private val pickaxes = setOf(
        "SELF_RECURSIVE_PICKAXE",
        "ANTI_SENTIENT_PICKAXE",
        "EON_PICKAXE",
        "CHRONO_PICKAXE",
    ).toInternalNames()

    private var currentRole: Role? = null

    private val addedList = ConcurrentLinkedQueue<LorenzVec>()
    private val removedList = ConcurrentLinkedQueue<LorenzVec>()

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!isEnabled()) return
        val location = event.location
        val old = event.oldState.block
        val new = event.newState.block

        if (new == Blocks.lapis_block) {
            originalBlocks[location] = old
            addedList.add(location)
        }
        if (originalBlocks[location] == new) {
            originalBlocks.remove(location)
            removedList.add(location)
        }
    }

    private fun addSnakeBlock(location: LorenzVec) {
        val snake = fixCollisions(findNearbySnakeHeads(location))
        if (snake == null) {
            snakes.add(LivingCaveSnake(mutableListOf(location)))
        } else {
            // hypixel is sometimes funny
            if (location in snake.blocks) return

            snake.blocks.add(0, location)
            snake.lastAddTime = SimpleTimeMark.now()
            snake.invalidHeadSince = null
        }
    }

    @HandleEvent
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        currentRole = when (event.newItem) {
            FROZEN_WATER_PUNGI -> Role.CALM
            in pickaxes -> Role.BREAK
            else -> null
        }
    }

    private var lastClickedBlock: LorenzVec? = null

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return

        lastClickedBlock = event.position

        val snake = snakes.find { event.position in it.blocks } ?: return

        selectedSnake = snake
        if (event.clickType == ClickType.RIGHT_CLICK) {
            if (InventoryUtils.itemInHandId == FROZEN_WATER_PUNGI)
                snake.lastCalmTime = SimpleTimeMark.now()
        } else {
            if (InventoryUtils.itemInHandId in pickaxes) {
                snake.lastHitTime = SimpleTimeMark.now()
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return

        if (LorenzUtils.debug && Minecraft.getMinecraft().thePlayer.isSneaking && snakes.isNotEmpty()) {
            snakes.clear()
            ChatUtils.debug("Snakes reset.", replaceSameMessage = true)
            return
        }

        addedList.drainForEach {
            addSnakeBlock(it)
        }

        removedList.drainForEach { location ->
            snakes.filter { location in it.blocks }.forEach {
                if (it.removeSnakeBlock(location, lastClickedBlock)) {
                    snakes.remove(it)
                }
            }
        }

        snakes.removeIf {
            val invalidShape = it.invalidShape()
            val invalidHead = it.invalidHead()
            if (invalidShape && LorenzUtils.debug) ChatUtils.chat("LivingCaveSnake remove because of invalid shape")
            if (invalidHead && LorenzUtils.debug) ChatUtils.chat("LivingCaveSnake remove because of invalid head")
            invalidShape || invalidHead
        }
        snakes.forEach { it.tick() }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (currentRole == null) return

        currentRole?.let { role ->
            snakes.forEach { it.render(event, role) }
        }
    }

    // sqrt(3) =~ 1.73
    private fun findNearbySnakeHeads(location: LorenzVec): List<LivingCaveSnake> =
        snakes.filter { it.blocks.isNotEmpty() && it.head.distance(location) < 1.74 }
            .sortedBy { it.head.distance(location) }

    private fun fixCollisions(found: List<LivingCaveSnake>): LivingCaveSnake? {
        if (found.size <= 1) return found.firstOrNull()

        val filtered = found.filter { it.state != LivingCaveSnake.State.CALM }
        return if (filtered.size < found.size) {
            filtered.firstOrNull()
        } else {
            found.firstOrNull()
        }
    }

    private fun isEnabled() = (RiftApi.inLivingCave() || RiftApi.inLivingStillness()) && config.highlight

    enum class Role {
        BREAK,
        CALM,
    }
}
