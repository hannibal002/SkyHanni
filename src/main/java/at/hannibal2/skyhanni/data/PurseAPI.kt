package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.milion
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PurseAPI {
    val pursePattern by RepoPattern.pattern("data.purseapi", "(Piggy|Purse): §6(?<coins>[\\\\d,]*).*")
    private var inventoryCloseTime = 0L
    var currentPurse = 0.0

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inventoryCloseTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {

        for (line in ScoreboardData.sidebarLinesFormatted) {
            val newPurse = pursePattern.matchMatcher(line) {
                group("coins").formatNumber().toDouble()
            } ?: continue
            val diff = newPurse - currentPurse
            if (diff == 0.0) continue
            currentPurse = newPurse

            PurseChangeEvent(diff, getCause(diff)).postAndCatch()
        }
    }

    // TODO add more causes in the future (e.g. ah/bz/bank)
    private fun getCause(diff: Double): PurseChangeCause {
        if (diff > 0) {
            if (diff == 1.0) {
                return PurseChangeCause.GAIN_TALISMAN_OF_COINS
            }

            if (diff == 15.milion || diff == 100.milion) {
                return PurseChangeCause.GAIN_DICE_ROLL
            }

            if (Minecraft.getMinecraft().currentScreen == null) {
                val timeDiff = System.currentTimeMillis() - inventoryCloseTime
                if (timeDiff > 2_000) {
                    return PurseChangeCause.GAIN_MOB_KILL
                }
            }
            return PurseChangeCause.GAIN_UNKNOWN
        } else {
            val timeDiff = System.currentTimeMillis() - SlayerAPI.questStartTime
            if (timeDiff < 1500) {
                return PurseChangeCause.LOSE_SLAYER_QUEST_STARTED
            }

            if (diff == -6_666_666.0 || diff == -666_666.0) {
                return PurseChangeCause.LOSE_DICE_ROLL_COST
            }

            return PurseChangeCause.LOSE_UNKNOWN
        }
    }
}
