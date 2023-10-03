package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullOwner
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TestCopyBestiaryValues {

    class BestiarityObject {
        @Expose
        var name: String = ""

        @Expose
        var skullOwner: String = ""

        @Expose
        var texture: String = ""

        @Expose
        var cap: Int = 0

        @Expose
        var mobs: Array<String> = emptyArray()

        @Expose
        var bracket: Int = 0
    }

    val pattern = "\\[Lv(?<lvl>.*)] (?<text>.*)".toPattern()

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onLateInventoryOpen(event: InventoryUpdatedEvent) {
        if (!SkyHanniMod.feature.dev.debug.copyBestiaryData) return
        SkyHanniDebugsAndTests.displayLine = ""

        val backItem = event.inventoryItems[3 + 9 * 5 + 3]
        if (backItem == null) {
            return
        }
        if (backItem.getLore().none { it.contains("Bestiary Milestone") }) {
            return
        }

        val rankingItem = event.inventoryItems[3 + 9 * 5 + 2]
        if (rankingItem == null) {
            return
        }
        if (rankingItem.getLore().none { it.contains("Ranking") }) {
            return
        }

        val titleItem = event.inventoryItems[4] ?: return
        copy(titleItem, event.inventoryItems)
    }

    private fun copy(titleItem: ItemStack, inventoryItems: Map<Int, ItemStack>) {
        val name = titleItem.name ?: return
        val titleName = name.split(" ").dropLast(1).joinToString(" ")

        val obj: BestiarityObject = BestiarityObject()
        obj.name = titleName
        obj.texture = titleItem.getSkullTexture() ?: "no texture found"
        obj.skullOwner = titleItem.getSkullOwner() ?: "no skullOwner found"

        val lore = titleItem.getLore()
        val overallProgress = lore.find { it.contains("Overall Progress") }
        if (overallProgress == null) {
            println("overallProgress not found!")
            return
        }
        val capLine = lore.nextAfter(overallProgress) ?: return
        val rawCap = capLine.substringAfter("/").removeColor().formatNumber()
        obj.cap = rawCap.toInt()

        val mobs = mutableListOf<String>()
        for (i in 10..43) {
            val stack = inventoryItems[i] ?: continue
            val stackName = stack.name ?: continue
            pattern.matchMatcher(stackName.removeColor()) {
                val lvl = group("lvl").toInt()
                var text = group("text").lowercase().replace(" ", "_")

                val master = text.endsWith("(master)")
                val masterText = if (master) "master_" else ""
                if (master) {
                    text = text.split("_").dropLast(1).joinToString("_")
                }
                val result = "$masterText${text}_$lvl"
                mobs.add(result)
            }
        }
        obj.mobs = mobs.toTypedArray()

        val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
        val text = gson.toJson(obj)
        OSUtils.copyToClipboard(text)

        SkyHanniDebugsAndTests.displayLine = "Bestiary for $titleName"
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(4, "dev.copyBestiaryData", "dev.debug.copyBestiaryData")
    }
}