package at.hannibal2.skyhanni.data.mob

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.mob.ShulkerEntity
import net.minecraft.entity.passive.AxolotlEntity
import net.minecraft.entity.passive.CodEntity
import net.minecraft.entity.passive.FrogEntity
import net.minecraft.entity.passive.PandaEntity
import net.minecraft.entity.passive.SalmonEntity
import net.minecraft.entity.passive.TadpoleEntity
import net.minecraft.entity.passive.TropicalFishEntity
import net.minecraft.util.DyeColor

object ModernIslandExceptions {

    internal fun galatea(
        baseEntity: LivingEntity,
        armorStand: ArmorStandEntity?,
        nextEntity: LivingEntity?,
    ): MobData.MobResult? = when {

        baseEntity is TadpoleEntity ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Birries"),
            )

        baseEntity is FrogEntity ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Mossybit"),
            )

        baseEntity is PandaEntity && baseEntity.isBrown ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Mochibear"),
            )

        baseEntity is PandaEntity && baseEntity.productGene == PandaEntity.Gene.NORMAL ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Bambuleaf"),
            )

        baseEntity is AxolotlEntity ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Coralot"),
            )

        baseEntity is CodEntity ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Cod"),
            )

        baseEntity is SalmonEntity ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Salmon"),
            )

        baseEntity is TropicalFishEntity && baseEntity.patternColor == DyeColor.LIGHT_BLUE ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Azure"),
            )

        baseEntity is TropicalFishEntity && baseEntity.patternColor == DyeColor.GREEN ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Verdant"),
            )

        baseEntity is ShulkerEntity && baseEntity.color == DyeColor.GREEN ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Hideonleaf"),
            )

        else -> null
    }

}
