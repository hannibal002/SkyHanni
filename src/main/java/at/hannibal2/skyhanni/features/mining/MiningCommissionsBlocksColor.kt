package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.block.BlockCarpet
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object MiningCommissionsBlocksColor {

    private val config get() = SkyHanniMod.feature.mining.commissionsBlocksColor

    var enabled = false
    var active = false

    private val patternGroup = RepoPattern.group("mining.commissions")

    /**
     * REGEX-TEST: §a§lCITRINE GEMSTONE COLLECTOR §r§eCommission Complete! Visit the King §r§eto claim your rewards!
     */
    private val commissionCompletePattern by patternGroup.pattern(
        "complete",
        "§a§l(?<name>.*) §r§eCommission Complete! Visit the King §r§eto claim your rewards!"
    )

    var color: EnumDyeColor = EnumDyeColor.RED

    private fun glass() = { state: IBlockState, result: Boolean ->
        if (result) {
            state.withProperty(BlockCarpet.COLOR, color)
        } else {
            state.withProperty(BlockCarpet.COLOR, EnumDyeColor.GRAY)
        }
    }

    private fun block() = { _: IBlockState, result: Boolean ->
        val wool = Blocks.wool.defaultState
        if (result) {
            wool.withProperty(BlockCarpet.COLOR, color)
        } else {
            wool.withProperty(BlockCarpet.COLOR, EnumDyeColor.GRAY)
        }
    }

    val aquamarine = Blocks.stained_glass
    val aquamarine_2 = Blocks.stained_glass_pane

    val citrine = Blocks.stained_glass
    val citrine_2 = Blocks.stained_glass_pane

    val glacite = Blocks.packed_ice

    val onyx = Blocks.stained_glass
    val onyx_2 = Blocks.stained_glass_pane

    val umber = Blocks.hardened_clay
    val umber_2 = Blocks.stained_hardened_clay
    val umber_3 = Blocks.double_stone_slab2 // red sandstone

    val peridot = Blocks.stained_glass
    val peridot_2 = Blocks.stained_glass_pane

    val tungston = Blocks.cobblestone
    val tungston_2 = Blocks.stone_stairs
    val tungston_3 = Blocks.clay

    val mithril = Blocks.stained_hardened_clay
    val mithril_2 = Blocks.wool
    val mithril_3 = Blocks.prismarine

    private var oldSneakState = false

    private var dirty = false
    private var forceDirty = false

    private var inDwarvenMines = false
    private var inCrystalHollows = false
    private var inGlaciteArea = false

    // TODO Commissin API
    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        for (block in MiningBlock.entries) {
            val tabList = " §r§f${block.displayName}: "
            val newValue = event.tabList.any { it.startsWith(tabList) && !it.contains("DONE") }
            if (block.highlight != newValue) {
                if (newValue && block in ignoredTabListCommissions) continue
                block.highlight = newValue
                dirty = true
            }
        }
    }

    private val ignoredTabListCommissions = TimeLimitedSet<MiningBlock>(5.seconds)

    // TODO Commissin API
    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!enabled) return
        commissionCompletePattern.matchMatcher(event.message) {
            val name = group("name")
            val block = MiningBlock.entries.find { it.displayName.equals(name, ignoreCase = true) } ?: return
            block.highlight = false
            dirty = true
            ignoredTabListCommissions.add(block)
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (LorenzUtils.lastWorldSwitch.passedSince() > 4.seconds) {
            inGlaciteArea = MiningAPI.inGlaciteArea() && !IslandType.MINESHAFT.isInIsland()
            inDwarvenMines = IslandType.DWARVEN_MINES.isInIsland() &&
                !(inGlaciteArea || HypixelData.skyBlockArea.equalsOneOf("Dwarven Base Camp", "Fossil Research Center"))
            inCrystalHollows = IslandType.CRYSTAL_HOLLOWS.isInIsland() && HypixelData.skyBlockArea != "Crystal Nucleus"
        }

        // TODO add dwarven mines support
//         val newEnabled = (inDwarvenMines || inCrystalHollows || inGlaciteArea) && config.enabled
        val newEnabled = (inCrystalHollows || inGlaciteArea) && config.enabled
        var reload = false
        if (newEnabled != enabled) {
            enabled = newEnabled
            reload = true
            if (enabled) {
                active = true
            }
        }

        if (enabled) {
            if (config.sneakQuickToggle.get()) {
                val sneaking = Minecraft.getMinecraft().thePlayer.isSneaking
                if (sneaking != oldSneakState) {
                    oldSneakState = sneaking
                    if (oldSneakState) {
                        active = !active
                        dirty = true
                    }
                }
            }
            if (dirty) {
                reload = true
            }
        }

        if (reload) {
            Minecraft.getMinecraft().renderGlobal.loadRenderers()
            dirty = false
        }
    }

    @SubscribeEvent
    fun onConfigReload(event: ConfigLoadEvent) {
        color = config.color.get().toDyeColor()
        config.sneakQuickToggle.onToggle {
            oldSneakState = false
            if (!active) {
                active = true
                dirty = true
            }
        }
        config.color.onToggle {
            color = config.color.get().toDyeColor()
            dirty = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        enabled = false
        inDwarvenMines = false
        inCrystalHollows = false
        inGlaciteArea = false
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Mining Commissions Blocks Color")
        if (!enabled) {
            event.addIrrelevant("not enabled")
            return
        }

        event.addData {
            add("inDwarvenMines: $inDwarvenMines")
            add("inCrystalHollows: $inCrystalHollows")
            add("inGlaciteArea: $inGlaciteArea")
            add("active: $active")
        }
    }

    enum class MiningBlock(
        val displayName: String,
        val onCheck: (IBlockState) -> Boolean,
        val onColor: (IBlockState, Boolean) -> IBlockState,
        var highlight: Boolean = false,
        val checkIsland: () -> Boolean,
    ) {
        // Dwarven Mines
        MITHRIL(
            "Mithril Everywhere",
            onCheck = { state ->
                (state.block == mithril && state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.CYAN)) ||
                    (state.block == mithril_2 && (state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.GRAY) ||
                        state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.LIGHT_BLUE))) ||
                    state.block == mithril_3
            },
            onColor = block(),
            checkIsland = { true }
        ),

        // Crystal Hollows
        AMBER(
            "Amber Gemstone Collector",
            onCheck = { state ->
                (state.block == aquamarine || state.block == aquamarine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.ORANGE)
            },
            onColor = glass(),
            checkIsland = { !inDwarvenMines }
        ),
        TOPAZ(
            "Topaz Gemstone Collector",
            onCheck = { state ->
                (state.block == aquamarine || state.block == aquamarine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.YELLOW)
            },
            onColor = glass(),
            checkIsland = { !inDwarvenMines }
        ),
        AMETHYST(
            "Amethyst Gemstone Collector",
            onCheck = { state ->
                (state.block == aquamarine || state.block == aquamarine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.PURPLE)
            },
            onColor = glass(),
            checkIsland = { !inDwarvenMines }
        ),
        RUBY(
            "Ruby Gemstone Collector",
            onCheck = { state ->
                (state.block == aquamarine || state.block == aquamarine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.RED)
            },
            onColor = glass(),
            checkIsland = { !inDwarvenMines }
        ),
        JADE(
            "Jade Gemstone Collector",
            onCheck = { state ->
                (state.block == aquamarine || state.block == aquamarine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.LIME)
            },
            onColor = glass(),
            checkIsland = { !inDwarvenMines }
        ),
        SAPPHIRE(
            "Sapphire Gemstone Collector",
            onCheck = { state ->
                (state.block == aquamarine || state.block == aquamarine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.LIGHT_BLUE)
            },
            onColor = glass(),
            checkIsland = { !inDwarvenMines }
        ),
        HARD_STONE(
            "Hardstone Miner",
            onCheck = { state ->
                state.block == glacite
            },
            onColor = block(),
            checkIsland = { !inDwarvenMines }
        ),

        // Glacite Tunnels
        GLACITE(
            "Glacite Collector",
            onCheck = { state ->
                state.block == glacite
            },
            onColor = block(),
            checkIsland = { inGlaciteArea }
        ),
        UMBER(
            "Umber Collector",
            onCheck = { state ->
                (state.block == umber || state.block == umber_3) ||
                    (state.block == umber_2 && state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.BROWN))
            },
            onColor = block(),
            checkIsland = { inGlaciteArea }
        ),
        TUNGSTON(
            "Tungsten Collector",
            onCheck = { state ->
                state.block == tungston || state.block == tungston_2 || state.block == tungston_3
            },
            onColor = block(),
            checkIsland = { inGlaciteArea }
        ),
        PERIDOT(
            "Peridot Gemstone Collector",
            onCheck = { state ->
                (state.block == peridot || state.block == peridot_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.GREEN)
            },
            onColor = glass(),
            checkIsland = { inGlaciteArea }
        ),
        AQUAMARINE(
            "Aquamarine Gemstone Collector",
            onCheck = { state ->
                (state.block == aquamarine || state.block == aquamarine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.BLUE)
            },
            onColor = glass(),
            checkIsland = { inGlaciteArea }
        ),
        CITRINE(
            "Citrine Gemstone Collector",
            onCheck = { state ->
                (state.block == citrine || state.block == citrine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.BROWN)
            },
            onColor = glass(),
            checkIsland = { inGlaciteArea }
        ),
        ONYX(
            "Onyx Gemstone Collector",
            onCheck = { state ->
                (state.block == onyx || state.block == onyx_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.BLACK)
            },
            onColor = glass(),
            checkIsland = { inGlaciteArea }
        ),
        ;
    }
}
