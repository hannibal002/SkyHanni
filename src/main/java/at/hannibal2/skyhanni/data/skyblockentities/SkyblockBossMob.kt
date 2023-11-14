package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand


//﴾ [Lv200] aMage Outlawa 70M/70M❤ ﴿
//﴾ [Lv500] Magma Boss █████████████████████████ ﴿
open class SkyblockBossMob(
    baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, override val name: String,
    extraEntities: List<EntityLivingBase>?,
) : SkyblockMob(baseEntity, armorStand, extraEntities) {
    init {
        LorenzDebug.log("BossName: '$name' , Colorless: '${armorStand?.name?.removeColor()}' , Raw: '${armorStand?.name}'")
    }
}
