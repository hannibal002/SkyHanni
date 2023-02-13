package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class SkillExperience {

    private val actionBarPattern = Pattern.compile("(?:.*)§3\\+(?:.*) (.*) \\((.*)\\/(.*)\\)(?:.*)")
    private val inventoryPattern = Pattern.compile("(?:.*) §e(.*)§6\\/(?:.*)")

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        val profileData = event.profileData
        for ((key, value) in profileData.entrySet()) {
            if (key.startsWith("experience_skill_")) {
                val label = key.substring(17)
                val exp = value.asLong
                skillExp[label] = exp
            }
        }
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        skillExp.clear()
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val matcher = actionBarPattern.matcher(event.message)
        if (!matcher.matches()) return

        val skill = matcher.group(1).lowercase()
        val overflow = matcher.group(2).formatNumber()
        val neededForNextLevel = matcher.group(3).formatNumber()
        val nextLevel = getLevelForExp(neededForNextLevel)
        val baseExp = getExpForLevel(nextLevel - 1)
        skillExp[skill] = baseExp + overflow
    }

    private var tick = 0
    private var dirty = true

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        tick++
        if (tick % 20 != 0) return
        if (InventoryUtils.openInventoryName() != "Your Skills") return

        if (!dirty) return
        dirty = false

        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val stack = slot.stack
            val name = stack.name?.removeColor() ?: continue

            val lore = stack.getLore()

            var next = false
            for (line in lore) {
                if (line.contains("Progress to Level")) {
                    next = true
                    continue
                }
                if (next) {
                    if (!name.contains(" ")) continue
                    val split = name.split(" ")
                    val skillName = split[0].lowercase()
                    val level = split[1].romanToDecimal()
                    val baseExp = getExpForLevel(level)
                    val matcher = inventoryPattern.matcher(line)
                    if (matcher.matches()) {
                        val rawNumber = matcher.group(1)
                        val overflow = rawNumber.formatNumber()
                        val experience = baseExp + overflow
                        skillExp[skillName] = experience
                    }
                    next = false
                }
            }
        }
        if (skillExp.isNotEmpty()) return
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        dirty = true
    }

    companion object {
        private val skillExp = mutableMapOf<String, Long>()

        private fun getLevelForExp(experience: Long): Int {
            var level = 1
            for (levelXp in levelingExp) {
                if (levelXp.toLong() == experience) {
                    return level
                }
                level++
            }

            return 0
        }

        fun getExpForLevel(requestedLevel: Int): Long {
            var total = 0L
            var level = 0
            for (levelXp in levelingExp) {
                total += levelXp
                level++
                if (level == requestedLevel) {
                    return total
                }
            }

            return 0
        }

        //TODO create additional event
        fun getExpForSkill(skillName: String) = skillExp[skillName.lowercase()] ?: 0

        private val levelingExp = listOf(
            50,
            125,
            200,
            300,
            500,
            750,
            1000,
            1500,
            2000,
            3500,
            5000,
            7500,
            10000,
            15000,
            20000,
            30000,
            50000,
            75000,
            100000,
            200000,
            300000,
            400000,
            500000,
            600000,
            700000,
            800000,
            900000,
            1000000,
            1100000,
            1200000,
            1300000,
            1400000,
            1500000,
            1600000,
            1700000,
            1800000,
            1900000,
            2000000,
            2100000,
            2200000,
            2300000,
            2400000,
            2500000,
            2600000,
            2750000,
            2900000,
            3100000,
            3400000,
            3700000,
            4000000,
            4300000,
            4600000,
            4900000,
            5200000,
            5500000,
            5800000,
            6100000,
            6400000,
            6700000,
            7000000
        )
    }
}

private fun String.formatNumber(): Long {
    var text = replace(",", "")
    val multiplier = if (text.endsWith("k")) {
        text = text.substring(0, text.length - 1)
        1_000
    } else if (text.endsWith("m")) {
        text = text.substring(0, text.length - 1)
        1_000_000
    } else {
        1
    }
    val d = text.toDouble()
    return (d * multiplier).toLong()
}
