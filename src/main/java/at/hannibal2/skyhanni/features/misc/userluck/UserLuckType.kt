package at.hannibal2.skyhanni.features.misc.userluck

import at.hannibal2.skyhanni.api.SkillApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.SkillOverflowLevelUpEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Pattern

sealed class UserLuckType {
    abstract val luck: Float

    object Limbo : UserLuckType() {
        override val luck get() = ProfileStorageData.playerSpecific?.limbo?.userLuck ?: 0f
    }

    @SkyHanniModule
    object Skills : UserLuckType() {
        override val luck get() = skillMap.sumAllValues().toFloat()

        private var _skillMap: Map<SkillType, Float>? = null
        val skillMap: Map<SkillType, Float>
            get() = _skillMap ?: calcSkillMap()

        private fun calcSkillMap(): Map<SkillType, Float> {
            val storage = SkillApi.storage ?: return SkillType.entries.associate { it to 0f }
            val map = mutableMapOf<SkillType, Float>()
            for ((skillType, skillInfo) in storage) {
                val level = skillInfo.level
                val overflow = skillInfo.overflowLevel
                val luck = luckFromOverflowLevel(level, overflow)
                map.addOrPut(skillType, luck.toFloat())
            }
            _skillMap = map
            return map
        }

        @HandleEvent
        fun onLevelUp(event: SkillOverflowLevelUpEvent) {
            val map = calcSkillMap().toMutableMap()
            val skillLuck = luckFromOverflowLevel(event.skill.maxLevel, event.newLevel).toFloat()

            map.put(event.skill, skillLuck)
        }

        private fun luckFromOverflowLevel(maxLevel: Int, currentOverflowLevel: Int) = ((currentOverflowLevel - maxLevel) / 5) * 50
    }

    companion object {
        val entries: List<UserLuckType> = listOf(Limbo, Skills)

        fun getTotalLuck(): Float = entries.sumOf { it.luck.toDouble() }.toFloat()
    }
}

enum class UserLuckMultiplier(val condition: (() -> Float), val isMultiplicative: Boolean = false) {
    JERRY_MAYOR(
        { if (Perk.STATSPOCALYPSE.isActive) 0.1f else 0f }
    ),
    SUPERIOR_DRAGON_ARMOR(
        { if (isWearingArmor(superiorDragonArmorNames)) 0.05f else 0f }
    ),
//     BLAZE_PET(
//         { /* (inNether || inKuudra) && pet level * 0.001 */ 0f},
//         isMultiplicative = true
//     ),
//     ENDER_DRAGON_PET(
//         { /* isLegendary && pet level * 0.001 */ 0f}
//     ),
    LEGION_ENCHANTMENT(
        { getTotalEnchantmentLevel("Legion") * 0.0007f },
    ),
    RENOWNED_REFORGE(
        { getTotalReforgeLevel("renowned") * 0.001f },
    ),
    ;

    companion object {
        fun totalLuckAfterBonus(luck: Float): Float {
            var totalLuck = luck
            val additiveBonuses = entries.filter { !it.isMultiplicative }
            val multiplicativeBonuses = entries.filter { it.isMultiplicative }

            totalLuck *= (additiveBonuses.sumOf { it.condition.invoke().toDouble() }).toFloat() + 1f
            multiplicativeBonuses.forEach {
                totalLuck *= 1f + it.condition.invoke()
            }

            return totalLuck
        }

        private val superiorDragonArmorNames by RepoPattern.pattern(
            "misc.userluck.armor.superior",
            "(?:BRONZE|SILVER|GOLD|DIAMOND)_HUNTER_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)",
        )

        private fun isWearingArmor(armorPattern: Pattern): Boolean =
            InventoryUtils.getArmor().all { armorPattern.matches(it?.getInternalNameOrNull()?.asString()) }

        private fun getTotalEnchantmentLevel(enchantment: String): Int =
            InventoryUtils.getArmor().sumOf { (it?.getEnchantments()?.getOrElse(enchantment) { 0 }) ?: 0 }

        private fun getTotalReforgeLevel(reforge: String): Int = //kotlin is dumb
            InventoryUtils.getArmor().sumOf { (if (it?.getReforgeName() == reforge) 1L else 0L) }.toInt()
    }
}
