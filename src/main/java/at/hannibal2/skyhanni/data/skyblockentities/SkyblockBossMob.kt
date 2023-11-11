package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand


//﴾ [Lv200] aMage Outlawa 70M/70M❤ ﴿
//﴾ [Lv500] Magma Boss █████████████████████████ ﴿
open class SkyblockBossMob(baseEntity: Entity, armorStand: EntityArmorStand?, override val name: String) : SkyblockMob(baseEntity, armorStand) {
    init {
        LorenzDebug.log("BossName: '$name' , Colorless: '${armorStand?.name?.removeColor()}' , Raw: '${armorStand?.name}'")
    }
}
