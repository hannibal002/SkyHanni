package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay.Companion.getLatestTrueFarmingFortune
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getLatestBlocksPerSecond
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class JacobContestTimeNeeded {
    private val config get() = GardenAPI.config
    private var display = emptyList<List<Any>>()
    private var currentBracket = ContestBracket.GOLD

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onLateInventoryOpen(event: InventoryUpdatedEvent) {
        if (FarmingContestAPI.inInventory) {
            update()
        }
    }

    private fun update() {
        val sorted = mutableMapOf<CropType, Double>()
        val map = mutableMapOf<CropType, Renderable>()
        for (crop in CropType.entries) {
            testCrop(crop, sorted, map)
        }

        this.display = buildList {
            addAsSingletonList("§e§lTime Needed for ${currentBracket.displayName} §eMedal!")

            addSelector<ContestBracket>(
                "§7Bracket: ",
                getName = { type -> type.name.lowercase() },
                isCurrent = { it == currentBracket },
                onChange = {
                    currentBracket = it
                    update()
                }
            )
            addAsSingletonList("")
            for (crop in sorted.sorted().keys) {
                val text = map[crop]!!
                add(listOf(crop.icon, text))
            }
        }
    }

    private fun testCrop(
        crop: CropType,
        sorted: MutableMap<CropType, Double>,
        map: MutableMap<CropType, Renderable>
    ) {

        val bps = crop.getBps()
        if (bps == null) {
            sorted[crop] = Double.MAX_VALUE
            map[crop] = Renderable.hoverTips(
                "§9${crop.cropName} §cNo speed data!",
                listOf("§cFarm ${crop.cropName} to show data!")
            )
            return
        }
        val ff = crop.getLatestTrueFarmingFortune()
        if (ff == null) {
            sorted[crop] = Double.MAX_VALUE
            map[crop] = Renderable.hoverTips(
                "§9${crop.cropName} §cNo Farming Fortune data!",
                listOf("§cHold a ${crop.cropName} specific", "§cfarming tool in hand to show data!")
            )
            return
        }

        val averages = FarmingContestAPI.calculateAverages(crop).second
        if (averages.isEmpty()) {
            sorted[crop] = Double.MAX_VALUE - 2
            map[crop] = Renderable.hoverTips(
                "§9${crop.cropName} §cNo contest data!",
                listOf(
                    "§cOpen more pages or participate",
                    "§cin a ${crop.cropName} Contest to show data!"
                )
            )
            return
        }

        val speed = (ff * crop.baseDrops * bps / 100).round(1).toInt()

        renderCrop(speed, crop, averages, sorted, map)
    }

    private fun renderCrop(
        speed: Int,
        crop: CropType,
        averages: Map<ContestBracket, Int>,
        sorted: MutableMap<CropType, Double>,
        map: MutableMap<CropType, Renderable>
    ) {
        var lowBPSWarning = listOf<String>()
        val rawSpeed = speed.toDouble()
        val speedForFormular = crop.getBps()?.let {
            if (it < 15) {
                val v = rawSpeed / it
                (v * 19.9).toInt()
            } else speed
        } ?: speed
        var showLine = ""
        val brackets = mutableListOf<String>()
        for (bracket in ContestBracket.entries) {
            val amount = averages[bracket]
            if (amount == null) {
                brackets.add("${bracket.displayName} §cNo contest data found!")
                continue
            }
            val timeInMinutes = amount.toDouble() / speedForFormular / 60
            val formatDuration = TimeUtils.formatDuration((timeInMinutes * 60 * 1000).toLong())
            val color = if (timeInMinutes < 20) "§b" else "§c"
            var marking = ""
            var bracketText = "${bracket.displayName} $color$formatDuration"
            var blocksPerSecond = crop.getBps()
            if (blocksPerSecond == null) {
                marking += "§0§l !" //hoping this never shows
                blocksPerSecond = 19.9
                lowBPSWarning = listOf("§cYour Blocks/second is too low,", "§cshowing 19.9 Blocks/second instead!")
            } else {
                if (blocksPerSecond < 15.0) {
                    marking += "§4§l !"
                    blocksPerSecond = 19.9
                    lowBPSWarning =
                        listOf("§cYour Blocks/second is too low,", "§cshowing 19.9 Blocks/second instead!")
                } else {
                    marking += " "
                }
            }
            val line = if (timeInMinutes < 20) {
                "§9${crop.cropName} §7in §b$formatDuration" + marking
            } else {
                val cropFF = crop.getLatestTrueFarmingFortune() ?: 0.0
                val cropsPerSecond = amount.toDouble() / blocksPerSecond / 60
                val ffNeeded = cropsPerSecond * 100 / 20 / crop.baseDrops
                val missing = (ffNeeded - cropFF).toInt()
                bracketText += " §7(Need ${missing.addSeparators()} FF more)"
                "§9${crop.cropName} §cNo ${currentBracket.displayName} §cmedal possible!" + marking
            }
            brackets.add(bracketText)
            if (bracket == currentBracket) {
                sorted[crop] = timeInMinutes
                showLine = line
            }
        }
        map[crop] = Renderable.hoverTips(showLine, buildList {
            add("§7Time Needed for §9${crop.cropName} Medals§7:")
            addAll(brackets)
            add("")
            val latestFF = crop.getLatestTrueFarmingFortune() ?: 0.0
            add("§7Latest FF: §e${(latestFF).addSeparators()}")
            val bps = crop.getBps()?.round(1) ?: 0
            add("§7${addBpsTitle()}§e${bps.addSeparators()}")
            addAll(lowBPSWarning)
        })
    }

    private fun addBpsTitle() = if (config.jacobContestCustomBps) "Custom Blocks/Second: " else "Your Blocks/Second: "

    private fun CropType.getBps() = if (config.jacobContestCustomBps) {
        config.jacobContestCustomBpsValue
    } else getLatestBlocksPerSecond()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!FarmingContestAPI.inInventory) return
        config.jacobContextTimesPos.renderStringsAndItems(display, posLabel = "Jacob Contest Time Needed")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.jacobContextTimes
}
