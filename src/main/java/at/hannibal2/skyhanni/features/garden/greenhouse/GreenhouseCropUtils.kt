package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getBlockState
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

object GreenhouseCropUtils {

    private val REGEX_DASH_SPACE = "[- ]".toRegex()

    private val SOUL_SAND_CROPS = setOf("Ashwreath", "Witherbloom", "Cindershade", "Zombud", "Phantomleaf", "Fire")
    private val SAND_CROPS = setOf("Blastberry", "Magic Jellybean", "All-in Aloe", "Glasscorn")
    private val END_STONE_CROPS = setOf("Chorus Fruit", "Timestalk")

    private val CROP_ID_OVERRIDES = mapOf(
        "do_not_eat_shroom" to "Do-not-eat-shroom",
        "plantboy_advance" to "PlantBoy Advance",
        "all_in_aloe" to "All-in Aloe"
    )

    private val mutationsSkullCache = mutableMapOf<String, ItemStack>()

    fun getSurface(name: String): Block {
        val vanillaCrop = getVanillaCropOrNull(name) ?: return when (name) {
            in SOUL_SAND_CROPS -> Blocks.SOUL_SAND
            "Veilshroom" -> Blocks.MYCELIUM
            in SAND_CROPS -> Blocks.SAND
            in END_STONE_CROPS -> Blocks.END_STONE
            else -> Blocks.FARMLAND
        }
        return vanillaCrop.cropSurface
    }

    fun parseCropId(id: String): String {
        return CROP_ID_OVERRIDES[id] ?: id.allLettersFirstUppercase()
    }

    fun getVanillaCropOrNull(name: String): CropType? = when (name) {
        "Wheat Seeds" -> CropType.WHEAT
        "Pumpkin Seeds" -> CropType.PUMPKIN
        "Melon Seeds" -> CropType.MELON
        else -> CropType.getByNameOrNull(name)
    }

    fun getCropBlock(name: String): BlockState? {
        val vanillaBlock = getVanillaCropOrNull(name)?.getBlockState()
        if (vanillaBlock != null) return vanillaBlock

        return when (name) {
            "Fire" -> Blocks.FIRE
            "Dead Plant", "Dead Plants" -> Blocks.DEAD_BUSH
            else -> null
        }?.defaultBlockState()
    }

    fun getMutationSkull(mutation: String): ItemStack = mutationsSkullCache.getOrPut(mutation) {
        val uppercaseName = if (mutation == "Fertilized Jerryseed") "JERRYFLOWER" else mutation.uppercase()
        val textureRepoName = uppercaseName.replace(REGEX_DASH_SPACE, "_")
        val texture = SkullTextureHolder.getTexture(textureRepoName)
        ItemUtils.createSkull(uppercaseName, "d0606454-04da-44c6-9bd4-5f3a4d8b388a", texture)
    }

    fun blockToCropName(block: Block): String? = when (block) {
        Blocks.FIRE -> "Fire"
        Blocks.DEAD_BUSH -> "Dead Plant"
        else -> block.defaultBlockState().getCropType()?.cropName
    }
}
