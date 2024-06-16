package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.utils.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import net.minecraft.block.state.IBlockState

enum class OreType(
    val oreName: String,
    private val internalNameString: String,
    val oreBlocks: List<OreBlock>,
    val internalName: NEUInternalName = internalNameString.asInternalName(),
) {
    MITHRIL(
        "Mithril",
        "MITHRIL_ORE",
        listOf(OreBlock.LOW_TIER_MITHRIL, OreBlock.MID_TIER_MITHRIL, OreBlock.HIGH_TIER_MITHRIL),
    ),
    TITANIUM(
        "Titanium",
        "TITANIUM_ORE",
        listOf(OreBlock.TITANIUM),
    ),
    COBBLESTONE(
        "Cobblestone",
        "COBBLESTONE",
        listOf(OreBlock.STONE, OreBlock.COBBLESTONE),
    ),
    COAL(
        "Coal",
        "COAL",
        listOf(OreBlock.COAL_ORE),
    ),
    IRON(
        "Iron",
        "IRON_INGOT",
        listOf(OreBlock.IRON_ORE),
    ),
    GOLD(
        "Gold",
        "GOLD_INGOT",
        listOf(OreBlock.GOLD_ORE, OreBlock.DWARVEN_GOLD),
    ),
    LAPIS(
        "Lapis Lazuli",
        "INK_SACK-4",
        listOf(OreBlock.LAPIS_ORE),
    ),
    REDSTONE(
        "Redstone",
        "REDSTONE",
        listOf(OreBlock.REDSTONE_ORE, OreBlock.DWARVEN_REDSTONE),
    ),
    EMERALD(
        "Emerald",
        "EMERALD",
        listOf(OreBlock.EMERALD_ORE, OreBlock.DWARVEN_EMERALD),
    ),
    DIAMOND(
        "Diamond",
        "DIAMOND",
        listOf(OreBlock.DIAMOND_ORE, OreBlock.DWARVEN_DIAMOND),
    ),
    NETHERRACK(
        "Netherrack",
        "NETHERRACK",
        listOf(OreBlock.NETHERRACK),
    ),
    QUARTZ(
        "Nether Quartz",
        "QUARTZ",
        listOf(OreBlock.QUARTZ_ORE),
    ),
    GLOWSTONE(
        "Glowstone",
        "GLOWSTONE_DUST",
        listOf(OreBlock.GLOWSTONE),
    ),
    MYCELIUM(
        "Mycelium",
        "MYCEL",
        listOf(OreBlock.MYCELIUM),
    ),
    RED_SAND(
        "Red Sand",
        "SAND-1",
        listOf(OreBlock.RED_SAND),
    ),
    SULPHUR(
        "Sulphur",
        "SULPHUR_ORE",
        listOf(OreBlock.SULPHUR),
    ),
    GRAVEL(
        "Gravel",
        "GRAVEL",
        listOf(OreBlock.GRAVEL),
    ),
    END_STONE(
        "End Stone",
        "ENDER_STONE",
        listOf(OreBlock.END_STONE),
    ),
    OBSIDIAN(
        "Obsidian",
        "OBSIDIAN",
        listOf(OreBlock.OBSIDIAN),
    ),
    HARD_STONE(
        "Hard Stone",
        "HARD_STONE",
        listOf(OreBlock.HARD_STONE_HOLLOWS, OreBlock.HARD_STONE_GLACIAL),
    ),
    RUBY(
        "Ruby",
        "ROUGH_RUBY_GEM",
        listOf(OreBlock.RUBY),
    ),
    AMBER(
        "Amber",
        "ROUGH_AMBER_GEM",
        listOf(OreBlock.AMBER),
    ),
    AMETHYST(
        "Amethyst",
        "ROUGH_AMETHYST_GEM",
        listOf(OreBlock.AMETHYST),
    ),
    JADE(
        "Jade",
        "ROUGH_JADE_GEM",
        listOf(OreBlock.JADE),
    ),
    SAPPHIRE(
        "Sapphire",
        "ROUGH_SAPPHIRE_GEM",
        listOf(OreBlock.SAPPHIRE),
    ),
    TOPAZ(
        "Topaz",
        "ROUGH_TOPAZ_GEM",
        listOf(OreBlock.TOPAZ),
    ),
    JASPER(
        "Jasper",
        "ROUGH_JASPER_GEM",
        listOf(OreBlock.JASPER),
    ),
    OPAL(
        "Opal",
        "ROUGH_OPAL_GEM",
        listOf(OreBlock.OPAL),
    ),
    AQUAMARINE(
        "Aquamarine",
        "ROUGH_AQUAMARINE_GEM",
        listOf(OreBlock.AQUAMARINE),
    ),
    CITRINE(
        "Citrine",
        "ROUGH_CITRINE_GEM",
        listOf(OreBlock.CITRINE),
    ),
    ONYX(
        "Onyx",
        "ROUGH_ONYX_GEM",
        listOf(OreBlock.ONYX),
    ),
    PERIDOT(
        "Peridot",
        "ROUGH_PERIDOT_GEM",
        listOf(OreBlock.PERIDOT),
    ),
    UMBER(
        "Umber",
        "UMBER",
        listOf(OreBlock.LOW_TIER_UMBER, OreBlock.HIGH_TIER_UMBER),
    ),
    TUNGSTEN(
        "Tungsten",
        "TUNGSTEN",
        listOf(OreBlock.LOW_TIER_TUNGSTEN, OreBlock.HIGH_TIER_TUNGSTEN),
    ),
    GLACITE(
        "Glacite",
        "GLACITE",
        listOf(OreBlock.GLACITE),
    ),
    ;

    companion object {

        fun IBlockState.isOreType(oreType: OreType): Boolean {
            return oreType.oreBlocks.intersect(MiningAPI.currentAreaOreBlocks)
                .any { it.checkBlock.invoke(this) }
        }

        fun OreType.isGemstone(): Boolean {
            return this.equalsOneOf(
                RUBY, AMBER, AMETHYST, JADE,
                SAPPHIRE, TOPAZ, JASPER, OPAL,
                AQUAMARINE, CITRINE, ONYX, PERIDOT,
            )
        }

        fun OreBlock.getOreType(): OreType? {
            return OreType.entries.firstOrNull {
                it.oreBlocks.contains(this)
            }
        }
    }
}
