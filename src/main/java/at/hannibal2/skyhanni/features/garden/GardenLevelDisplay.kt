package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.roundToInt

class GardenLevelDisplay {
    private val config get() = SkyHanniMod.feature.garden
    private val expToNextLevelPattern = "(?:.*) §e(?<nextLevelExp>.*)§6\\/(?:.*)".toPattern()
    private val overflowPattern = ".*§r §6(?<overflow>.*) XP".toPattern()
    private val namePattern = "Garden Level (?<currentLevel>.*)".toPattern()
    private var gardenExp
        get() = SkyHanniMod.feature.hidden.gardenExp
        set(value) {
            SkyHanniMod.feature.hidden.gardenExp = value
        }
    private var display = ""
    private var visitorRewardPattern = " {4}§r§8\\+§r§2(?<exp>.*) §r§7Garden Experience".toPattern()

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        update()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return

        visitorRewardPattern.matchMatcher(event.message) {
            gardenExp += group("exp").toInt()
            update()
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Desk") return
        val item = event.inventoryItems[4]!!

        namePattern.matchMatcher(item.name!!.removeColor()) {
            val currentLevel = group("currentLevel").romanToDecimalIfNeeded()
            var nextLevelExp = 0
            for (line in item.getLore()) {
                expToNextLevelPattern.matchMatcher(line) {
                    nextLevelExp = group("nextLevelExp").replace(",", "").toDouble().roundToInt()
                }
                overflowPattern.matchMatcher(line) {
                    val overflow = group("overflow").replace(",", "").toDouble().roundToInt()
                    gardenExp = overflow
                    update()
                    return
                }
            }
            val expForLevel = getExpForLevel(currentLevel).toInt()
            gardenExp = expForLevel + nextLevelExp
            update()
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): String {
        if (gardenExp == -1) return "§aGarden Level ? §cOpen the desk!"
        val currentLevel = getLevelForExp(gardenExp.toLong())
        val needForLevel = getExpForLevel(currentLevel).toInt()
        val nextLevel = currentLevel + 1
        val needForNextLevel = getExpForLevel(nextLevel).toInt()

        return "§aGarden Level $currentLevel" + if (needForNextLevel != 0) {
            val overflow = gardenExp - needForLevel
            val needForOnlyNextLvl = needForNextLevel - needForLevel

            val need = LorenzUtils.formatInteger(overflow)
            val have = LorenzUtils.formatInteger(needForOnlyNextLvl)
            " §7(§e$need§7/§e$have§7)"
        } else ""
    }

    private fun getLevelForExp(gardenExp: Long): Int {
        var tier = 0
        var totalCrops = 0L
        for (tierCrops in gardenExperience) {
            totalCrops += tierCrops
            if (totalCrops > gardenExp) {
                return tier
            }
            tier++
        }

        return tier
    }

    // TODO make table utils method
    private fun getExpForLevel(requestedLevel: Int): Long {
        var totalCrops = 0L
        var tier = 0
        for (tierCrops in gardenExperience) {
            totalCrops += tierCrops
            tier++
            if (tier == requestedLevel) {
                return totalCrops
            }
        }

        return 0
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.gardenLevelPos.renderString(display, posLabel = "Garden Level")
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.gardenLevelDisplay

    // TODO use repo
    private val gardenExperience = listOf(
        0,
        70,
        100,
        140,
        240,
        600,
        1500,
        2000,
        2500,
        3000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000, // level 15

        // overflow levels till 40 for now, in 10k steps
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
    )
}