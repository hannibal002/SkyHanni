package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.data.hotx.HotmReward
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.StringUtils.insert
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.distribute
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.BlockCompat
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderAndScale
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.WrappedStringRenderable.Companion.wrappedText
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import java.awt.Color
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object BlockStrengthGuide {

    private enum class DisplayOres(private val iconDel: () -> ItemStack, val oreBlocks: Set<OreBlock>) {
        VANILLA_ORES(
            { ItemStack(Blocks.redstone_block) },
            setOf(
                OreBlock.COAL_ORE,
                OreBlock.IRON_ORE,
                OreBlock.LAPIS_ORE,
                OreBlock.GOLD_ORE,
                OreBlock.EMERALD_ORE,
                OreBlock.DIAMOND_ORE,
                OreBlock.QUARTZ_ORE,
                OreBlock.REDSTONE_ORE,
            ),
        ),
        PURE_ORES(
            { ItemStack(Blocks.gold_block) },
            setOf(
                OreBlock.PURE_COAL,
                OreBlock.PURE_IRON,
                OreBlock.PURE_LAPIS,
                OreBlock.PURE_GOLD,
                OreBlock.PURE_EMERALD,
                OreBlock.PURE_DIAMOND,
                OreBlock.PURE_QUARTZ,
                OreBlock.PURE_REDSTONE,
            ),
        ),
        TITANIUM(
            { BlockCompat.createSmoothDiorite() },
            setOf(OreBlock.HIGH_TIER_MITHRIL),
        ),
        GRAY_MITHRIL(
            { ColoredBlockCompat.GRAY.createWoolStack() },
            setOf(OreBlock.LOW_TIER_MITHRIL),
        ),
        GREEN_MITHRIL(
            { ItemStack(Blocks.prismarine) },
            setOf(OreBlock.MID_TIER_MITHRIL),
        ),
        BLUE_MITHRIL(
            { ColoredBlockCompat.LIGHT_BLUE.createWoolStack() },
            setOf(OreBlock.HIGH_TIER_MITHRIL),
        ),
        TUNGSTEN_UMBER(
            { ItemStack(Blocks.clay) },
            setOf(
                OreBlock.LOW_TIER_UMBER,
                OreBlock.MID_TIER_UMBER,
                OreBlock.HIGH_TIER_UMBER,
                OreBlock.LOW_TIER_TUNGSTEN_MINESHAFT,
                OreBlock.LOW_TIER_TUNGSTEN_MINESHAFT,
                OreBlock.HIGH_TIER_TUNGSTEN,
            ),
        ),
        GLACITE(
            { ItemStack(Blocks.packed_ice) },
            setOf(OreBlock.GLACITE),
        ),
        OBSIDIAN(
            { ItemStack(Blocks.obsidian) },
            setOf(OreBlock.OBSIDIAN),
        ),
        RUBY(
            { ColoredBlockCompat.RED.createGlassStack() },
            setOf(OreBlock.RUBY),
        ),
        NUCLEUS_GEMSTONES(
            { ColoredBlockCompat.LIGHT_BLUE.createGlassStack() },
            setOf(OreBlock.AMBER, OreBlock.AMETHYST, OreBlock.JADE, OreBlock.SAPPHIRE),
        ),
        OPAL(
            { ColoredBlockCompat.WHITE.createGlassStack() },
            setOf(OreBlock.OPAL),
        ),
        TOPAZ(
            { ColoredBlockCompat.YELLOW.createGlassStack() },
            setOf(OreBlock.TOPAZ),
        ),
        JASPER(
            { ColoredBlockCompat.MAGENTA.createGlassStack() },
            setOf(OreBlock.JASPER),
        ),
        TUNNEL_GEMSTONES(
            { ColoredBlockCompat.BLACK.createGlassStack() },
            setOf(OreBlock.ONYX, OreBlock.PERIDOT, OreBlock.CITRINE, OreBlock.AQUAMARINE),
        ),
        HARD_STONE(
            { ItemStack(Blocks.stone) },
            setOf(OreBlock.HARD_STONE_HOLLOWS, OreBlock.HARD_STONE_TUNNELS, OreBlock.HARD_STONE_MINESHAFT),
        ),
        COBBLE_STONE(
            { ItemStack(Blocks.cobblestone) },
            setOf(OreBlock.COBBLESTONE),
        ),
        STONE(
            { ItemStack(Blocks.stone) },
            setOf(OreBlock.STONE),
        ),
        SULPHUR(
            { ItemStack(Blocks.sponge) },
            setOf(OreBlock.SULPHUR),
        ),
        NETHERRACK(
            { ItemStack(Blocks.netherrack) },
            setOf(OreBlock.NETHERRACK),
        ),
        END_STONE(
            { ItemStack(Blocks.end_stone) },
            setOf(OreBlock.END_STONE),
        );

        val icon by lazy(this.iconDel)

        val hoverText get() = oreBlocks.joinToString(", ") { it.name.allLettersFirstUppercase() }

        fun renderable(rawSpeed: SpeedClass): Renderable {
            val ore = oreBlocks.first()

            val speed = rawSpeed.base + when (ore.category) {
                OreCategory.DWARVEN_METAL -> rawSpeed.metal
                OreCategory.GEMSTONE -> rawSpeed.gemstone
                OreCategory.ORE -> rawSpeed.ore
                OreCategory.BLOCK -> rawSpeed.block
            }

            val ticks = ore.miningTicks(speed)
            val time = ticks.ticks.format(showMilliSeconds = true).transformIf({ ticks % 2 == 1 }) {
                this.insert(this.length - 1, '5')
            }

            val (progressBar, percentLine, untilNextLine) = processProgressData(ticks, speed, ore)

            return Renderable.hoverTips(
                Renderable.horizontal(
                    Renderable.item(icon),
                    progressBar,
                    Renderable.text("$ticks"),
                    spacing = 0,
                ),
                tips = buildList<Renderable> {
                    val blockName = name.allLettersFirstUppercase()
                    addString(blockName)
                    add(Renderable.placeholder(0, 5))

                    addString("§3Ticks: §f$ticks §7(§b$time§7)")
                    addExtraInfo("It takes you §b$time§7 to break $blockName.")
                    addExtraInfo("Only correct if the server has 20 TPS.")

                    addString("§3Your: §6${speed.toInt().addSeparators()} ${SkyblockStat.MINING_SPEED.icon}")
                    addExtraInfo("You have §6${speed.toInt().addSeparators()} mining speed")
                    addExtraInfo("when breaking $blockName :)")

                    untilNextLine?.let {
                        addString(it)
                        addExtraInfo("The mining speed you need more")
                        addExtraInfo("to mine $blockName in §b${ticks - 1}")
                    }
                    addString(percentLine)
                    add(Renderable.placeholder(0, 5))

                    addString("§3Block Strength: §f${ore.strength.addSeparators()}")
                    addExtraInfo("This defines the \"thoughness\" of a block.")
                    addExtraInfo("A higher number means it takes longer")
                    addExtraInfo("to break $blockName.")

                    addString("§3Softcap: §6${ore.speedSoftCap.addSeparators()} ${SkyblockStat.MINING_SPEED.icon}")
                    addExtraInfo("Having more than §6${ore.speedSoftCap.addSeparators()} mining speed")
                    addExtraInfo("will §c§lNOT §r§7break $blockName any faster.")

                    addString("§3Instant: §6${ore.speedForInstantMine.addSeparators()} ${SkyblockStat.MINING_SPEED.icon}")
                    addExtraInfo("Once you reach §6${ore.speedForInstantMine.addSeparators()} mining speed")
                    addExtraInfo("you break $blockName in §e§lone click§7.")

                    add(Renderable.placeholder(0, 5))
                    addString("§3Category: §f${ore.category.toString().allLettersFirstUppercase()}")
                    addString("§3Blocks in that group:")
                    add(Renderable.wrappedText(hoverText, setWidth = 200))

                    if (!showExtraInfos) {
                        add(Renderable.placeholder(0, 5))
                        addString("§eHold control-key to show extra infos!")
                    }
                },
            )
        }
    }

    private fun processProgressData(ticks: Int, speed: Double, ore: OreBlock): Triple<Renderable, String, String?> {
        val progressBar: Renderable
        val percentLine: String
        val untilNextLine: String?
        when (ticks) {
            1 -> {
                progressBar = Renderable.progressBar(1.0, InstantMineColor, InstantMineColor, width = 100)
                percentLine = "§6Instant Mine"
                untilNextLine = null
            }

            4 -> {
                progressBar = Renderable.progressBar(
                    speed.fractionOf(ore.speedForInstantMine),
                    SoftCapColor,
                    InstantMineColor,
                    width = 100,
                )
                percentLine = "§a${speed.fractionOf(ore.speedForInstantMine).times(100).roundTo(1)}% §fto Instant Mine"
                untilNextLine = "§6${
                    ceil(ore.speedForInstantMine - speed).toInt().addSeparators()
                } ${SkyblockStat.MINING_SPEED.icon} §cmissing §fto §b1 §ftick"
            }

            else -> {
                progressBar = Renderable.progressBar(
                    speed.fractionOf(ore.speedSoftCap),
                    BaseColor,
                    SoftCapColor,
                    width = 100,
                )
                percentLine = "§a${speed.fractionOf(ore.speedSoftCap).times(100).roundTo(1)}% §fto Soft Cap"
                val next = ticks - 1
                val nextTicksFormat = "tick".pluralize(next)
                untilNextLine = "§6${
                    ceil(ore.speedNeededForNextTick(speed)).toInt().addSeparators()
                } ${SkyblockStat.MINING_SPEED.icon} §cmissing §fto §b$next §f$nextTicksFormat"
            }
        }
        return Triple(progressBar, percentLine, untilNextLine)
    }

    private fun MutableList<Renderable>.addExtraInfo(info: String) {
        if (showExtraInfos) {
            addString("  §7$info")
        }
    }

    private val InstantMineColor = Color(0x1E, 0x90, 0xFF)
    private val SoftCapColor = Color(0x00, 0xFA, 0x9A)
    private val BaseColor = Color(0xFF, 0x63, 0x37)

    private var showExtraInfos = false

    private lateinit var speed: SpeedClass

    private var inMineshaft = false

    // TODO Dwarven Equip (Needs a Equipment API) , Goblin Pet and Mithril Pet (need the PetAPI v2)
    private fun requestSpeed(): SpeedClass {
        val itemInHand = InventoryUtils.getItemInHand()
        speed = SpeedClass(
            base = (
                SkyblockStat.MINING_SPEED.lastKnownValue ?: 0.0
                ) + if (inMineshaft) HotmData.EAGER_ADVENTURER.getReward()[HotmReward.MINING_SPEED] ?: 0.0 else 0.0,
            metal = HotmData.STRONG_ARM.getReward()[HotmReward.MINING_SPEED] ?: 0.0,
            gemstone = (
                HotmData.PROFESSIONAL.getReward()[HotmReward.MINING_SPEED] ?: 0.0
                ) + (
                itemInHand?.getHypixelEnchantments()?.get("lapidary")?.times(20.0) ?: 0.0
                ) + when (itemInHand?.getInternalNameOrNull()?.asString()) {
                "GEMSTONE_DRILL_1", "GEMSTONE_DRILL_2", "GEMSTONE_DRILL_3", "GEMSTONE_DRILL_4" -> 800.0
                else -> 0.0
            },
            ore = 0.0,
            block = 0.0,
        )

        return speed
    }

    private data class SpeedClass(
        val base: Double,
        val metal: Double,
        val gemstone: Double,
        val ore: Double,
        val block: Double,
    ) {
        fun toRenderables() = listOf(
            base.toInt().addSeparators(),
            gemstone.toInt().addSeparators(),
            metal.toInt().addSeparators(),
        ).map { Renderable.text("§6$it", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER) }
    }

    private val headerHeaderLine = listOf("Base", "Gemstone", "Metal").map {
        Renderable.text(
            text = it,
            scale = 0.75,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
    }

    private var display: Renderable? = null

    private fun createDisplay(): Renderable {
        requestSpeed()
        return Renderable.drawInsideRoundedRectWithOutline(
            Renderable.vertical(
                Renderable.vertical(
                    createHeader(),
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                ),
                Renderable.table(
                    createTableContent(), 5, 3,
                ),
                spacing = 8,
            ),
            color = LorenzColor.GRAY.addOpacity(180),
            topOutlineColor = Color(0, 0, 0, 200).rgb,
            bottomOutlineColor = Color(0, 0, 0, 200).rgb,
            borderOutlineThickness = 3,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
    }

    private fun createTableContent(): List<List<Renderable>> = DisplayOres.entries.map {
        it.renderable(speed)
    }.distribute(3)

    private fun createHeader(): List<Renderable> = listOf(
        Renderable.text(
            SkyblockStat.MINING_SPEED.iconWithName,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        ),
        Renderable.horizontal(
            Renderable.table(
                listOf(
                    headerHeaderLine,
                    speed.toRenderables(),
                ),
                xSpacing = 5,
            ),
            Renderable.clickable(
                Renderable.text(
                    "§${if (inMineshaft) 'b' else '7'}Mineshaft",
                    scale = 0.5,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                ),
                onLeftClick = {
                    inMineshaft = !inMineshaft
                    display = createDisplay()
                },
            ),
            spacing = 3,
        ),
    )

    private var shouldBlockSHMenu = false
        set(value) {
            field = value
            if (!value) {
                display = null
            } else {
                lastSet = SimpleTimeMark.now()
            }
        }

    private var sbMenuOpened = false

    private var lastSet = SimpleTimeMark.farPast()
    private var lastRunCommand = SimpleTimeMark.farPast()

    fun onCommand() {
        when {
            RiftApi.inRift() -> "in the rift"
            DungeonApi.inDungeon() -> "in dungeons"
            KuudraApi.inKuudra -> "in kuudra"
            else -> null
        }?.let {
            ChatUtils.userError("The Block Strengh Guide does not work $it!")
            return

        }
        lastRunCommand = SimpleTimeMark.now()
        shouldBlockSHMenu = true
        sbMenuOpened = false
        HypixelCommands.skyblockMenu()
    }

    @HandleEvent
    fun onGuiContainerPreDraw(event: GuiContainerEvent.PreDraw) {
        if (!shouldBlockSHMenu) return

        if (!sbMenuOpened) {
            if (lastRunCommand.passedSince() < 2.seconds) {
                sbMenuOpened = SkyblockStat.MINING_SPEED.lastAssignment.passedSince() < 1.0.seconds
                Renderable.text(
                    "Loading...",
                    scale = 2.0,
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                    verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                ).renderXYAligned(0, 0, event.gui.width, event.gui.height)
                event.cancel()
            } else {
                ErrorManager.logErrorStateWithData(
                    "could not load mining data for /shblockstrengh command",
                    "opened /sbmenu and found no mining speed in the next 2s",
                    "island" to SkyBlockUtils.currentIsland,
                    "graph area" to SkyBlockUtils.graphArea,
                    "scoreboard area" to SkyBlockUtils.scoreboardArea,
                    "location" to LocationUtils.playerLocation(),
                    betaOnly = true,
                )
            }
            return
        }
        event.cancel()

        val display = display ?: createDisplay().also {
            display = it
        }

        Renderable.withMousePosition(event.mouseX, event.mouseY) {
            display.renderAndScale(0, 0, event.gui.width, event.gui.height, 20)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "SkyBlock Menu") return
        DelayedRun.runDelayed(100.milliseconds) {
            if (lastRunCommand.passedSince() < 3.seconds) {
                lastRunCommand = SimpleTimeMark.farPast()
            }
        }
    }

    @HandleEvent(SkyHanniTickEvent::class)
    fun onTick() {
        val now = KeyboardManager.isModifierKeyDown()
        if (showExtraInfos != now) {
            showExtraInfos = now
            display = createDisplay()
        }
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        if (!sbMenuOpened) return
        shouldBlockSHMenu = false
    }

    @HandleEvent(IslandChangeEvent::class)
    fun onIslandChange() {
        shouldBlockSHMenu = false
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shblockstrength") {
            description = "Shows how many ticks you need to break any block with your mining speed."
            category = CommandCategory.MAIN
            aliases = listOf("shminingspeed")
            simpleCallback { onCommand() }
        }
    }
}
