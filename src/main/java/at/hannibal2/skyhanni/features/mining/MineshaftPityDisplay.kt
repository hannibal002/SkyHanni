package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HotmData
import at.hannibal2.skyhanni.data.HotmReward
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.mining.OreMinedEvent
import at.hannibal2.skyhanni.features.mining.MineshaftPityDisplay.PityBlock.Companion.getPity
import at.hannibal2.skyhanni.features.mining.OreType.Companion.getOreType
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.CollectionUtils.removeFirst
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.Text
import at.hannibal2.skyhanni.utils.chat.Text.hover
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.block.BlockStone
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MineshaftPityDisplay {
    private val config get() = SkyHanniMod.feature.mining.mineshaftPityDisplay

    private val profileStorage get() = ProfileStorageData.profileSpecific?.mining?.mineshaft

    private var minedBlocks: List<PityData>
        get() = profileStorage?.blocksBroken ?: mutableListOf()
        set(value) {
            profileStorage?.blocksBroken = value
        }

    private var PityBlock.efficientMiner: Int
        get() = minedBlocks.firstOrNull { it.pityBlock == this }?.efficientMiner ?: 0
        set(value) {
            minedBlocks.firstOrNull {
                ChatUtils.chat("is value: ${it.pityBlock} == $this")
                it.pityBlock == this
            }?.let {
                ChatUtils.chat("setting value: ${it.efficientMiner} = $value")
                it.efficientMiner = value
            } ?: run {
                ChatUtils.chat("adding value: $this")
                minedBlocks = minedBlocks.editCopy { add(PityData(this@efficientMiner, 0, value)) }
            }
        }

    private var PityBlock.blocksBroken: Int
        get() = minedBlocks.firstOrNull { it.pityBlock == this }?.blocksBroken ?: 0
        set(value) {
            minedBlocks.firstOrNull {
                ChatUtils.chat("is value: ${it.pityBlock} == $this")
                it.pityBlock == this
            }?.let {
                ChatUtils.chat("setting value: ${it.efficientMiner} = $value")
                it.blocksBroken = value
            } ?: run {
                ChatUtils.chat("adding value: $this")
                minedBlocks = minedBlocks.editCopy { add(PityData(this@blocksBroken, value, 0)) }
            }
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
    fun onOreMined(event: OreMinedEvent) {
        if (!isEnabled()) return
        ChatUtils.chat("a")

        val oreType = event.originalOre.getOreType()
        val effMinerBlocks = event.extraBlocks.map { (block, amount) ->
            block.getOreType() to amount
        }.removeFirst { it.first == oreType } // extraBlocks also contains the original block mined
        ChatUtils.chat("b")

        oreType?.addMined(1, false)
        effMinerBlocks.forEach { (ore, amount) ->
            ore?.addMined(amount, true)
        }
        update()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (MiningNotifications.mineshaftSpawn.matches(event.message)) {
            val pityCounter = calculateCounter()
            val chance = calculateChance(pityCounter)
            val counterUntilPity = MAX_COUNTER - pityCounter
            val totalBlocks = PityBlock.entries.sumOf { it.blocksBroken + it.efficientMiner }
            //val totalBlocks = minedBlocks.sumOf { it.efficientMiner + it.blocksBroken }

            mineshaftTotalBlocks += totalBlocks
            mineshaftTotalCount++

            val message = event.message + " §e($counterUntilPity)"

            val hover = mutableListOf<String>()
            hover.add("§7Blocks mined: §e$totalBlocks")
            hover.add("§7Pity Counter: §e$pityCounter")
            hover.add(
                "§7Chance: " +
                    "§e1§6/§e${chance.round(1)} " +
                    "§7(§b${((1.0 / chance) * 100).addSeparators()}%§7)"
            )
            minedBlocks.forEach {
                hover.add(
                    "    §7${it.pityBlock.displayName} mined: " +
                        "§e${it.blocksBroken.addSeparators()} §6[${it.efficientMiner.addSeparators()}] " +
                        "§e(${it.pityBlock.getPity().addSeparators()}/${counterUntilPity.addSeparators()})"
                )
            }
            hover.add("")
            hover.add(
                "§7Average Blocks/Mineshaft: " +
                    "§e${(mineshaftTotalBlocks / mineshaftTotalCount.toDouble()).addSeparators()}"
            )

            if (!lastMineshaftSpawn.isFarPast()) {
                hover.add("")
                hover.add("§7Time since Last Mineshaft: §e${lastMineshaftSpawn.passedSince().format()}")
            }
            lastMineshaftSpawn = SimpleTimeMark.now()

            resetCounter()

            val newComponent = Text.text(message) {
                this.hover = Text.multiline(hover)
            }

            if (config.modifyChatMessage) event.chatComponent = newComponent
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        update()
    }

    private fun calculateCounter(): Double {
        val counter = MAX_COUNTER.toDouble()
        if (minedBlocks.isEmpty()) return counter
        val difference = minedBlocks.sumOf { it.pityBlock.getPity() }
        return counter - difference
    }

    // if the chance is 1/1500, it will return 1500
    private fun calculateChance(counter: Double): Double {
        val surveyorPercent = HotmData.SURVEYOR.getReward()[HotmReward.MINESHAFT_CHANCE] ?: 0.0
        val peakMountainPercent = HotmData.PEAK_OF_THE_MOUNTAIN.getReward()[HotmReward.MINESHAFT_CHANCE] ?: 0.0
        val chance = counter / (1 + surveyorPercent / 100 + peakMountainPercent / 100)
        return chance
    }

    private fun update() {
        val pityCounter = calculateCounter()
        val chance = calculateChance(pityCounter)
        val counterUntilPity = MAX_COUNTER - pityCounter

        val blocksToPityList = buildList {
            val multipliers = PityBlock.entries.map { it.multiplier }.toSet().sorted()

            multipliers.forEach { multiplier ->
                val iconsList = PityBlock.entries
                    .filter { it.multiplier == multiplier }
                    .map { Renderable.itemStack(it.displayItem) }
                add(
                    Renderable.horizontalContainer(
                        listOf(
                            Renderable.horizontalContainer(iconsList),
                            Renderable.string("§b${pityCounter / multiplier}")
                        ), 2
                    )
                )
            }
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
                Renderable.string("§3Chance: §e1§6/§e${chance.round(1)} §7(§b${((1.0 / chance) * 100).addSeparators()}%§7)")
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
        profileStorage?.blocksBroken = mutableListOf()
        update()
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.MINESHAFT) return
        resetCounter()
    }

    fun isEnabled() = MiningAPI.inGlacialTunnels() && config.enabled

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

    private fun OreType.addMined(amount: Int, efficientMiner: Boolean) {
        val block = PityBlock.getByOreTypeOrNull(this) ?: return
        ChatUtils.chat("a")
        if (efficientMiner) block.efficientMiner += amount
        else block.blocksBroken += amount
    }

    data class PityData(val pityBlock: PityBlock, var blocksBroken: Int, var efficientMiner: Int)

    enum class PityBlock(
        val displayName: String,
        val oreTypes: List<OreType>,
        val multiplier: Int,
        val displayItem: ItemStack
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
        ),

        ;

        companion object {

            fun getByOreTypeOrNull(oreType: OreType) = entries.firstOrNull { oreType in it.oreTypes }

            fun PityBlock.getPity() = (blocksBroken + efficientMiner / 2.0) * multiplier
        }
    }
}
