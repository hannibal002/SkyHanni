package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class GardenLevelDisplay {
    private val config get() = GardenAPI.config.gardenLevels
    private val expToNextLevelPattern by RepoPattern.pattern("garden.level.inventory.nextxp", ".* §e(?<nextLevelExp>.*)§6/.*")
    private val overflowPattern by RepoPattern.pattern("garden.level.inventory.overflow", ".*§r §6(?<overflow>.*)")
    private val currentLevelPattern by RepoPattern.pattern("garden.level.inventory.currentlevel", "Garden Level (?<currentLevel>.*)")
    private var display = ""
    private val visitorRewardPattern by RepoPattern.pattern("garden.level.chat.increase", " {4}§r§8\\+§r§2(?<exp>.*) §r§7Garden Experience")

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        update()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatMessage(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return

        visitorRewardPattern.matchMatcher(event.message) {
            addExp(group("exp").toInt())
        }
    }

    private fun addExp(moreExp: Int) {
        val gardenExp = GardenAPI.gardenExp ?: return
        val oldLevel = GardenAPI.getGardenLevel()
        GardenAPI.gardenExp = gardenExp + moreExp
        val newLevel = GardenAPI.getGardenLevel()
        if (newLevel == oldLevel + 1 && newLevel > 15) {
            LorenzUtils.runDelayed(50.milliseconds) {
                LorenzUtils.clickableChat(
                    " \n§b§lGARDEN LEVEL UP §8$oldLevel ➜ §b$newLevel\n" +
                        " §8+§aRespect from Elite Farmers and SkyHanni members :)\n ",
                    "/gardenlevels",
                    false
                )
            }
        }
        update()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!GardenAPI.inGarden()) return
        if (event.inventoryName != "Desk") return
        val item = event.inventoryItems[4]!!

        val currentLevel = currentLevelPattern.matchMatcher(item.name!!.removeColor()) {
            group("currentLevel").romanToDecimalIfNecessary()
        } ?: return
        var nextLevelExp = 0L
        for (line in item.getLore()) {
            expToNextLevelPattern.matchMatcher(line) {
                nextLevelExp = group("nextLevelExp").formatNumber()
            }
            overflowPattern.matchMatcher(line) {
                val overflow = group("overflow").formatNumber()
                GardenAPI.gardenExp = overflow
                update()
                return
            }
        }
        val expForLevel = GardenAPI.getExpForLevel(currentLevel).toInt()
        GardenAPI.gardenExp = expForLevel + nextLevelExp
        update()
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): String {
        val gardenExp = GardenAPI.gardenExp ?: return "§aGarden Level ? §cOpen the desk!"
        val currentLevel = GardenAPI.getGardenLevel()
        val needForLevel = GardenAPI.getExpForLevel(currentLevel).toInt()
        val nextLevel = currentLevel + 1
        val needForNextLevel = GardenAPI.getExpForLevel(nextLevel).toInt()

        return "§aGarden Level $currentLevel" + if (needForNextLevel != 0) {
            val overflow = gardenExp - needForLevel
            val needForOnlyNextLvl = needForNextLevel - needForLevel

            val need = LorenzUtils.formatInteger(overflow)
            val have = LorenzUtils.formatInteger(needForOnlyNextLvl)
            " §7(§e$need§7/§e$have§7)"
        } else ""
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (GardenAPI.hideExtraGuis()) return

        config.pos.renderString(display, posLabel = "Garden Level")
    }

    private fun isEnabled() = GardenAPI.inGarden() && config.display

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.gardenLevelDisplay", "garden.gardenLevels.display")
        event.move(3, "garden.gardenLevelPos", "garden.gardenLevels.pos")
    }
}
