package at.hannibal2.skyhanni.data.skyblockentities

import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

/**Mob that need Specific Rules to get correctly Detected*/
class SkyblockSpecialMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, override val name: String) : SkyblockMob(baseEntity, armorStand, null) {}
