package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class CommissionsCalculator {
    private val config get() = SkyHanniMod.feature.mining.commissionsCalculator

    private val DWARVEN = IslandType.DWARVEN_MINES

    private val patternGroup = RepoPattern.group("commissions.calculator")
    private val hotmXPPattern by patternGroup.pattern(
        "hotm.xp.loreline",
        "(?i)(?:(?:§.)*-)? ?(?:§.)*(?<xp>[\\d,.]+) (?:§.)*HOTM (?:§.)*EXP(?:(?:§.)* (?:§.)*\\((?:§.)*DAILY ?BONUS(?:§.)*\\)(?:§.)*)?"
    )
    private val commissionItemPattern by patternGroup.pattern(
        "commission.itemname",
        "(?i)(?:§.)*Commission #(?<commission>\\d+)(?: \\(?(?:§.)*\\(?(?:§.)*NEW(?:§.)*\\)?(?:§.)*)?"
    )
    private val tierItemPattern by patternGroup.pattern(
        "hotm.tier.coloreditemname",
        "§eTier (?<tier>\\d+)"
    )
    private val maxTierItemPattern by patternGroup.pattern(
        "hotm.maxtier.coloreditemname",
        "§aTier (?<tier>\\d+)"
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

    private val firstLine: String = "§lCommissions Calculator:"
    private val fatDisclaimer: List<Renderable> = listOf(
        Renderable.string("§c§lDisclaimer: §r§cThis calculator only accounts for"),
        Renderable.string("§cthe standard HOTM XP gain from claiming commissions"),
        Renderable.string("§cbefore any modifiers/boosts or other sources."),
    )
    private val disclaimerOpenHOTM: Renderable = Renderable.clickAndHover("§eTo update these calculations, click here to open /hotm.",
        listOf("Click to run §e/hotm"), onClick = { LorenzUtils.sendCommandToServer("hotm") }
    )

    private var display: List<Renderable> = listOf()

    private var currentHOTMTier: Int = 0
    private var currentHOTMXP: Int = 0

    private enum class HOTMTier(
        val tier: Int,
        val xpToNextTier: Int,
        val xpPerComm: Double
    ) {
        ONE(1, 3000, 100.0),
        TWO(2, 9000, 200.0),
        THREE(3, 25000, 400.0),
        FOUR(4, 60000, 400.0),
        FIVE(5, 100000, 400.0),
        SIX(6, 150000, 400.0),
        ;
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        if (currentHOTMTier.isMaxHOTMTier()) return
        val chestName = event.inventoryName
        if (chestName.isNotValidChestName()) return
        val colorCode = if (DWARVEN.isInIsland()) "§2" else "§5" //themed based on mining island
        val items = event.inventoryItems
        val newList = mutableListOf(Renderable.string("$colorCode$firstLine"))
        if (chestName == "Heart of the Mountain") {
            hotmStatus(items, newList, colorCode)
            drawDisplay(newList)
            return
        }
        if (currentHOTMTier == 0) {
            newList.add(Renderable.clickAndHover(
                "§cOpen the §e/hotm §ctree.",
                listOf("Click to run §e/hotm"),
                onClick = { LorenzUtils.sendCommandToServer("hotm") }
            ))
            drawDisplay(newList)
            return
        }
        val hotmInfo = HOTMTier.entries.find { it.tier == currentHOTMTier } ?: return
        val (perComm, toNextTier, commsToNextTier) = grabRelevantInfoFrom(hotmInfo)
        updateListFromChest(chestName, newList, items, colorCode, perComm, commsToNextTier, toNextTier)
    }

    private fun grabRelevantInfoFrom(hotmInfo: HOTMTier): Triple<Double, Int, Int> {
        val perComm = hotmInfo.xpPerComm
        val toNextTier = hotmInfo.xpToNextTier
        val commsToNextTier = ceil(abs(toNextTier - currentHOTMXP) / perComm).roundToInt()
        return Triple(perComm, toNextTier, commsToNextTier)
    }

    private fun updateListFromChest(chestName: String, listBeingModified: MutableList<Renderable>, items: Map<Int, ItemStack>, colorCode: String, perComm: Double, commsToNextTier: Int, toNextTier: Int) {
        when (chestName) {
            "Commissions" -> hotmFromComms(items, listBeingModified, colorCode)
            "Commission Milestones" -> untilNextMilestone(items, perComm, listBeingModified, colorCode)
            else -> return
        }
        commissionsUntilNextTier(listBeingModified, colorCode, commsToNextTier, toNextTier)
        drawDisplay(listBeingModified)
    }

    private fun commissionsUntilNextTier(listBeingModified: MutableList<Renderable>, colorCode: String, commsToNextTier: Int, toNextTier: Int) {
        listBeingModified.addAll(
            listOf(
                Renderable.string(" §7- $colorCode${commsToNextTier.addSeparators()} §fmore commissions to ${colorCode}HOTM ${currentHOTMTier + 1}"),
                Renderable.string(" §7- §f(to reach $colorCode${toNextTier.addSeparators()} HOTM XP §ffrom $colorCode${currentHOTMXP.addSeparators()} HOTM XP§f)"),
            )
        )
    }

    private fun hotmFromComms(items: Map<Int, ItemStack>, listBeingModified: MutableList<Renderable>, colorCode: String) {
        var hotmGain = 0L
        for ((_, item) in items) {
            val lore = item.getLore()
            if (lore.none { it == "§a§lCOMPLETED" } && config.completedOnly) continue
            commissionItemPattern.matchMatcher(item.cleanName()) {
                for (line in lore) {
                    hotmXPPattern.matchMatcher(line) {
                        hotmGain += group("xp").formatNumber()
                    }
                }
            }
        }
        listBeingModified.add(Renderable.string(" §7- §f(At least $colorCode${hotmGain.addSeparators()} HOTM XP §fto be gained from claiming completed commissions)"))
    }

    private fun hotmStatus(items: Map<Int, ItemStack>, listBeingModified: MutableList<Renderable>, colorCode: String) {
        listBeingModified.add(Renderable.string(" §e(Remember to scroll up the HOTM tree!)"))
        loop@for ((_, item) in items) {
            val itemName = item.name ?: ""
            maxTierItemPattern.matchMatcher(itemName) {
                val foundHOTMTier = group("tier").groupToInt()
                if (foundHOTMTier < currentHOTMTier) break@loop
                val lastHOTMTier = HOTMTier.entries[foundHOTMTier - 2]
                currentHOTMTier = lastHOTMTier.tier + 1
                currentHOTMXP = lastHOTMTier.xpToNextTier
                break@loop
            }
            tierItemPattern.matchMatcher(itemName) {
                currentHOTMTier = group("tier").groupToInt()
                for (line in item.getLore()) {
                    tierProgressPattern.matchMatcher(line) {
                        currentHOTMXP = group("obtained").groupToInt()
                    }
                }
            }
        }
        val hotmInfo = HOTMTier.entries.find { it.tier == currentHOTMTier } ?: return
        val (perComm, toNextTier, commsToNextTier) = grabRelevantInfoFrom(hotmInfo)
        listBeingModified.addAll(
            listOf(
                Renderable.string(" §7- §fCurrent HOTM Tier: $colorCode$currentHOTMTier"),
                Renderable.string(" §7- §fCurrent HOTM XP: $colorCode${currentHOTMXP.addSeparators()}"),
                Renderable.string(" §7- §fXP per Commission: $colorCode${perComm.addSeparators()}"),
            )
        )
        commissionsUntilNextTier(listBeingModified, colorCode, commsToNextTier, toNextTier)
    }

    private fun untilNextMilestone(items: Map<Int, ItemStack>, perComm: Double, listBeingModified: MutableList<Renderable>, colorCode: String) {
        for ((_, item) in items) {
            milestoneRewardsItemPattern.matchMatcher(item.cleanName()) {
                val milestone = group("milestone").romanToDecimalIfNecessary()
                for (line in item.getLore()) {
                    mileProgressPattern.matchMatcher(line) {
                        val completed = group("completed").groupToInt()
                        val required = group("required").groupToInt()
                        remainingMilestones(required, completed, perComm, listBeingModified, colorCode, milestone)
                    }
                }
            }
        }
    }

    private fun remainingMilestones(required: Int, completed: Int, perComm: Double, listBeingModified: MutableList<Renderable>, colorCode: String, milestone: Int) {
        val commsToNextMilestone = required - completed
        val singularOrPlural = StringUtils.optionalPlural(commsToNextMilestone, "commission", "commissions")
        val hotmXPGain = (commsToNextMilestone * perComm).roundToInt().addSeparators()
        if (commsToNextMilestone > 0 && (completed < required || config.allMilestones)) listBeingModified.add(Renderable.string(" §7- $colorCode$singularOrPlural §fleft to complete §6Milestone $milestone §f($colorCode+$hotmXPGain HOTM XP§f)"))
    }

    private fun drawDisplay(list: List<Renderable>) {
        display = list
        if (currentHOTMTier != 0) {
            display += fatDisclaimer
            if (InventoryUtils.openInventoryName() != "Heart of the Mountain") display += disclaimerOpenHOTM
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (currentHOTMTier.isMaxHOTMTier()) return
        if (InventoryUtils.openInventoryName().isNotValidChestName()) return
        config.position.renderRenderables(display, posLabel = "Commissions Calculator")
    }

    private fun isEnabled() = (DWARVEN.isInIsland() || IslandType.CRYSTAL_HOLLOWS.isInIsland()) && config.enabled
    private fun String.isNotValidChestName(): Boolean = this != "Commission Milestones" && this != "Commissions" && this != "Heart of the Mountain"
    private fun String.groupToInt(): Int = this.formatNumber().toInt()
    private fun Int.isMaxHOTMTier(): Boolean = this == HOTMTier.entries.last().tier + 1
}
