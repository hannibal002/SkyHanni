package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.FarmingContestEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestPhase
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes

class AnitaPersonalBest {

    private val config get() = GardenAPI.config.anitaPersonalBestConfig

    private val patternGroup = RepoPattern.group("garden.inventory.anita.personalbests")

    private val personalBestValue by patternGroup.pattern(
        "value",
        "§7Personal Best: §6(?<personalBest>[0-9,.]+)"
    )

    private val personalBestBonus by patternGroup.pattern(
        "bonus",
        "§7Bonus: §6\\+(?<bonus>[0-9,.]+)☘ .*"
    )

    private val newPersonalBestPattern by patternGroup.pattern(
        "newvalue",
        "§e\\[NPC] Jacob§f: §rYou collected §e(?<amount>[0-9,.]+) §fitems! §d§lPERSONAL BEST§f!.*"
    )

    private val newBonusPattern by patternGroup.pattern(
        "newbonus",
        "§e\\[NPC] Jacob§f: §rYour §6Personal Bests §fperk is now granting you §6\\+(?<amount>[0-9,.]+)☘ (?<crop>.*) Fortune.*",
    )

    private val personalBestPassed by patternGroup.pattern(
        "passed",
        "§e\\[NPC] Jacob§f: §r§d§lPERSONAL BEST§f! You've surpassed your previous record of §e(?<amount>[0-9,.]+) §fitems collected in the §a(?<crop>.*) Contest.*"
    )

    private val contestAmount by patternGroup.pattern(
        "amountcollected",
        " ((?:COLLECTED)?|(?:BRONZE|SILVER|GOLD|PLATINIUM|DIAMOND)? with) (?<amount>[\\d,]+)"
    )

    private var display = listOf<Renderable>()
    private var lastCrop: CropType? = null
    private var lastContestTime = SimpleTimeMark.farPast()
    private var lastContestType: CropType? = null
    private var newPersonalBest = 0L
    private var newBonus = 0.0
    private var newBestSidebar = 0L
    private var update = false

    private val calc = mapOf(
        CropType.WHEAT to 1000,
        CropType.CARROT to 3000,
        CropType.POTATO to 3000,
        CropType.PUMPKIN to 1000,
        CropType.MUSHROOM to 1000,
        CropType.CACTUS to 2000,
        CropType.SUGAR_CANE to 2000,
        CropType.NETHER_WART to 3000,
        CropType.COCOA_BEANS to 3000,
        CropType.MELON to 5000
    )

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (config.onlyInContest && !FarmingContestAPI.inContest) return
        config.position.renderRenderables(display, posLabel = "Anita Personal Best")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(1)) return
        display = drawDisplay()

        for (line in ScoreboardData.sidebarLines) {
            contestAmount.matchMatcher(line.removeColor()) {
                newBestSidebar = group("amount").formatLong()
            }
        }

        val storage = GardenAPI.storage ?: return
        val currentCrop = lastCrop ?: return
        val cropPersonalBest = storage.cropPersonalBest[currentCrop] ?: 0L

        update = (newBestSidebar > cropPersonalBest) && FarmingContestAPI.inContest && FarmingContestAPI.contestCrop == lastCrop
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        lastCrop = event.crop
    }

    private fun drawDisplay(): List<Renderable> = buildList {
        val storage = GardenAPI.storage ?: return@buildList
        val currentCrop = lastCrop ?: return@buildList
        val cropPersonalBest = storage.cropPersonalBest[currentCrop]
        val cropBonus = storage.cropBonus[currentCrop]
        if (cropPersonalBest == null || cropBonus == null) {
            add(Renderable.string("§cNo saved personal best for ${currentCrop.cropName}!"))
            return@buildList
        }
        if (!update) {
            add(Renderable.string("§6${currentCrop.cropName} Personal Best: §b${cropPersonalBest.addSeparators()}"))
        } else {
            val oldBest = "§8§m${cropPersonalBest.addSeparators()}"
            add(Renderable.string("§6${currentCrop.cropName} Personal Best: $oldBest §6${newBestSidebar.addSeparators()}"))
        }

        if (config.showBonus) {
            if (!update) {
                add(Renderable.string("§6+$cropBonus☘ ${currentCrop.cropName} Fortune"))
            } else {
                val old = "§8§m+$cropBonus☘ "
                var newBest = 0L
                for (line in ScoreboardData.sidebarLines) {
                    contestAmount.matchMatcher(line.removeColor()) {
                        newBest = group("amount").formatLong()
                    }
                }
                val newnew = ((newBest.toDouble() / calc.getOrDefault(currentCrop, 1)) * 0.1).roundToPrecision(2)
                val new = "§6+$newnew☘"
                add(Renderable.string("$old$new ${currentCrop.cropName} Fortune"))
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        if (inventoryName != "Personal Bests") return
        val storage = GardenAPI.storage ?: return
        for (item in event.inventoryItems.values) {
            val name = item.name.split(" ").dropLast(2).joinToString(" ").removeColor()
            val crop = CropType.getByNameOrNull(name) ?: continue
            var pb = 0L
            var bonus = 0.0
            for (line in item.getLore()) {
                personalBestValue.matchMatcher(line) {
                    pb = group("personalBest").formatLong()
                }
                personalBestBonus.matchMatcher(line) {
                    bonus = group("bonus").formatDouble()
                }
            }
            storage.cropPersonalBest[crop] = pb
            storage.cropBonus[crop] = bonus
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        personalBestPassed.matchMatcher(event.message) {
            update = true
        }

        if (lastContestTime.passedSince() > 2.minutes) return

        newPersonalBestPattern.matchMatcher(event.message) {
            newPersonalBest = group("amount").formatLong()
        }
        newBonusPattern.matchMatcher(event.message) {
            val bonus = group("amount").formatDouble()
            newBonus = bonus
            val cropName = group("crop")
            val crop = CropType.getByNameOrNull(cropName) ?: return
            if (crop != lastContestType) return
            val storage = GardenAPI.storage ?: return
            storage.cropPersonalBest[crop] = newPersonalBest
            storage.cropBonus[crop] = newBonus
            update = false
            ChatUtils.chat("Updated your personal best for ${crop.cropName}!")
        }
    }

    @SubscribeEvent
    fun onContestEnded(event: FarmingContestEvent) {
        if (event.phase != FarmingContestPhase.STOP) return
        lastContestTime = SimpleTimeMark.now()
        lastContestType = event.crop
        update = false
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.enabled
}
