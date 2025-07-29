package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuReforgeJson
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.data.model.SkyblockStatList
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.EnumUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.item.ItemStack

@SkyHanniModule
object ReforgeApi {
    var reforgeList: List<Reforge> = emptyList()
        private set(value) {
            field = value
            nonePowerStoneReforge = value.filterNot { it.isReforgeStone }
            onlyPowerStoneReforge = value.filter { it.isReforgeStone }
        }

    var nonePowerStoneReforge: List<Reforge> = emptyList()
        private set

    var onlyPowerStoneReforge: List<Reforge> = emptyList()
        private set

    enum class ReforgeType {
        SWORD,
        BOW,
        ARMOR,
        CHESTPLATE,
        HELMET,
        CLOAK,
        BELT,
        AXE,
        HOE,
        AXE_AND_HOE,
        PICKAXE,
        EQUIPMENT,
        ROD,
        SWORD_AND_ROD,
        SPECIAL_ITEMS,
        VACUUM
    }

    class Reforge(
        val name: String,
        val type: ReforgeType,
        val stats: Map<LorenzRarity, SkyblockStatList>,
        val reforgeStone: NeuInternalName? = null,
        val specialItems: List<NeuInternalName>? = null,
        val extraProperty: Map<LorenzRarity, String> = emptyMap(),
        val costs: Map<LorenzRarity, Long>? = null,
    ) {

        val isReforgeStone = reforgeStone != null

        val rawReforgeStoneName = reforgeStone?.itemNameWithoutColor

        val lowercaseName = name.lowercase().replace('-', '_')

        fun isValid(itemStack: ItemStack) = isValid(itemStack.getItemCategoryOrNull(), itemStack.getInternalName())

        fun isValid(itemCategory: ItemCategory?, internalName: NeuInternalName) = when (type) {
            ReforgeType.SWORD -> itemCategory in setOf(
                ItemCategory.SWORD,
                ItemCategory.GAUNTLET,
                ItemCategory.LONGSWORD,
            )
            ReforgeType.BOW -> itemCategory in setOf(ItemCategory.BOW, ItemCategory.SHORT_BOW)
            ReforgeType.ARMOR -> itemCategory in setOf(
                ItemCategory.HELMET,
                ItemCategory.CARNIVAL_MASK,
                ItemCategory.CHESTPLATE,
                ItemCategory.LEGGINGS,
                ItemCategory.BOOTS,
            )
            ReforgeType.HELMET -> itemCategory == ItemCategory.HELMET
            ReforgeType.CHESTPLATE -> itemCategory == ItemCategory.CHESTPLATE
            ReforgeType.EQUIPMENT -> itemCategory in setOf(
                ItemCategory.NECKLACE,
                ItemCategory.CLOAK,
                ItemCategory.BELT,
                ItemCategory.GLOVES,
                ItemCategory.BRACELET,
            )
            ReforgeType.CLOAK -> itemCategory == ItemCategory.CLOAK
            ReforgeType.BELT -> itemCategory == ItemCategory.BELT
            ReforgeType.AXE -> itemCategory == ItemCategory.AXE
            ReforgeType.HOE -> itemCategory == ItemCategory.HOE
            ReforgeType.AXE_AND_HOE -> itemCategory in setOf(ItemCategory.HOE, ItemCategory.AXE)
            ReforgeType.PICKAXE -> itemCategory in setOf(ItemCategory.PICKAXE, ItemCategory.DRILL, ItemCategory.GAUNTLET)
            ReforgeType.ROD -> itemCategory == ItemCategory.FISHING_ROD
            ReforgeType.SWORD_AND_ROD -> itemCategory in setOf(
                ItemCategory.SWORD,
                ItemCategory.GAUNTLET,
                ItemCategory.LONGSWORD,
                ItemCategory.FISHING_ROD,
            )
            ReforgeType.VACUUM -> itemCategory == ItemCategory.VACUUM
            ReforgeType.SPECIAL_ITEMS -> specialItems?.contains(internalName) ?: false
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Reforge

            if (name != other.name) return false
            if (type != other.type) return false
            if (stats != other.stats) return false
            if (reforgeStone != other.reforgeStone) return false
            if (specialItems != other.specialItems) return false
            if (extraProperty != other.extraProperty) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + stats.hashCode()
            result = 31 * result + (reforgeStone?.hashCode() ?: 0)
            result = 31 * result + (specialItems?.hashCode() ?: 0)
            result = 31 * result + extraProperty.hashCode()
            return result
        }

        override fun toString(): String = "Reforge $name"
    }

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val reforgeStoneData = event.getConstant<Map<String, NeuReforgeJson>>("reforgestones", gson = reforgeGson).values
        val reforgeData = event.getConstant<Map<String, NeuReforgeJson>>("reforges", gson = reforgeGson).values
        reforgeList = (reforgeStoneData + reforgeData).map(::mapReforge)
    }

    private val reforgeGson: Gson = BaseGsonBuilder.gson()
        .registerTypeAdapter(SkyblockStat::class.java, SkyHanniTypeAdapters.SKYBLOCK_STAT.nullSafe())
        .registerTypeAdapter(
            SkyblockStatList::class.java,
            object : TypeAdapter<SkyblockStatList>() {
                override fun write(out: JsonWriter, value: SkyblockStatList) {
                    out.beginObject()
                    value.entries.forEach {
                        out.name(it.key.name.lowercase()).value(it.value)
                    }
                    out.endObject()
                }

                override fun read(reader: JsonReader): SkyblockStatList {
                    reader.beginObject()
                    val list = SkyblockStatList()
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        val value = reader.nextDouble()

                        val stat = SkyblockStat.getValueOrNull(name.uppercase()) ?: run {
                            ErrorManager.logErrorStateWithData(
                                "Unknown stat: '${name.uppercase()}'",
                                "Stat list could not parse stat",
                                "failed" to name.uppercase(),
                                betaOnly = true,
                            )
                            continue
                        }

                        list[stat] = value
                    }
                    reader.endObject()
                    return list
                }
            },
        ).create()

    private fun mapReforge(it: NeuReforgeJson): Reforge {
        val type = it.itemType
        return Reforge(
            name = it.reforgeName,
            type = EnumUtils.enumValueOf<ReforgeType>(type.first),
            stats = it.reforgeStats.orEmpty(),
            reforgeStone = it.internalName,
            specialItems = type.second.takeIf { it.isNotEmpty() },
            extraProperty = it.reforgeAbility,
            costs = it.reforgeCosts,
        )
    }
}
