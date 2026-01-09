package at.hannibal2.skyhanni.data.mob

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Cod
import net.minecraft.world.entity.animal.Panda
import net.minecraft.world.entity.animal.Salmon
import net.minecraft.world.entity.animal.TropicalFish
import net.minecraft.world.entity.animal.axolotl.Axolotl
import net.minecraft.world.entity.animal.frog.Frog
import net.minecraft.world.entity.animal.frog.Tadpole
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.item.DyeColor

object ModernIslandExceptions {

    internal fun galatea(
        baseEntity: LivingEntity,
        armorStand: ArmorStand?,
        nextEntity: LivingEntity?,
    ): MobData.MobResult? = when {

        baseEntity is Tadpole && armorStand == null ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Birries"),
            )

        baseEntity is Frog && armorStand == null ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Mossybit"),
            )

        baseEntity is Panda && baseEntity.isBrown ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Mochibear"),
            )

        baseEntity is Panda && baseEntity.variant == Panda.Gene.NORMAL ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Bambuleaf"),
            )

        baseEntity is Axolotl ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Coralot"),
            )

        baseEntity is Cod ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Cod"),
            )

        baseEntity is Salmon ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Salmon"),
            )

        baseEntity is TropicalFish && baseEntity.patternColor == DyeColor.LIGHT_BLUE ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Azure"),
            )

        baseEntity is TropicalFish && baseEntity.patternColor == DyeColor.GREEN ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Verdant"),
            )

        baseEntity is Shulker && baseEntity.color == DyeColor.GREEN ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Hideonleaf"),
            )

        else -> null
    }

}
