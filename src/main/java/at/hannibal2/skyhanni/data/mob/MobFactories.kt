package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EnumUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand

object MobFactories {
    fun slayer(
        baseEntity: LivingEntity,
        armorStand: ArmorStand,
        extraEntityList: List<LivingEntity>,
    ): Mob? =
        MobFilter.slayerNameFilter.matchMatcher(armorStand.cleanName()) {
            Mob(
                baseEntity = baseEntity,
                mobType = Mob.Type.SLAYER,
                armorStand = armorStand,
                name = this.group("name"),
                additionalEntities = extraEntityList,
                levelOrTier = this.groupOrNull("tier")?.romanToDecimal() ?: 5,
                hypixelTypes = this.groupOrNull("mobtype").orEmpty(),
            )
        }

    fun boss(
        baseEntity: LivingEntity,
        armorStand: ArmorStand,
        extraEntityList: List<LivingEntity> = emptyList(),
        overriddenName: String? = null,
    ): Mob? =
        MobFilter.bossMobNameFilter.matchMatcher(armorStand.cleanName()) {
            Mob(
                baseEntity = baseEntity,
                mobType = Mob.Type.BOSS,
                armorStand = armorStand,
                name = overriddenName ?: this.group("name"),
                levelOrTier = group("level")?.takeIf { it.isNotEmpty() }?.toInt() ?: -1,
                additionalEntities = extraEntityList,
                hypixelTypes = this.groupOrNull("mobtype").orEmpty(),
            )
        }

    fun dungeon(
        baseEntity: LivingEntity,
        armorStand: ArmorStand,
        extraEntityList: List<LivingEntity> = emptyList(),
    ): Mob? =
        MobFilter.dungeonNameFilter.matchMatcher(armorStand.cleanName()) {
            Mob(
                baseEntity = baseEntity,
                mobType = Mob.Type.DUNGEON,
                armorStand = armorStand,
                name = this.group("name"),
                additionalEntities = extraEntityList,
                hasStar = this.group("star")?.isNotEmpty() ?: false,
                attribute = this.group("attribute")?.takeIf { it.isNotEmpty() }
                    ?.let {
                        EnumUtils.enumValueOfOrNull<MobFilter.DungeonAttribute>(it)
                    },
                hypixelTypes = this.groupOrNull("mobtype").orEmpty(),
            )
        }

    fun basic(
        baseEntity: LivingEntity,
        armorStand: ArmorStand,
        extraEntityList: List<LivingEntity>? = null,
    ): Mob? =
        MobFilter.mobNameFilter.findMatcher(armorStand.cleanName()) {
            Mob(
                baseEntity = baseEntity,
                mobType = Mob.Type.BASIC,
                armorStand = armorStand,
                name = this.group("name").removeCorruptedSuffix(
                    this.group("corrupted")?.isNotEmpty() ?: false,
                ),
                additionalEntities = extraEntityList,
                levelOrTier = this.group("level")?.takeIf { it.isNotEmpty() }
                    ?.toInt() ?: -1,
                hypixelTypes = this.groupOrNull("mobtype").orEmpty(),
            )
        }

    fun basic(baseEntity: LivingEntity, name: String) =
        Mob(baseEntity = baseEntity, mobType = Mob.Type.BASIC, name = name)

    fun summon(
        baseEntity: LivingEntity,
        armorStand: ArmorStand,
        extraEntityList: List<LivingEntity>,
    ): Mob? =
        MobFilter.summonFilter.matchMatcher(armorStand.cleanName()) {
            Mob(
                baseEntity = baseEntity,
                mobType = Mob.Type.SUMMON,
                armorStand = armorStand,
                name = this.group("name"),
                additionalEntities = extraEntityList,
                ownerName = this.group("owner"),
            )
        }

    fun displayNpc(baseEntity: LivingEntity, armorStand: ArmorStand, clickArmorStand: ArmorStand): Mob =
        Mob(
            baseEntity = baseEntity,
            mobType = Mob.Type.DISPLAY_NPC,
            armorStand = armorStand,
            name = armorStand.cleanName(),
            additionalEntities = listOf(clickArmorStand),
        )

    fun player(baseEntity: LivingEntity): Mob = Mob(baseEntity, Mob.Type.PLAYER, name = baseEntity.name.formattedTextCompatLessResets())
    fun projectile(baseEntity: LivingEntity, name: String): Mob =
        Mob(baseEntity = baseEntity, mobType = Mob.Type.PROJECTILE, name = name)

    fun special(baseEntity: LivingEntity, name: String, armorStand: ArmorStand? = null) =
        Mob(baseEntity = baseEntity, mobType = Mob.Type.SPECIAL, armorStand = armorStand, name = name)

    private fun String.removeCorruptedSuffix(case: Boolean) = if (case) this.dropLast(1) else this
    fun dojo(baseEntity: LivingEntity, armorStand: ArmorStand): Mob? =
        MobFilter.dojoFilter.matchMatcher(armorStand.cleanName()) {
            Mob(
                baseEntity = baseEntity,
                mobType = Mob.Type.SPECIAL,
                armorStand = armorStand,
                name = if (this.group("points")
                        ?.isNotEmpty() == true
                ) "Points: " + this.group("points") else this.group("empty").toString(),
            )
        }

    fun minionMob(baseEntity: LivingEntity) =
        Mob(baseEntity, Mob.Type.SPECIAL, name = MobFilter.MINION_MOB_PREFIX + baseEntity.cleanName())

}
