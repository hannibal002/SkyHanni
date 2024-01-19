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
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class CommissionsCalculator {
    private val config get() = SkyHanniMod.feature.mining.commissionsCalculator

    private val DWARVEN = IslandType.DWARVEN_MINES
    private val CRYSTAL = IslandType.CRYSTAL_HOLLOWS

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

    private val display: MutableList<Renderable> = mutableListOf<Renderable>()
    private val firstLine: String = "§lCommissions Calculator:"
    private val fatDisclaimer: List<Renderable> = listOf<Renderable>(
        Renderable.string("§c§lDisclaimer: §r§cThis calculator only accounts for"),
        Renderable.string("§cthe standard HOTM XP gain from claiming commissions"),
        Renderable.string("§cbefore any modifiers/boosts or other sources."),
        Renderable.clickAndHover("§eTo update these calculations, click here to open /hotm.",
            listOf("Click to run §e/hotm"), onClick = { LorenzUtils.sendCommandToServer("hotm") }
        ),
    )

    private var currentHOTMLevel: Int = 0
    private var currentHOTMXP: Int = 0

    private enum class HOTMProgression(
        val tier: Int,
        val toNextTier: Int,
        val perComm: Double
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
        if (!inMiningIsland()) return
        if (!isEnabled()) return
        val chestName = event.inventoryName
        if (chestName.isNotValidChestName()) return
        val colorCode = if (DWARVEN.isInIsland()) "§2" else "§5" //themed based on mining island
        if (currentHOTMLevel == 0) {
            listOf(
                Renderable.string("$colorCode$firstLine"),
                Renderable.clickAndHover("§cOpen the §e/hotm §ctree.",
                    listOf("Click to run §e/hotm"), onClick = { LorenzUtils.sendCommandToServer("hotm") }
                ),
            ).update()
            return
        }
        val hotmInfo = HOTMProgression.entries.find { it.tier == currentHOTMLevel } ?: return
        val perComm = hotmInfo.perComm
        val toNextTier = hotmInfo.toNextTier
        val items = event.inventoryItems
        val commsToNextTier = ceil(abs(toNextTier - currentHOTMXP) / perComm).roundToInt()
        val newList = mutableListOf<Renderable>(
            Renderable.string("$colorCode$firstLine"),
        )
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
            newList.addAll(
                listOf(
                    Renderable.string(" §e(Remember to scroll up the HOTM tree!)"),
                    Renderable.string(" §7- §fCurrent HOTM Level: $colorCode$currentHOTMLevel"),
                    Renderable.string(" §7- §fCurrent HOTM XP: $colorCode${currentHOTMXP.addSeparators()}"),
                )
            )
            newList.update()
            return
        }
        if (chestName == "Commissions") {
            var hotmXP: Long = 0
            loop@for ((_, item) in items) {
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
            newList.add(Renderable.string(" §7- §f(At least $colorCode${hotmXP.addSeparators()} HOTM XP §fto be gained from claiming completed commissions)"))
        }
        if (chestName == "Commission Milestones") {
            for ((_, item) in items) {
                milestoneRewardsItemPattern.matchMatcher(item.cleanName()) {
                    val milestone = group("milestone").romanToDecimalIfNecessary()
                    for (line in item.getLore()) {
                        mileProgressPattern.matchMatcher(line) {
                            val completed = group("completed").groupToInt()
                            val required = group("required").groupToInt()
                            val commsToNextMilestone = abs(required - completed)
                            if (completed < required) {
                                val remainingPlural = StringUtils.optionalPlural(commsToNextMilestone, "commission", "commissions")
                                val hotmXPGain = (commsToNextMilestone * perComm).roundToInt()
                                newList.add(
                                    Renderable.string(" §7- $colorCode$remainingPlural §fleft to complete §6Milestone $milestone §f($colorCode+${hotmXPGain.addSeparators()} HOTM XP§f)"),
                                )
                            }
                            if (!config.allMilestones) {
                                val lastElement = newList.takeLast(1).first()
                                newList.clear()
                                newList.addAll(
                                    listOf<Renderable>(
                                        Renderable.string("$colorCode$firstLine"),
                                        lastElement,
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        newList.addAll(
            listOf(
                Renderable.string(" §7- $colorCode${commsToNextTier.addSeparators()} §fmore commissions to ${colorCode}HOTM ${currentHOTMLevel + 1}"),
                Renderable.string(" §7- §f(to reach $colorCode${toNextTier.addSeparators()} HOTM XP §ffrom $colorCode${currentHOTMXP.addSeparators()} HOTM XP§f)"),
            )
        )
        newList.update()
    }
    
    private fun List<Renderable>.update() {
        display.clear()
        display.addAll(this)
        if (currentHOTMLevel != 0) display.addAll(fatDisclaimer)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!inMiningIsland()) return
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName().isNotValidChestName()) return
        config.position.renderRenderables(display, posLabel = "Commissions Calculator")
    }

    private fun isEnabled() = config.enabled
    private fun inMiningIsland(): Boolean = DWARVEN.isInIsland() || CRYSTAL.isInIsland()
    private fun String.isNotValidChestName(): Boolean = this != "Commission Milestones" && this != "Commissions" && this != "Heart of the Mountain"
    private fun String.groupToInt(): Int = this.formatNumber().toInt()
}
