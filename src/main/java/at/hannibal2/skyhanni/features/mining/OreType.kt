package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import net.minecraft.block.state.IBlockState

enum class OreType(
    internalName: String,
    vararg oreBlocks: OreBlock,
    oreName: String? = null
) {
    MITHRIL(
        "MITHRIL_ORE",
        OreBlock.LOW_TIER_MITHRIL, OreBlock.MID_TIER_MITHRIL, OreBlock.HIGH_TIER_MITHRIL,
    ),
    TITANIUM(
        "TITANIUM_ORE",
        OreBlock.TITANIUM,
    ),
    COBBLESTONE(
        "COBBLESTONE",
        OreBlock.STONE, OreBlock.COBBLESTONE,
    ),
    COAL(
        "COAL",
        OreBlock.COAL_ORE, OreBlock.PURE_COAL,
    ),
    IRON(
        "IRON_INGOT",
        OreBlock.IRON_ORE, OreBlock.PURE_IRON,
    ),
    GOLD(
        "GOLD_INGOT",
        OreBlock.GOLD_ORE, OreBlock.PURE_GOLD,
    ),
    LAPIS(
        "INK_SACK-4",
        OreBlock.LAPIS_ORE, OreBlock.PURE_LAPIS,
        oreName = "Lapis Lazuli"
    ),
    REDSTONE(
        "REDSTONE",
        OreBlock.REDSTONE_ORE, OreBlock.PURE_REDSTONE,
    ),
    EMERALD(
        "EMERALD",
        OreBlock.EMERALD_ORE, OreBlock.PURE_EMERALD,
    ),
    DIAMOND(
        "DIAMOND",
        OreBlock.DIAMOND_ORE, OreBlock.PURE_DIAMOND,
    ),
    NETHERRACK(
        "NETHERRACK",
        OreBlock.NETHERRACK,
    ),
    QUARTZ(
        "QUARTZ",
        OreBlock.QUARTZ_ORE,
        oreName = "Nether Quartz"
    ),
    GLOWSTONE(
        "GLOWSTONE_DUST",
        OreBlock.GLOWSTONE,
    ),
    MYCELIUM(
        "MYCEL",
        OreBlock.MYCELIUM,
    ),
    RED_SAND(
        "SAND-1",
        OreBlock.RED_SAND,
    ),
    SULPHUR(
        "SULPHUR_ORE",
        OreBlock.SULPHUR,
    ),
    GRAVEL(
        "GRAVEL",
        OreBlock.GRAVEL,
    ),
    END_STONE(
        "ENDER_STONE",
        OreBlock.END_STONE,
    ),
    OBSIDIAN(
        "OBSIDIAN",
        OreBlock.OBSIDIAN,
    ),
    HARD_STONE(
        "HARD_STONE",
        OreBlock.HARD_STONE_HOLLOWS, OreBlock.HARD_STONE_TUNNELS, OreBlock.HARD_STONE_MINESHAFT,
    ),
    RUBY(
        "ROUGH_RUBY_GEM",
        OreBlock.RUBY,
    ),
    AMBER(
        "ROUGH_AMBER_GEM",
        OreBlock.AMBER,
    ),
    AMETHYST(
        "ROUGH_AMETHYST_GEM",
        OreBlock.AMETHYST,
    ),
    JADE(
        "ROUGH_JADE_GEM",
        OreBlock.JADE,
    ),
    SAPPHIRE(
        "ROUGH_SAPPHIRE_GEM",
        OreBlock.SAPPHIRE,
    ),
    TOPAZ(
        "ROUGH_TOPAZ_GEM",
        OreBlock.TOPAZ,
    ),
    JASPER(
        "ROUGH_JASPER_GEM",
        OreBlock.JASPER,
    ),
    OPAL(
        "ROUGH_OPAL_GEM",
        OreBlock.OPAL,
    ),
    AQUAMARINE(
        "ROUGH_AQUAMARINE_GEM",
        OreBlock.AQUAMARINE,
    ),
    CITRINE(
        "ROUGH_CITRINE_GEM",
        OreBlock.CITRINE,
    ),
    ONYX(
        "ROUGH_ONYX_GEM",
        OreBlock.ONYX,
    ),
    PERIDOT(
        "ROUGH_PERIDOT_GEM",
        OreBlock.PERIDOT,
    ),
    UMBER(
        "UMBER",
        OreBlock.LOW_TIER_UMBER, OreBlock.MID_TIER_UMBER, OreBlock.HIGH_TIER_UMBER,
    ),
    TUNGSTEN(
        "TUNGSTEN",
        OreBlock.LOW_TIER_TUNGSTEN_TUNNELS, OreBlock.LOW_TIER_TUNGSTEN_MINESHAFT, OreBlock.HIGH_TIER_TUNGSTEN,
    ),
    GLACITE(
        "GLACITE",
        OreBlock.GLACITE,
    ),
    ;

    val oreName = oreName ?: toFormattedName()

    val oreBlocks = oreBlocks.toSet()

    val internalName: NeuInternalName = internalName.toInternalName()

    fun isType(oreBlock: OreBlock): Boolean = oreBlock in oreBlocks

    fun isGemstone(): Boolean = this in gemstones

    companion object {

        private val gemstones = setOf(
            RUBY, AMBER, AMETHYST, JADE,
            SAPPHIRE, TOPAZ, JASPER, OPAL,
            AQUAMARINE, CITRINE, ONYX, PERIDOT,
        )

        fun IBlockState.isOreType(oreType: OreType): Boolean {
            for (oreBlock in oreType.oreBlocks) {
                if (oreBlock !in MiningApi.currentAreaOreBlocks) continue
                if (oreBlock.checkBlock(this)) {
                    return true
                }
            }
            return false
        }

        fun OreBlock.getOreType(): OreType? {
            return OreType.entries.firstOrNull { it.isType(this) }
        }
    }
}
