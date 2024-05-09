package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.mining.CustomBlockMineEvent
import at.hannibal2.skyhanni.features.mining.OreType.Companion.getOreType
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.createHoverableChat
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.block.BlockStone
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MineshaftPityDisplay {
    private val config get() = SkyHanniMod.feature.mining.mineshaftPityDisplay

    private val profileStorage get() = ProfileStorageData.profileSpecific?.mining?.mineshaft

    private var minedBlocks: MutableMap<PityBlocks, Int>
        get() = profileStorage?.blocksBroken ?: mutableMapOf()
        set(value) {
            profileStorage?.blocksBroken = value
        }

    private var mineshaftTotalBlocks: Long
        get() = profileStorage?.mineshaftTotalBlocks ?: 0L
        set(value) {
            profileStorage?.mineshaftTotalBlocks = value
        }

    private var mineshaftTotalCount: Int
        get() = profileStorage?.mineshaftTotalCount ?: 0
        set(value) {
            profileStorage?.mineshaftTotalCount = value
        }

    var lastMineshaftSpawn = SimpleTimeMark.farPast()

    var display = listOf<Renderable>()

    private const val MAX_COUNTER = 2000

    @SubscribeEvent
    fun onCustomBlockMine(event: CustomBlockMineEvent) {
        if (!isEnabled()) return
        val oreType = event.originalOre.getOreType() ?: return
        ChatUtils.debug("Mined block: ${oreType.oreName}")
        val pityBlock = PityBlocks.entries.firstOrNull {
            it.oreTypes.contains(oreType)
        } ?: return
        minedBlocks.addOrPut(pityBlock, 1)
        update()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (MiningNotifications.mineshaftSpawn.matches(event.message)) {
            val pityCounter = calculateCounter()
            val counterUntilPity = MAX_COUNTER - pityCounter
            val totalBlocks = minedBlocks.values.sum()

            mineshaftTotalBlocks += totalBlocks
            mineshaftTotalCount++

            val message = "§5§lWOW! §r§aYou found a §r§bGlacite Mineshaft §r§aportal! §e($counterUntilPity)"

            val hover = mutableListOf<String>()
            hover.add("§7Blocks mined: §e$totalBlocks")
            hover.add("§7Pity Counter: §e$pityCounter")
            minedBlocks.forEach {
                hover.add("    §7${it.key.displayName} mined: §e${it.value} (${(it.value * it.key.multiplier)}/$counterUntilPity)")
            }
            hover.add("")
            hover.add("§7Average Blocks/Mineshaft: §e${(mineshaftTotalBlocks / mineshaftTotalCount.toDouble()).addSeparators()}")

            if (!lastMineshaftSpawn.isFarPast()) {
                hover.add("")
                hover.add("§7Time since Last Mineshaft: §e${lastMineshaftSpawn.passedSince().format()}")
            }
            lastMineshaftSpawn = SimpleTimeMark.now()

            resetCounter()

            if (config.modifyChatMessage) event.chatComponent = createHoverableChat(message, hover)
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        update()
    }

    private fun calculateCounter(): Int {
        if (minedBlocks.isEmpty()) return MAX_COUNTER
        var counter = MAX_COUNTER
        minedBlocks.forEach { counter -= it.key.multiplier * it.value }
        return counter
    }

    private fun update() {
        val pityCounter = calculateCounter()
        val counterUntilPity = MAX_COUNTER - pityCounter

        val multipliers = PityBlocks.entries.map { it.multiplier }.toSet().sorted()
        val blocksToPityList = mutableListOf<Renderable>()

        multipliers.forEach { multiplier ->
            val iconsList = PityBlocks.entries
                .filter { it.multiplier == multiplier }
                .map { Renderable.itemStack(it.item) }
            blocksToPityList.add(
                Renderable.horizontalContainer(
                    listOf(
                        Renderable.horizontalContainer(iconsList),
                        Renderable.string("§b${pityCounter / multiplier}")
                    ), 2
                )
            )
        }

        val neededToPityRenderable = Renderable.verticalContainer(
            listOf(
                Renderable.string("§3Needed to pity:"),
                Renderable.horizontalContainer(
                    listOf(
                        Renderable.placeholder(10, 0),
                        Renderable.verticalContainer(blocksToPityList)
                    )
                )
            )
        )

        val map = buildMap {
            put(MineshaftPityLines.TITLE, Renderable.string("§9§lMineshaft Pity Counter"))
            put(MineshaftPityLines.COUNTER, Renderable.string("§3Pity Counter: §e$counterUntilPity§6/§e$MAX_COUNTER"))
            put(
                MineshaftPityLines.CHANCE,
                Renderable.string("§3Chance: §e1§6/§e$pityCounter §7(§b${((1.0 / pityCounter) * 100).addSeparators()}%§7)")
            )
            put(MineshaftPityLines.NEEDED_TO_PITY, neededToPityRenderable)
            put(
                MineshaftPityLines.TIME_SINCE_MINESHAFT,
                Renderable.string("§3Last Mineshaft: §e${lastMineshaftSpawn.passedSince().format()}")
            )
            put(
                MineshaftPityLines.AVERAGE_BLOCKS_MINESHAFT,
                Renderable.string("§3Average Blocks/Mineshaft: §e${(mineshaftTotalBlocks / mineshaftTotalCount.toDouble()).addSeparators()}")
            )
        }

        display = config.mineshaftPityLines.filter { it.shouldDisplay() }.mapNotNull { map[it] }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        display.ifEmpty { update() }
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

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.MINESHAFT) return
        resetCounter()
    }

    fun isEnabled() = MiningAPI.inGlacialTunnels() && config.enable

    enum class MineshaftPityLines(private val display: String, val shouldDisplay: () -> Boolean = { true }) {
        TITLE("§3§lMineshaft Pity Counter"),
        COUNTER("§3Counter: §e561§6/§e2000"),
        CHANCE("§3Chance: §e1§6/§e1439 §7(§b0.069%§7)"),
        NEEDED_TO_PITY("§3Needed to pity:\n§7   <blocks>"),
        TIME_SINCE_MINESHAFT("§3Last Mineshaft: §e21m 5s", { !lastMineshaftSpawn.isFarPast() }),
        AVERAGE_BLOCKS_MINESHAFT("§3Average Blocks/Mineshaft: §e361.5", { mineshaftTotalCount != 0 })

        ;

        override fun toString(): String {
            return display
        }
    }

    enum class PityBlocks(
        val displayName: String,
        val oreTypes: List<OreType>,
        val multiplier: Int,
        val item: ItemStack
    ) {
        MITHRIL(
            "Mithril",
            listOf(OreType.MITHRIL),
            1,
            ItemStack(Blocks.wool, 1, EnumDyeColor.LIGHT_BLUE.metadata)
        ),

        GEMSTONE(
            "Gemstone",
            listOf(
                OreType.RUBY, OreType.AMBER, OreType.AMETHYST, OreType.JADE,
                OreType.SAPPHIRE, OreType.TOPAZ, OreType.JASPER, OreType.OPAL,
                OreType.AQUAMARINE, OreType.CITRINE, OreType.ONYX, OreType.PERIDOT
            ),
            4,
            ItemStack(Blocks.stained_glass, 1, EnumDyeColor.BLUE.metadata)
        ),
        GLACITE(
            "Glacite",
            listOf(OreType.GLACITE),
            4,
            ItemStack(Blocks.packed_ice)
        ),
        TUNGSTEN(
            "Tungsten",
            listOf(OreType.TUNGSTEN),
            4,
            ItemStack(Blocks.clay)
        ),
        UMBER(
            "Umber",
            listOf(OreType.UMBER),
            4,
            ItemStack(Blocks.red_sandstone)
        ),

        TITANIUM(
            "Titanium",
            listOf(OreType.TITANIUM),
            8,
            ItemStack(Blocks.stone, 1, BlockStone.EnumType.DIORITE_SMOOTH.metadata)
        )
    }
}
