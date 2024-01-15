package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class CommissionsCalculator {

    private val mc = Minecraft.getMinecraft()
    private val config get() = SkyHanniMod.feature.mining.commissionsCalculator

    private val DWARVEN = IslandType.DWARVEN_MINES
    private val CRYSTAL = IslandType.CRYSTAL_HOLLOWS

    private val patternGroup = RepoPattern.group("commissions.calculator")
    private val hotmXPPattern by patternGroup.pattern(
        "hotm.xp.loreline",
        "(?:(?:§.)*-)? ?(?:§.)*(?<xp>[\\d,.]+) (?:§.)*[hH][oO][tT][mM] (?:§.)*[eE][xX][pP](?:(?:§.)* (?:§.)*\\((?:§.)*[dD][aA][iI][lL][yY] ?[bB][oO][nN][uU][sS](?:§.)*\\)(?:§.)*)?"
    )
    private val commissionItemPattern by patternGroup.pattern(
        "commission.itemname",
        "(?:§.)*Commission #(?<commission>\\d+)(?: (?:§.)*[nN][eE][wW])?"
    )
    private val tierItemPattern by patternGroup.pattern(
        "hotm.tier.itemname",
        "§eTier (?<tier>\\d+)"
    )
    private val mileProgressPattern by patternGroup.pattern(
        "milestone.progress.loreline",
        "(?:§.| )*(?<completed>[\\d,.]+)(?:§.)*/(?:§.)*(?<required>[\\d,.]+)"
    )
    private val tierProgressPattern by patternGroup.pattern(
        "hotm.tier.progress.loreline",
        "(?:§.| )*(?<obtained>[\\d,.]+)(?:§.)*/(?:§.)*(?<needed>[\\S,.]+)"
    )
    private val milestoneRewardsItemPattern by patternGroup.pattern(
        "milestone.itemname",
        "(?:§.)*Milestone (?<milestone>\\S+) Rewards"
    )

    private val display: MutableList<String> = mutableListOf<String>()
    private val firstLine: String = "§lCommissions Calculator:"
    private val fatDisclaimer: List<String> = listOf(
        "§c§lDisclaimer: §r§cThis calculator does not include",
        "§cother potential sources of HOTM XP, including the",
        "§c900 HOTM XP Daily Bonus.",
        "§cTo update these calculations, please open /hotm."
    )

    private var currentHOTMLevel: Int = 0
    private var currentHOTMXP: Int = 0

    private enum class HOTMProgression(
        val tier: Int,
        val toNextTier: Int,
        val perComm: Double
    ) {
        ONE(1, 3000,  100.0),
        TWO(2, 9000,  200.0),
        THREE(3, 25000,  400.0),
        FOUR(4,60000,400.0),
        FIVE(5, 100000,400.0),
        SIX(6, 150000, 400.0),
        ;
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inMiningIsland()) return
        if (!isEnabled()) return
        if (mc.currentScreen !is GuiChest) return
        val chestName = event.inventoryName
        if (chestName.isNotValidChestName()) return
        val colorCode = if (DWARVEN.isInIsland()) "§2" else "§5" //themed based on mining island
        if (currentHOTMLevel == 0) {
            listOf(
                "$colorCode$firstLine",
                "§cPlease open the Heart of the Mountain (/hotm)."
            ).update()
            return
        }
        val hotmInfo = HOTMProgression.entries.find { it.tier == currentHOTMLevel } ?: return
        val perComm = hotmInfo.perComm
        val toNextTier = hotmInfo.toNextTier
        val items = event.inventoryItems
        val commsToNextTier = ceil(abs(toNextTier - currentHOTMXP) / perComm).roundToInt()
        if (chestName == "Commissions") {
            var hotmXP: Long = 0
            loop@ for ((_, item) in items) {
                commissionItemPattern.matchMatcher(item.cleanName()) {
                    val lore = item.getLore()
                    if (lore.none { it == "§a§lCOMPLETED" } && config.completedOnly) continue@loop
                    for (line in lore) {
                        hotmXPPattern.matchMatcher(line) {
                            hotmXP += group("xp").formatNumber()
                        }
                    }
                }
            }
            listOf(
                "$colorCode$firstLine",
                " §7- §f(At least $colorCode${hotmXP.addSeparators()} HOTM XP §fto be gained from claiming completed commissions)",
                " §7- $colorCode${commsToNextTier.addSeparators()} §fmore commissions to ${colorCode}HOTM ${currentHOTMLevel + 1}",
                " §7- §f(to reach $colorCode${toNextTier.addSeparators()} HOTM XP §ffrom $colorCode${currentHOTMXP.addSeparators()} HOTM XP§f)",
            ).update()
            return
        }
        if (chestName == "Heart of the Mountain") {
            for ((_, item) in items) {
                tierItemPattern.matchMatcher(item.cleanName()) {
                    currentHOTMLevel = group("tier").groupToInt()
                    for (line in item.getLore()) {
                        tierProgressPattern.matchMatcher(line) {
                            currentHOTMXP = group("obtained").groupToInt()
                        }
                    }
                }
            }
            listOf(
                "$colorCode$firstLine",
                " §7- §fCurrent HOTM Level: $colorCode$currentHOTMLevel",
                " §7- §fCurrent HOTM XP: $colorCode${currentHOTMXP.addSeparators()}",
                "§c(Remember to scroll up the HOTM tree!)"
            ).update()
            return
        }
        if (chestName == "Commission Milestones") {
            for ((_, item) in items) {
                milestoneRewardsItemPattern.matchMatcher(item.cleanName()) {
                    val milestoneInt = group("milestone").romanToDecimalIfNecessary()
                    for (line in item.getLore()) {
                        mileProgressPattern.matchMatcher(line) {
                            val completed = group("completed").groupToInt()
                            val required = group("required").groupToInt()
                            val remaining = abs(required - completed)
                            if (completed < required) {
                                val remainingPlural = StringUtils.optionalPlural(remaining, "commission", "commissions")
                                val hotmXPGain = (remaining * perComm).roundToInt()
                                listOf(
                                    "$colorCode$firstLine",
                                    " §7- $colorCode$remainingPlural §fleft to complete §6Milestone $milestoneInt §f($colorCode+${hotmXPGain.addSeparators()} HOTM XP§f)",
                                    " §7- $colorCode${commsToNextTier.addSeparators()} §fmore commissions to ${colorCode}HOTM ${currentHOTMLevel + 1}",
                                    " §7- §f(to reach $colorCode${toNextTier.addSeparators()} HOTM XP §ffrom $colorCode${currentHOTMXP.addSeparators()} HOTM XP§f)",
                                ).update()
                                return
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun List<String>.update() {
        display.clear()
        display.addAll(this)
        if (currentHOTMLevel != 0) display.addAll(fatDisclaimer)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!inMiningIsland()) return
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName().isNotValidChestName()) return
        config.position.renderStrings(display, posLabel = "Commissions Calculator")
    }

    private fun isEnabled() = config.enabled
    private fun inMiningIsland(): Boolean = DWARVEN.isInIsland() || CRYSTAL.isInIsland()
    private fun String.isNotValidChestName(): Boolean = this != "Commission Milestones" && this != "Commissions" && this != "Heart of the Mountain"
    private fun String.groupToInt(): Int = this.formatNumber().toInt()
}
