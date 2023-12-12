package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

object MobFactories {
    fun slayer(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
        MobFilter.slayerNameFilter.find(armorStand.cleanName())?.let {
            Mob(baseEntity = baseEntity, mobType = Mob.Type.Slayer, armorStand = armorStand, name = it.groupValues[1], additionalEntities = extraEntityList, levelOrTier = it.groupValues[2].romanToDecimal())
        }

    fun boss(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase> = emptyList(), overriddenName: String? = null): Mob? =
        MobFilter.bossMobNameFilter.find(armorStand.cleanName())?.let {
            Mob(
                baseEntity = baseEntity, mobType = Mob.Type.Boss, armorStand = armorStand, name = overriddenName
                ?: it.groupValues[3], additionalEntities = extraEntityList
            )
        }

    fun dungeon(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase> = emptyList()): Mob? =
        MobFilter.dungeonNameFilter.find(armorStand.cleanName())?.let {
            Mob(baseEntity, Mob.Type.Dungeon, armorStand, it.groupValues[3], extraEntityList, hasStar = it.groupValues[1].isNotEmpty(), attribute = it.groupValues[2].takeIf { it.isNotEmpty() }
                ?.let { MobFilter.DungeonAttribute.valueOf(it) })
        }

    fun basic(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>? = null): Mob? =
        MobFilter.mobNameFilter.find(armorStand.cleanName())?.let {
            Mob(baseEntity = baseEntity, mobType = Mob.Type.Basic, armorStand = armorStand, name = it.groupValues[4].removeCorruptedSuffix(it.groupValues[3].isNotEmpty()), additionalEntities = extraEntityList, levelOrTier = it.groupValues[2].takeIf { it.isNotEmpty() }
                ?.toInt() ?: -1)
        }

    fun basic(baseEntity: EntityLivingBase, name: String) =
        Mob(baseEntity = baseEntity, mobType = Mob.Type.Basic, name = name)

    fun summon(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
        MobFilter.summonRegex.find(armorStand.cleanName())?.let {
            Mob(baseEntity = baseEntity, mobType = Mob.Type.Summon, armorStand = armorStand, name = it.groupValues[2], additionalEntities = extraEntityList, ownerName = it.groupValues[1])
        }

    fun displayNPC(baseEntity: EntityLivingBase, armorStand: EntityArmorStand): Mob =
        Mob(baseEntity = baseEntity, mobType = Mob.Type.DisplayNPC, armorStand = armorStand, name = armorStand.cleanName())

    fun player(baseEntity: EntityLivingBase): Mob = Mob(baseEntity, Mob.Type.Player)
    fun projectile(baseEntity: EntityLivingBase, name: String): Mob =
        Mob(baseEntity = baseEntity, mobType = Mob.Type.Projectile, name = name)

    fun special(baseEntity: EntityLivingBase, name: String, armorStand: EntityArmorStand? = null) =
        Mob(baseEntity = baseEntity, mobType = Mob.Type.Special, armorStand = armorStand, name = name)

    private fun String.removeCorruptedSuffix(case: Boolean) = if (case) this.dropLast(1) else this
    fun dojo(baseEntity: EntityLivingBase, armorStand: EntityArmorStand): Mob? =
        MobFilter.dojoFilter.find(armorStand.cleanName())?.let {
            Mob(
                baseEntity = baseEntity, mobType = Mob.Type.Special, armorStand = armorStand, name = if (it.groupValues[1].isNotEmpty()) "Points: " + it.groupValues[1] else it.groupValues[2]
            )
        }

}
