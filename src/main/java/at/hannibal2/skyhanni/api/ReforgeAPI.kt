package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuReforgeJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.EnumMap

object ReforgeAPI {
    var reforgeList: List<Reforge> = emptyList()
        private set

    val nonePowerStoneReforge by lazy { reforgeList.filterNot { it.isReforgeStone } }

    enum class ReforgeType {
        SWORD, BOW, ARMOR, CHESTPLATE, HELMET, CLOAK, AXE, HOE, AXE_AND_HOE, PICKAXE, EQUIPMENT, ROD, SWORD_AND_ROD, SPECIAL_ITEMS, VACUUM
    }

    class Reforge(
        val name: String,
        val type: ReforgeType,
        val stats: Map<LorenzRarity, StatList>,
        val reforgeStone: NEUInternalName? = null,
        val specialItems: List<NEUInternalName>? = null,
        val extraProperty: Map<LorenzRarity, String> = emptyMap()
    ) {

        constructor(
            name: String,
            type: ReforgeType,
            stats: Map<LorenzRarity, StatList>,
            reforgeStone: NEUInternalName? = null,
            specialItems: List<NEUInternalName>? = null,
            extraPropertyText: String? = null,
            customStat: Map<LorenzRarity, Double>? = null
        ) : this(name, type, stats, reforgeStone, specialItems,
            extraPropertyText?.let { t ->
                customStat?.map { it.key to t.replace("$", it.value.round(1).toString()) }?.toMap()
                    ?: stats.keys.associateWith { t }
            } ?: emptyMap())

        val isReforgeStone = reforgeStone != null

        private val internalNameToRawName = "(_.)".toRegex()
        val rawReforgeStoneName = when (reforgeStone) {
            "SKYMART_BROCHURE".asInternalName() -> "SkyMart Brochure"
            "FULL-JAW_FANGING_KIT".asInternalName() -> "Full-Jaw Fanging Kit"
            else -> reforgeStone?.asString()?.lowercase()?.let {
                internalNameToRawName.replace(it) {
                    " " + it.groupValues[1][1].uppercase()
                }.replaceFirstChar { it.uppercase() }
            }
        }

        val lowercaseName = name.lowercase()

        fun isValid(itemStack: ItemStack) = isValid(itemStack.getItemCategoryOrNull(), itemStack.getInternalName())

        fun isValid(itemCategory: ItemCategory?, internalName: NEUInternalName) =
            when (type) {
                ReforgeType.SWORD -> setOf(
                    ItemCategory.SWORD,
                    ItemCategory.GAUNTLET,
                    ItemCategory.LONGSWORD,
                    ItemCategory.FISHING_WEAPON
                ).contains(itemCategory)

                ReforgeType.BOW -> itemCategory == ItemCategory.BOW || itemCategory == ItemCategory.SHORT_BOW
                ReforgeType.ARMOR -> setOf(
                    ItemCategory.HELMET,
                    ItemCategory.CHESTPLATE,
                    ItemCategory.LEGGINGS,
                    ItemCategory.BOOTS
                ).contains(itemCategory)

                ReforgeType.CHESTPLATE -> itemCategory == ItemCategory.CHESTPLATE
                ReforgeType.HELMET -> itemCategory == ItemCategory.HELMET
                ReforgeType.CLOAK -> itemCategory == ItemCategory.CLOAK
                ReforgeType.AXE -> itemCategory == ItemCategory.AXE
                ReforgeType.HOE -> itemCategory == ItemCategory.HOE
                ReforgeType.AXE_AND_HOE -> itemCategory == ItemCategory.HOE || itemCategory == ItemCategory.AXE
                ReforgeType.PICKAXE -> itemCategory == ItemCategory.PICKAXE || itemCategory == ItemCategory.DRILL || itemCategory == ItemCategory.GAUNTLET
                ReforgeType.EQUIPMENT -> setOf(
                    ItemCategory.CLOAK,
                    ItemCategory.BELT,
                    ItemCategory.NECKLACE,
                    ItemCategory.BRACELET,
                    ItemCategory.GLOVES
                ).contains(itemCategory)

                ReforgeType.ROD -> itemCategory == ItemCategory.FISHING_ROD || itemCategory == ItemCategory.FISHING_WEAPON
                ReforgeType.SWORD_AND_ROD -> setOf(
                    ItemCategory.SWORD,
                    ItemCategory.GAUNTLET,
                    ItemCategory.LONGSWORD,
                    ItemCategory.FISHING_ROD,
                    ItemCategory.FISHING_WEAPON
                ).contains(itemCategory)

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

        override fun toString(): String {
            return "Reforge $name"
        }

    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        /*   reforgeList = event.getConstant<ReforgesJson>("Reforges").reforges.mapNotNull {
              val value = it.value
              Reforge(
                  it.key,
                  ReforgeType.valueOf(value.type),
                  value.stats.mapNotNull {
                      LorenzRarity.valueOf(it.key) to StatList().apply {
                          it.value.forEach {
                              this[StatType.valueOf(it.key)] = it.value
                          }
                      }
                  }.toMap(),
                  value.reforgeStone?.asInternalName(),
                  value.specialItems?.mapNotNull { it.asInternalName() },
                  value.extraProperty,
                  value.customStat?.mapNotNull { LorenzRarity.valueOf(it.key) to it.value }?.toMap()
              )
          }
   */
    }

    @SubscribeEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val reforgeStoneData = event.readConstant<Map<String, NeuReforgeJson>>("reforgestones").values
        val reforgeData = event.readConstant<Map<String, NeuReforgeJson>>("reforges").values
        reforgeList = (reforgeStoneData + reforgeData).map {
            reforge(it)
        }
    }

    private fun reforge(it: NeuReforgeJson): Reforge {
        val type = it.itemType
        return Reforge(
            name = it.reforgeName,
            type = LorenzUtils.enumValueOf<ReforgeType>(type.first),
            stats = it.reforgeStats ?: emptyMap(),
            reforgeStone = it.internalName,
            specialItems = type.second.takeIf { it.isNotEmpty() },
            extraProperty = it.reforgeAbility
        )
    }

    enum class StatType(val icon: String) {
        DAMAGE("§c❁"),
        HEALTH("§c❤"),
        DEFENSE("§a❈"),
        STRENGTH("§c❁"),
        INTELLIGENCE("§b✎"),
        CRIT_DAMAGE("§9☠"),
        CRIT_CHANCE("§9☣"),
        FEROCITY("§c⫽"),
        BONUS_ATTACK_SPEED("§e⚔"),
        ABILITY_DAMAGE("§c๑"),
        HEALTH_REGEN("§c❣"),
        VITALITY("§4♨"),
        MENDING("§a☄"),
        TRUE_DEFENCE("§7❂"),
        SWING_RANGE("§eⓈ"),
        SPEED("§f✦"),
        SEA_CREATURE_CHANCE("§3α"),
        MAGIC_FIND("§b✯"),
        PET_LUCK("§d♣"),
        FISHING_SPEED("§b☂"),
        BONUS_PEST_CHANCE("§2ൠ"),
        COMBAT_WISDOM("§3☯"),
        MINING_WISDOM("§3☯"),
        FARMING_WISDOM("§3☯"),
        FORAGING_WISDOM("§3☯"),
        FISHING_WISDOM("§3☯"),
        ENCHANTING_WISDOM("§3☯"),
        ALCHEMY_WISDOM("§3☯"),
        CARPENTRY_WISDOM("§3☯"),
        RUNECRAFTING_WISDOM("§3☯"),
        SOCIAL_WISDOM("§3☯"),
        TAMING_WISDOM("§3☯"),
        MINING_SPEED("§6⸕"),
        BREAKING_POWER("§2Ⓟ"),
        PRISTINE("§5✧"),
        FORAGING_FORTUNE("§☘"),
        FARMING_FORTUNE("§6☘"),
        MINING_FORTUNE("§6☘"),
        FEAR("§a☠")
        ;

        val fname = name.lowercase().allLettersFirstUppercase()

        val iconWithName = "$icon $fname"

        fun asString(value: Int) = (if (value > 0) "+" else "") + value.toString() + " " + this.icon

        companion object {
            val fontSizeOfLargestIcon by lazy {
                entries.maxOf { Minecraft.getMinecraft().fontRendererObj.getStringWidth(it.icon) } + 1
            }
        }
    }

    class StatList : EnumMap<StatType, Double>(StatType::class.java), Map<StatType, Double> {
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

            /** @throws kotlin.error if one of the strings couldn't be matched to a StatType */
            fun fromJson(map: Map<String, Double>) =
                StatList().apply {
                    val e = map.mapKeys {
                        LorenzUtils.enumValueOf<StatType>(it.key.uppercase())
                    }
                    putAll(e)
                }

        }
    }

}
