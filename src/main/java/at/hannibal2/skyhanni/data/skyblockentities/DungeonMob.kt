package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.SkyblockMobUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

class DungeonMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?) : SkyblockMob(baseEntity, armorStand) {

    val Attribute: String?
    val hasStar: Boolean
    override val name: String

    init { // TODO test with every dungeon boss
        var initStartIndex = 0
        val nameWithoutColor = armorStand?.name?.removeColor()
        if (nameWithoutColor != null) {
            // If new Dungeon Mobs get added that have a name longer than 3 words this must be changed
            // limit = max words in name + 1x Attribute name + 1x Health + 1x Star
            val words = nameWithoutColor.split(" ", ignoreCase = true, limit = 6)

            hasStar = (words[initStartIndex] == "✯").also { if (it) initStartIndex++ }

            Attribute =
                SkyblockMobUtils.dungeonAttribute.firstOrNull { it == words[initStartIndex] }?.also { initStartIndex++ }

            // For a wierd reason the Undead Skeletons (or similar)
            // can spawn with a level if they are summoned with the 3 skulls
            words[initStartIndex].startsWith("[").also { if (it) initStartIndex++ }

            name = words.subList(initStartIndex, words.lastIndex).joinToString(separator = " ")
        } else {
            Attribute = null
            hasStar = false
            name = SkyblockMobUtils.errorNameFinding(baseEntity.name)
        }
    }
}
