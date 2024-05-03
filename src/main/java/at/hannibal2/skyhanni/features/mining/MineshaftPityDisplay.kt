package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.mining.CustomBlockMineEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
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
            resultList.add("Mineshaft Pity Counter: ${MAX_COUNTER - calculateCounter()}/$MAX_COUNTER")
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
        if (minedBlocks.isEmpty()) return MAX_COUNTER
        var counter = MAX_COUNTER
        minedBlocks.forEach { counter -= it.key.shaftMultiplier * it.value }
        return counter
    }

    fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val counter = calculateCounter()

        add(Renderable.string("§3Pity Counter: §e${MAX_COUNTER - counter}§6/§e$MAX_COUNTER"))
        add(Renderable.string("§3Chance: §e1§6/§e$counter §7(§b${((1.0 / counter) * 100).addSeparators()}%§7)"))

        add(Renderable.string("§3Needed to pity:"))
        val multiplierList = listOf(2, 4, 8)

        val list = mutableListOf<Renderable>()
        multiplierList.forEach { multiplier ->
            val iconList = mutableListOf<Renderable>()
            OreType.entries.filter { it.shaftMultiplier == multiplier }.forEach {
                iconList.add(Renderable.itemStack(it.item, xSpacing = 0))
            }
            val icons = Renderable.horizontalContainer(iconList)
            val blocksNeeded = counter / multiplier
            list.add(
                Renderable.horizontalContainer(
                    listOf(
                        Renderable.placeholder(10, icons.height),
                        icons,
                        Renderable.string("§b$blocksNeeded")
                    ),
                    2
                )
            )
        }
        add(Renderable.verticalContainer(list))

        /*OreType.entries.filter { it.shaftMultiplier != 0 }.forEach {
            add(Renderable.horizontalContainer(
                listOf(
                    Renderable.itemStack(it.item),
                    Renderable.string(it.oreName)
                ),
                spacing = 2
            ))
        }*/
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
