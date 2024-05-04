package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import net.minecraft.block.BlockCarpet
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MiningBlockColors {

    var enabled = false
    var active = false

    private fun glass() = { state: IBlockState, result: Boolean ->
        if (result) {
            state.withProperty(BlockCarpet.COLOR, EnumDyeColor.PINK)
//                 state.withProperty(BlockCarpet.COLOR, EnumDyeColor.LIME)
        } else {
            state.withProperty(BlockCarpet.COLOR, EnumDyeColor.GRAY)
//                 state.withProperty(BlockCarpet.COLOR, EnumDyeColor.WHITE)
//                 if (state == Blocks.stained_glass) {
//                     Blocks.glass.defaultState
//                 } else {
//                     Blocks.glass_pane.defaultState
//                 }
        }
    }

    private fun block() = { _: IBlockState, result: Boolean ->
        val wool = Blocks.wool.defaultState
        if (result) {
            wool.withProperty(BlockCarpet.COLOR, EnumDyeColor.PINK)
//                 wool.withProperty(BlockCarpet.COLOR, EnumDyeColor.LIME)
        } else {

            wool.withProperty(BlockCarpet.COLOR, EnumDyeColor.GRAY)
//                 wool.withProperty(BlockCarpet.COLOR, EnumDyeColor.WHITE)
//                 Blocks.snow.defaultState
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


    private var oldSneakState = false

    private var dirty = false

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        for (value in GlaciteTunnelBlock.entries) {
            val newValue = event.tabList.any { it.startsWith(value.tabList) && !it.contains("DONE") }
            if (value.highlight != newValue) {
                value.highlight = newValue
                dirty = true
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (LorenzUtils.inSkyBlock) {
            val sneaking = Minecraft.getMinecraft().thePlayer.isSneaking

            if (sneaking != oldSneakState) {
                oldSneakState = sneaking
                if (oldSneakState) {
                    dirty = true
                    active = !active
                }
            }

            if (dirty) {
                Minecraft.getMinecraft().renderGlobal.loadRenderers()
                dirty = false
            }
        }

        enabled = IslandType.DWARVEN_MINES.isInIsland() && LorenzUtils.skyBlockArea.equalsOneOf(
            "Glacite Tunnels",
            "Glacite Lake"
        )
    }

    enum class GlaciteTunnelBlock(
        val tabList: String,
        val onCheck: (IBlockState) -> Boolean,
        val onColor: (IBlockState, Boolean) -> IBlockState,
        var highlight: Boolean = false,
    ) {
        GLACITE(
            " §r§fGlacite Collector: ",
            onCheck = { state ->
                state.block == glacite
            },
            onColor = block()
        ),
        UMBER(
            " §r§fUmber Collector:",
            onCheck = { state ->
                (state.block == umber || state.block == umber_3) ||
                    (state.block == umber_2 && state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.BROWN))
            },
            onColor = block()
        ),
        TUNGSTON(
            " §r§fTungsten Collector: ",
            onCheck = { state ->
                state.block == tungston || state.block == tungston_2 || state.block == tungston_3
            },
            onColor = block()
        ),
        PERIDOT(
            " §r§fPeridot Gemstone Collector: ",
            onCheck = { state ->
                (state.block == peridot || state.block == peridot_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.GREEN)
            },
            onColor = glass()
        ),
        AQUAMARINE(
            " §r§fAquamarine Gemstone Collector:",
            onCheck = { state ->
                (state.block == aquamarine || state.block == aquamarine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.BLUE)
            },
            onColor = glass()
        ),
        CITRINE(
            " §r§fCitrine Gemstone Collector: ",
            onCheck = { state ->
                (state.block == citrine || state.block == citrine_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.BROWN)
            },
            onColor = glass()
        ),
        ONYX(
            " §r§fOnyx Gemstone Collector:",
            onCheck = { state ->
                (state.block == onyx || state.block == onyx_2) &&
                    state.getValue(BlockCarpet.COLOR).equalsOneOf(EnumDyeColor.BLACK)
            },
            onColor = glass()
        ),
        ;
    }
}
