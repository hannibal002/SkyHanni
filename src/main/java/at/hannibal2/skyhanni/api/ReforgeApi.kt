package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuReforgeJson
import at.hannibal2.skyhanni.data.model.SkyblockStatList
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EnumUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object ReforgeApi {
    var reforges: List<Reforge> = emptyList()
        private set(value) {
            field = value
            basicReforges = value.filterNot { it.isReforgeStone }
            reforgeStones = value.filter { it.isReforgeStone }
        }

    var basicReforges: List<Reforge> = emptyList()
        private set

    var reforgeStones: List<Reforge> = emptyList()
        private set

    enum class ReforgeType(vararg val itemCategories: ItemCategory) {
        SWORD(ItemCategory.SWORD, ItemCategory.LONGSWORD, ItemCategory.GAUNTLET),
        BOW(ItemCategory.BOW, ItemCategory.SHORT_BOW),
        ARMOR(
            ItemCategory.CARNIVAL_MASK, ItemCategory.HELMET,
            ItemCategory.CHESTPLATE, ItemCategory.LEGGINGS, ItemCategory.BOOTS,
        ),
        CHESTPLATE(ItemCategory.CHESTPLATE),
        HELMET(ItemCategory.HELMET),
        CLOAK(ItemCategory.CLOAK),
        BELT(ItemCategory.BELT),
        AXE(ItemCategory.AXE),
        FARMING_TOOL(ItemCategory.FARMING_TOOL),
        PICKAXE(ItemCategory.PICKAXE, ItemCategory.DRILL, ItemCategory.GAUNTLET),
        EQUIPMENT(
            ItemCategory.NECKLACE, ItemCategory.CLOAK, ItemCategory.BELT,
            ItemCategory.GLOVES, ItemCategory.BRACELET,
        ),
        ROD(ItemCategory.FISHING_ROD),
        SWORD_AND_ROD(ItemCategory.SWORD, ItemCategory.GAUNTLET, ItemCategory.LONGSWORD, ItemCategory.FISHING_ROD),
        SPECIAL_ITEMS,
        VACUUM(ItemCategory.VACUUM),
    }

    sealed interface ReforgeData {
        val name: String
        val type: ReforgeType
        val stats: Map<LorenzRarity, SkyblockStatList>
        val reforgeStone: NeuInternalName?
        val specialItems: List<NeuInternalName>?
        val reforgeAbility: Map<LorenzRarity, String>
    }

    data class ReforgeDataImpl(
        override val name: String,
        override val type: ReforgeType,
        override val stats: Map<LorenzRarity, SkyblockStatList>,
        override val reforgeStone: NeuInternalName? = null,
        override val specialItems: List<NeuInternalName>? = null,
        override val reforgeAbility: Map<LorenzRarity, String> = emptyMap(),
    ) : ReforgeData

    class Reforge(
        private val data: ReforgeDataImpl,
        val nbtModifier: String,
        val costs: Map<LorenzRarity, Long> = emptyMap(),
    ) : ReforgeData by data {
        val isReforgeStone = reforgeStone != null
        val rawReforgeStoneName = reforgeStone?.itemNameWithoutColor

        fun isValid(itemStack: ItemStack) = isValid(itemStack.getItemCategoryOrNull(), itemStack.getInternalName())
        fun isValid(itemCategory: ItemCategory?, internalName: NeuInternalName) = when (type) {
            ReforgeType.SPECIAL_ITEMS -> specialItems?.contains(internalName) ?: false
            else -> itemCategory in type.itemCategories
        }

        override fun equals(other: Any?) = other is Reforge && data == other.data
        override fun hashCode() = data.hashCode()
        override fun toString() = "Reforge $name"
    }

    private fun NeuReforgeJson.mapReforge() = Reforge(
        data = ReforgeDataImpl(
            name = reforgeName,
            type = EnumUtils.enumValueOf<ReforgeType>(itemType.first),
            stats = reforgeStats.orEmpty(),
            reforgeStone = internalName,
            specialItems = itemType.second.takeIf { it.isNotEmpty() },
            reforgeAbility = reforgeAbility,
        ),
        nbtModifier = nbtModifier,
        costs = reforgeCosts.orEmpty(),
    )

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val reforgeStoneData = event.getConstant<Map<String, NeuReforgeJson>>("reforgestones").values
        val reforgeData = event.getConstant<Map<String, NeuReforgeJson>>("reforges").values
        reforges = (reforgeStoneData + reforgeData).map { it.mapReforge() }
    }
}
