package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.mining.CustomBlockMineEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MineshaftPityDisplay {
    private val config get() = SkyHanniMod.feature.mining.mineshaftPityDisplay

    var minedBlocks = mutableMapOf<OreType, Int>()

    var display = listOf<Renderable>()

    const val MAX_COUNTER = 2000

    @SubscribeEvent
    fun onCustomBlockMine(event: CustomBlockMineEvent) {
        if (!isEnabled()) return
        val oreType = event.originalOre.oreType ?: return
        if (oreType == OreType.HARD_STONE) return
        minedBlocks.addOrPut(oreType, 1)
        update()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (MiningNotifications.mineshaftSpawn.matches(event.message)) {
            val resultList = mutableListOf<String>()
            resultList.add("Mineshaft Pity Counter: ${calculateCounter()}/$MAX_COUNTER")
            resultList.add("Blocks mined:")
            minedBlocks.forEach {
                resultList.add("    ${it.key.oreName}: ${it.value}")
            }
            val string = resultList.joinToString("\n")
            OSUtils.copyToClipboard(string)
            ChatUtils.chat("Copied shaft spawn info to clipboard!")
            resetCounter()
        }
    }

    private fun calculateCounter(): Int {
        if (minedBlocks.isEmpty()) return 0
        var counter = 0
        minedBlocks.forEach { counter += it.key.shaftMultiplier * it.value }
        return counter
    }

    fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val counter = calculateCounter()

        add(
            Renderable.horizontalContainer(
                listOf(
                    Renderable.itemStack(ItemStack(Blocks.packed_ice)),
                    Renderable.string("§3Pity Counter: §e$counter§6/§e$MAX_COUNTER")
                ),
                spacing = 2
            )
        )

        val multiplierList = listOf(2, 4, 8)

        val list = mutableListOf<Renderable>()
        multiplierList.forEach { multiplier ->
            val iconList = mutableListOf<Renderable>()
            OreType.entries.filter { it.shaftMultiplier == multiplier }.forEach {
                iconList.add(Renderable.itemStack(it.item))
            }
            val icons = Renderable.horizontalContainer(iconList, -5)
            val blocksNeeded = MAX_COUNTER - counter
            list.add(Renderable.horizontalContainer(listOf(icons, Renderable.string("$blocksNeeded")), 2))
        }
        add(Renderable.verticalContainer(list))
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return
        config.position.renderRenderables(
            listOf(Renderable.verticalContainer(display, 2)),
            posLabel = "Mineshaft Pity Counter"
        )
    }

    fun resetCounter() {
        minedBlocks = mutableMapOf()
        update()
    }

    fun isEnabled() = MiningAPI.inGlacialTunnels() && config.enable
}
