package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.findMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

object MobFactories {
    fun slayer(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
        MobFilter.slayerNameFilter.matchMatcher(armorStand.cleanName()) {
            Mob(baseEntity = baseEntity, mobType = Mob.Type.Slayer, armorStand = armorStand, name = this.group(1), additionalEntities = extraEntityList, levelOrTier = this.group(2).romanToDecimal())
        }

    fun boss(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase> = emptyList(), overriddenName: String? = null): Mob? =
        MobFilter.bossMobNameFilter.matchMatcher(armorStand.cleanName()) {
            Mob(
                baseEntity = baseEntity, mobType = Mob.Type.Boss, armorStand = armorStand, name = overriddenName
                ?: this.group(3), additionalEntities = extraEntityList
            )
        }

    fun dungeon(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase> = emptyList()): Mob? =
        MobFilter.dungeonNameFilter.matchMatcher(armorStand.cleanName()) {
            Mob(baseEntity, Mob.Type.Dungeon, armorStand, this.group(3), extraEntityList, hasStar = this.group(1).isNotEmpty(), attribute = this.group(2).takeIf { it.isNotEmpty() }
                ?.let { MobFilter.DungeonAttribute.valueOf(it) })
        }

    fun basic(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>? = null): Mob? =
        MobFilter.mobNameFilter.findMatcher(armorStand.cleanName()) {
            Mob(baseEntity = baseEntity, mobType = Mob.Type.Basic, armorStand = armorStand, name = this.group(4).removeCorruptedSuffix(this.group(3).isNotEmpty()), additionalEntities = extraEntityList, levelOrTier = this.group(2).takeIf { it.isNotEmpty() }
                ?.toInt() ?: -1)
        }

    fun basic(baseEntity: EntityLivingBase, name: String) =
        Mob(baseEntity = baseEntity, mobType = Mob.Type.Basic, name = name)

    fun summon(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
        MobFilter.summonFilter.matchMatcher(armorStand.cleanName()) {
            Mob(baseEntity = baseEntity, mobType = Mob.Type.Summon, armorStand = armorStand, name = this.group(2), additionalEntities = extraEntityList, ownerName = this.group(1))
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
        MobFilter.dojoFilter.matchMatcher(armorStand.cleanName()) {
            Mob(
                baseEntity = baseEntity, mobType = Mob.Type.Special, armorStand = armorStand, name = if (this.group(1).isNotEmpty()) "Points: " + this.group(1) else this.group(2)
            )
        }

    fun minionMob(baseEntity: EntityLivingBase) =
        Mob(baseEntity, Mob.Type.Special, name = MobFilter.minionMobPrefix + baseEntity.cleanName())

}
