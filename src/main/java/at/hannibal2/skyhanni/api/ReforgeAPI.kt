package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.jsonobjects.repo.ReforgesJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.EnumMap

object ReforgeAPI {

    var reforgeList: List<Reforge> = emptyList()
        private set

    val nonePowerStoneReforge by lazy { reforgeList.filterNot { it.isReforgeStone } }

    enum class ReforgeType {
        Swords, Bows, Armor, Chestplates, Helmets, Cloaks, Axes, Hoes, HoesAndAxes, Pickaxes, Equipment, FishingRods, FishingRodsAndSwords, SpecialItems, Vacuums
    }

    class Reforge(
        val name: String,
        val type: ReforgeType,
        val stats: Map<LorenzRarity, StatList>,
        val reforgeStone: NEUInternalName? = null,
        val specialItems: List<NEUInternalName>? = null,
        val extraPropertyText: String? = null, // '$' will be replaced by the custom stat
        val customStat: Map<LorenzRarity, Double>? = null,
    ) {
        val isReforgeStone = reforgeStone != null

        val extraProperty = if (extraPropertyText != null) customStat?.map { it.key to extraPropertyText.replace("$", it.value.toString()) }?.toMap()
            ?: LorenzRarity.entries.associateWith { extraPropertyText } else null

        private val internalNameToRawName = "(_.)".toRegex()
        val rawReforgeStoneName = reforgeStone?.asString()?.lowercase()?.let {
            internalNameToRawName.replace(it) {
                " " + it.groupValues[1][1].uppercase()
            }.replaceFirstChar { it.uppercase() }
        }

        val lowercaseName = name.lowercase()

        fun isValid(itemStack: ItemStack) = isValid(itemStack.getItemCategoryOrNull(), itemStack.getInternalName())

        fun isValid(itemCategory: ItemCategory?, internalName: NEUInternalName) =
            when (type) {
                ReforgeType.Swords -> setOf(ItemCategory.SWORD, ItemCategory.GAUNTLET, ItemCategory.LONGSWORD, ItemCategory.FISHING_WEAPON).contains(itemCategory)
                ReforgeType.Bows -> itemCategory == ItemCategory.BOW || itemCategory == ItemCategory.SHORT_BOW
                ReforgeType.Armor -> setOf(ItemCategory.HELMET, ItemCategory.CHESTPLATE, ItemCategory.LEGGINGS, ItemCategory.BOOTS).contains(itemCategory)
                ReforgeType.Chestplates -> itemCategory == ItemCategory.CHESTPLATE
                ReforgeType.Helmets -> itemCategory == ItemCategory.HELMET
                ReforgeType.Cloaks -> itemCategory == ItemCategory.CLOAK
                ReforgeType.Axes -> itemCategory == ItemCategory.AXE
                ReforgeType.Hoes -> itemCategory == ItemCategory.HOE
                ReforgeType.HoesAndAxes -> itemCategory == ItemCategory.HOE || itemCategory == ItemCategory.AXE
                ReforgeType.Pickaxes -> itemCategory == ItemCategory.PICKAXE || itemCategory == ItemCategory.DRILL || itemCategory == ItemCategory.GAUNTLET
                ReforgeType.Equipment -> setOf(ItemCategory.CLOAK, ItemCategory.BELT, ItemCategory.NECKLACE, ItemCategory.BRACELET, ItemCategory.GLOVES).contains(itemCategory)
                ReforgeType.FishingRods -> itemCategory == ItemCategory.FISHING_ROD || itemCategory == ItemCategory.FISHING_WEAPON
                ReforgeType.FishingRodsAndSwords -> setOf(ItemCategory.SWORD, ItemCategory.GAUNTLET, ItemCategory.LONGSWORD, ItemCategory.FISHING_ROD, ItemCategory.FISHING_WEAPON).contains(itemCategory)
                ReforgeType.Vacuums -> itemCategory == ItemCategory.VACUUM
                ReforgeType.SpecialItems -> specialItems?.contains(internalName) ?: false
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
            if (extraPropertyText != other.extraPropertyText) return false
            if (customStat != other.customStat) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + stats.hashCode()
            result = 31 * result + (reforgeStone?.hashCode() ?: 0)
            result = 31 * result + (specialItems?.hashCode() ?: 0)
            result = 31 * result + (extraPropertyText?.hashCode() ?: 0)
            result = 31 * result + (customStat?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "Reforge $name"
        }

    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        reforgeList = event.getConstant<ReforgesJson>("Reforges").reforges.mapNotNull {
            val value = it.value
            Reforge(it.key, ReforgeType.valueOf(value.type), value.stats.mapNotNull {
                LorenzRarity.valueOf(it.key) to StatList().apply { it.value.forEach { this[StatType.valueOf(it.key)] = it.value } }
            }.toMap(), value.reforgeStone?.asInternalName(), value.specialItems?.mapNotNull { it.asInternalName() }, value.extraProperty, value.customStat?.mapNotNull { LorenzRarity.valueOf(it.key) to it.value }?.toMap())
        }

    }

    enum class StatType(val icon: String) {
        Damage("§c❁"),
        Health("§c❤"),
        Defense("§a❈"),
        Strength("§c❁"),
        Intelligence("§b✎"),
        Crit_Damage("§9☠"),
        Crit_Chance("§9☣"),
        Ferocity("§c⫽"),
        Bonus_Attack_Speed("§e⚔"),
        Ability_Damage("§c๑"),
        Health_Regen("§c❣"),
        Vitality("§4♨"),
        Mending("§a☄"),
        True_Defence("§7❂"),
        Swing_Range("§eⓈ"),
        Speed("§f✦"),
        Sea_Creature_Chance("§3α"),
        Magic_Find("§b✯"),
        Pet_Luck("§d♣"),
        Fishing_Speed("§b☂"),
        Bonus_Pest_Chance("§2ൠ"),
        Combat_Wisdom("§3☯"),
        Mining_Wisdom("§3☯"),
        Farming_Wisdom("§3☯"),
        Foraging_Wisdom("§3☯"),
        Fishing_Wisdom("§3☯"),
        Enchanting_Wisdom("§3☯"),
        Alchemy_Wisdom("§3☯"),
        Carpentry_Wisdom("§3☯"),
        Runecrafting_Wisdom("§3☯"),
        Social_Wisdom("§3☯"),
        Taming_Wisdom("§3☯"),
        Mining_Speed("§6⸕"),
        Breaking_Power("§2Ⓟ"),
        Pristine("§5✧"),
        Foraging_Fortune("§☘"),
        Farming_Fortune("§6☘"),
        Mining_Fortune("§6☘"),
        Fear("§a☠")
        ;

        val iconWithName = icon + " " + name.replace("_", " ")

        fun asString(value: Int) = (if (value > 0) "+" else "") + value.toString() + " " + this.icon
    }


    class StatList : EnumMap<StatType, Double>(StatType::class.java) {
        operator fun minus(other: StatList): StatList {
            return StatList().apply {
                for ((key, value) in this@StatList) {
                    this[key] = value - (other[key] ?: 0.0)
                }
                for ((key, value) in other) {
                    if (this[key] == null) {
                        this[key] = (this@StatList[key] ?: 0.0) - value
                    }
                }
            }
        }


        companion object {
            fun mapOf(vararg list: Pair<StatType, Double>) = StatList().apply {
                for ((key, value) in list) {
                    this[key] = value
                }
            }
        }
    }
}
