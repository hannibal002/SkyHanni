package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.LivingEntity

@PrimaryFunction("onSlayerEntityDeath")
class SlayerEntityDeathEvent(
    entity: LivingEntity,
    val slayerType: SlayerType,
    val tier: Int,
    val owner: String?,
) : EntityDeathEvent<LivingEntity>(entity)
