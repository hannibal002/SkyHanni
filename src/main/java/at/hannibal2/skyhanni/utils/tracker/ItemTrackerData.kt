package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

abstract class ItemTrackerData : TrackerData() {

    private val config get() = SkyHanniMod.feature.misc.tracker

    private var lastClickDelay = 0L

    companion object {
        val SKYBLOCK_COIN by lazy { "SKYBLOCK_COIN".asInternalName() }
    }

    abstract fun resetItems()

    abstract fun getDescription(timesGained: Long): List<String>

    abstract fun getCoinFormat(item: TrackedItem, numberColor: String): Pair<String, List<String>>

    open fun getCustomPricePer(internalName: NEUInternalName) = SkyHanniTracker.getPricePer(internalName)

    override fun reset() {
        items.clear()
        resetItems()
    }

    fun additem(internalName: NEUInternalName, stackSize: Int) {
        val item = items.getOrPut(internalName) { TrackedItem() }

        item.timesGained++
        item.totalAmount += stackSize
        item.lastTimeUpdated = SimpleTimeMark.now()
    }

    fun drawItem(
        tracker: SkyHanniItemTracker<out ItemTrackerData>,
        item: TrackedItem,
        internalName: NEUInternalName,
        map: MutableMap<Renderable, Long>
    ): Long {
        val amount = item.totalAmount

        val pricePer = if (internalName == SKYBLOCK_COIN) 1.0 else getCustomPricePer(internalName)

        val price = (pricePer * amount).toLong()
        val displayAmount = if (internalName == SKYBLOCK_COIN) item.timesGained else amount

        var name = if (internalName == SKYBLOCK_COIN) {
            "§6Coins"
        } else {
            internalName.getItemStack().nameWithEnchantment ?: error("no name for $internalName")
        }
        val priceFormat = NumberUtil.format(price)
        val hidden = item.hidden

        val newDrop = item.lastTimeUpdated.passedSince() < 10.seconds && config.showRecentDrops
        val numberColor = if (newDrop) "§a§l" else "§7"

        if (hidden) {
            name = "§8§m" + name.removeColor(keepFormatting = true).replace("§r", "")
        }

        val text = " $numberColor${displayAmount.addSeparators()}x $name§7: §6$priceFormat"
        val (displayName, lore) = if (internalName == SKYBLOCK_COIN) {
            getCoinFormat(item, numberColor)
        } else text to buildLore(item, hidden, newDrop)

        val renderable = if (tracker.isInventoryOpen()) Renderable.clickAndHover(displayName, lore) {
            if (System.currentTimeMillis() > lastClickDelay + 150) {
                if (KeyboardManager.isControlKeyDown()) {
                    items.remove(internalName)
                    LorenzUtils.chat("§e[SkyHanni] Removed ${if (internalName == SKYBLOCK_COIN) "§6Coins" else internalName.getItemStack().nameWithEnchantment} §efrom Fishing Frofit Tracker.")
                    lastClickDelay = System.currentTimeMillis() + 500
                } else {
                    item.hidden = !hidden
                    lastClickDelay = System.currentTimeMillis()
                }
                tracker.update()
            }
        } else Renderable.string(displayName)
        if (tracker.isInventoryOpen() || !hidden) {
            map[renderable] = price
        }
        return price
    }

    private fun buildLore(
        item: TrackedItem,
        hidden: Boolean,
        newDrop: Boolean
    ) = buildList {
        addAll(getDescription(item.timesGained))
        add("")
        if (newDrop) {
            add("§aYou caught this item recently.")
            add("")
        }
        add("§eClick to " + (if (hidden) "show" else "hide") + "!")
        add("§eControl + Click to remove this item!")
    }

    @Expose
    var items: MutableMap<NEUInternalName, TrackedItem> = HashMap()

    class TrackedItem {
        @Expose
        var internalName: NEUInternalName? = null

        @Expose
        var timesGained: Long = 0

        @Expose
        var totalAmount: Long = 0

        @Expose
        var hidden = false

        var lastTimeUpdated = SimpleTimeMark.farPast()
    }
}
