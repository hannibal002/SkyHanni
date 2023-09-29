package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.SlayerChangeEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeWordsAtEnd
import io.github.moulberry.notenoughupdates.util.Constants
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

class SlayerRngMeterDisplay {
    private val config get() = SkyHanniMod.feature.slayer.rngMeterDisplay
    private var display = ""
    private val inventoryNamePattern = "(?<name>.*) RNG Meter".toPattern()
    private val updatePattern = "   §dRNG Meter §f- §d(?<exp>.*) Stored XP".toPattern()
    private val changedItemPattern = "§aYou set your §r.* RNG Meter §r§ato drop §r.*§a!".toPattern()
    private var lastItemDroppedTime = 0L

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (event.repeatSeconds(1) && lastItemDroppedTime != 0L && System.currentTimeMillis() > lastItemDroppedTime + 4_000) {
            lastItemDroppedTime = 0L
            update()
        }
    }

    @SubscribeEvent
    fun onSlayerChange(event: SlayerChangeEvent) {
        update()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {

        if (!isEnabled()) return

        if (config.hideChat && SlayerAPI.isInSlayerArea) {
            changedItemPattern.matchMatcher(event.message) {
                event.blockedReason = "slayer_rng_meter"
            }
        }

        val currentMeter = updatePattern.matchMatcher(event.message) {
            group("exp").formatNumber()
        } ?: return

        val storage = getStorage() ?: return
        val old = storage.currentMeter
        storage.currentMeter = currentMeter

        if (old != -1L) {
            val item = storage.itemGoal
            val hasItemSelected = item != "" && item != "?"
            if (!hasItemSelected && config.warnEmpty) {
                LorenzUtils.warning("§c[Skyhanni] No Slayer RNG Meter Item selected!")
                TitleUtils.sendTitle("§cNo RNG Meter Item!", 3.seconds)
            }
            var blockChat = config.hideChat && hasItemSelected
            val diff = currentMeter - old
            if (diff > 0) {
                storage.gainPerBoss = diff
            } else {
                storage.itemGoal = ""
                blockChat = false
                val from = old.addSeparators()
                val to = storage.goalNeeded.addSeparators()

                var rawPercentage = old.toDouble() / storage.goalNeeded
                if (rawPercentage > 1) rawPercentage = 1.0
                val percentage = LorenzUtils.formatPercentage(rawPercentage)
                LorenzUtils.chat("§e[SkyHanni] §dRNG Meter §7dropped at §e$percentage §7XP ($from/${to}§7)")
                lastItemDroppedTime = System.currentTimeMillis()
            }
            if (blockChat) {
                event.blockedReason = "slayer_rng_meter"
            }
        }
        update()
    }

    private fun getStorage(): Storage.ProfileSpecific.SlayerRngMeterStorage? {
        return ProfileStorageData.profileSpecific?.slayerRngMeter?.getOrPut(getCurrentSlayer()) {
            Storage.ProfileSpecific.SlayerRngMeterStorage()
        }
    }

    private fun getCurrentSlayer() = SlayerAPI.latestSlayerCategory.removeWordsAtEnd(1).removeColor()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        val name = inventoryNamePattern.matchMatcher(event.inventoryName) {
            group("name")
        } ?: return

        if (name != getCurrentSlayer()) return

        val storage = getStorage() ?: return

        val selectedItem = event.inventoryItems.values.find { item -> item.getLore().any { it.contains("§aSELECTED") } }
        if (selectedItem == null) {
            storage.itemGoal = ""
            storage.goalNeeded = -1
        } else {
            storage.itemGoal = selectedItem.nameWithEnchantment
            val jsonObject = Constants.RNGSCORE["slayer"].asJsonObject.get(getCurrentSlayer()).asJsonObject
            storage.goalNeeded = jsonObject.get(selectedItem.getInternalName_old()).asLong
        }
        update()
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): String {
        val storage = getStorage() ?: return ""

        if (SlayerAPI.latestSlayerCategory.let {
                it.endsWith(" I") || it.endsWith(" II")
            }) {
            return ""
        }
        val latestSlayerCategory = SlayerAPI.latestSlayerCategory
        latestSlayerCategory.endsWith(" I")

        with(storage) {
            if (itemGoal == "?") return "§cOpen RNG Meter Inventory!"
            if (itemGoal == "") {
                return if (lastItemDroppedTime != 0L) {
                    "§a§lRNG Item dropped!"
                } else {
                    "§eNo RNG Item selected!"
                }
            }
            if (currentMeter == -1L || gainPerBoss == -1L) return "§cKill the slayer boss 2 times!"

            val missing = goalNeeded - currentMeter + gainPerBoss
            var timesMissing = missing.toDouble() / gainPerBoss
            if (timesMissing < 1) timesMissing = 1.0
            timesMissing = ceil(timesMissing)

            return "$itemGoal §7in §e${timesMissing.toInt().addSeparators()} §7bosses!"
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInSlayerArea) return
        if (!SlayerAPI.hasActiveSlayerQuest()) return

        config.pos.renderString(display, posLabel = "Rng Meter Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
